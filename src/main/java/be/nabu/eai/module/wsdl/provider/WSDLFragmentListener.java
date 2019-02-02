package be.nabu.eai.module.wsdl.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.wsdl.client.WSDLInterface;
import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.authentication.api.Device;
import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.ServiceRuntime;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.BaseTypeInstance;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.binding.xml.XMLMarshaller;
import be.nabu.libs.types.properties.AttributeQualifiedDefaultProperty;
import be.nabu.libs.types.properties.ElementQualifiedDefaultProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.NamespaceProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class WSDLFragmentListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private WebApplication application;
	private WSDLProvider provider;
	private String path;

	public WSDLFragmentListener(WSDLProvider provider, WebApplication application, String path) {
		this.provider = provider;
		this.application = application;
		this.path = path;
	}
	
	@Override
	public HTTPResponse handle(HTTPRequest request) {
		try {
			URI uri = HTTPUtils.getURI(request, false);
			String path = URIUtils.normalize(uri.getPath());
			if (path.equals(provider.getFullPath(application, this.path))) {
				if (request.getMethod().equalsIgnoreCase("POST")) {
					Token token = WebApplicationUtils.getToken(application, request);
					Device device = WebApplicationUtils.getDevice(application, request, token);
					WebApplicationUtils.checkRole(application, token, provider.getConfig().getRoles());

					// validate content type
					String contentType = MimeUtils.getContentType(request.getContent().getHeaders());
					if (!"application/soap+xml".equals(contentType) && !"text/xml".equals(contentType) && !"application/xml".equals(contentType)) {
						throw new HTTPException(415, "Invalid content type: " + contentType, token);
					}
					String encoding = MimeUtils.getCharset(request.getContent().getHeaders());
					Charset charset = encoding == null ? provider.getCharset() : Charset.forName(encoding);
					
					DefinedService service = null;
					ComplexContent soapEnvelope = null;
					Header header = MimeUtils.getHeader("SOAPAction", request.getContent().getHeaders());
					String soapAction = header != null && header.getValue() != null ? header.getValue().replaceAll("\"", "").trim() : null;
					if (soapAction != null && !soapAction.isEmpty()) {
						for (DefinedService potential : provider.getConfiguration().getServices()) {
							WSDLInterface iface = null;
							if (potential.getServiceInterface().getParent() instanceof WSDLInterface) {
								iface = (WSDLInterface) potential.getServiceInterface().getParent();
							}
							if ((iface != null && soapAction.equals(iface.getOperation().getSoapAction())) || potential.getId().equals(soapAction)) {
								service = potential;
								break;
							}
						}
						if (service == null) {
							throw new HTTPException(400, "Invalid soap action: " + header.getValue(), token);
						}
						// immediately parse the input using the given service
						ComplexType requestType = buildRequestEnvelope(service, provider.getSoapVersion(), true);
						XMLBinding binding = new XMLBinding(requestType, charset);
						ReadableContainer<ByteBuffer> readable = ((ContentPart) request.getContent()).getReadable();
						try {
							soapEnvelope = binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]);
						}
						finally {
							readable.close();
						}
					}
					// we need to guess the service based on the input
					else {
						for (DefinedService potential : provider.getConfiguration().getServices()) {
							ComplexType requestType = buildRequestEnvelope(potential, provider.getSoapVersion(), true);
							try {
								XMLBinding binding = new XMLBinding(requestType, charset);
								ReadableContainer<ByteBuffer> readable = ((ContentPart) request.getContent()).getReadable();
								try {
									soapEnvelope = binding.unmarshal(IOUtils.toInputStream(readable), new Window[0]);
									service = potential;
									break;
								}
								finally {
									readable.close();
								}
							}
							catch (Exception e) {
								// try next
							}
						}
						if (service == null) {
							throw new HTTPException(400, "Invalid input does not match any service", token);
						}
					}
					
					// check permissions
					if (application.getPermissionHandler() != null) {
						if (!application.getPermissionHandler().hasPermission(token, provider.getId(), service.getId())) {
							throw new HTTPException(token == null ? 401 : 403, "User '" + (token == null ? Authenticator.ANONYMOUS : token.getName()) + "' does not have permission to '" + request.getMethod().toLowerCase() + "' on '" + path + "' for wsdl endpoint: " + provider.getId(), token);
						}
					}
	
					HTTPResponse checkRateLimits = WebApplicationUtils.checkRateLimits(application, token, device, service.getId(), null, request);
					if (checkRateLimits != null) {
						return checkRateLimits;
					}
					
					ServiceRuntime runtime = new ServiceRuntime(service, provider.getRepository().newExecutionContext(token));
					runtime.getContext().put("session", WebApplicationUtils.getSession(application, request));
					runtime.getContext().put("device", device);
					
					ComplexContent output = runtime.run((ComplexContent) soapEnvelope.get("Body/" + WSDLProvider.getInputName(service)));
					ComplexType responseEnvelope = buildRequestEnvelope(service, provider.getSoapVersion(), false);
					ComplexContent newInstance = responseEnvelope.newInstance();
					newInstance.set("Body/" + WSDLProvider.getOutputName(service), output);
					XMLMarshaller marshaller = new XMLMarshaller(new BaseTypeInstance(responseEnvelope));
					// set the qualified-ness
					marshaller.setElementQualified(true);
					marshaller.setAttributeQualified(false);
					marshaller.setAllowQualifiedOverride(true);
					marshaller.setPrefix(responseEnvelope.getNamespace(), "soap");
					// the default namespace gets in the way of the elements that are not qualified
					marshaller.setAllowDefaultNamespace(false);
	//				XMLBinding binding = new XMLBinding(responseEnvelope, charset);
					ByteArrayOutputStream result = new ByteArrayOutputStream();
					marshaller.marshal(result, charset, newInstance);
	//				binding.marshal(result, newInstance);
					byte[] byteArray = result.toByteArray();
					List<Header> headers = new ArrayList<Header>();
					headers.add(new MimeHeader("Content-Length", "" + byteArray.length));
					headers.add(new MimeHeader("Content-Type", contentType + "; charset=" + charset.name()));
					PlainMimeContentPart part = new PlainMimeContentPart(null,
						IOUtils.wrap(byteArray, true),
						headers.toArray(new Header[headers.size()])
					);
					return new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), part);
				}
				else if (request.getMethod().equalsIgnoreCase("GET")) {
					Map<String, List<String>> queryProperties = URIUtils.getQueryProperties(uri);
					// we are requesting the WSDL
					if (queryProperties.containsKey("wsdl") || queryProperties.containsKey("WSDL")) {
						byte[] bytes = provider.getWSDL(application, this.path).getBytes(provider.getCharset());
						PlainMimeContentPart part = new PlainMimeContentPart(null,
							IOUtils.wrap(bytes, true),
							new MimeHeader("Content-Length", "" + bytes.length),
							new MimeHeader("Content-Type", "text/xml")	// application/wsdl+xml
						);
						return new DefaultHTTPResponse(request, 200, HTTPCodes.getMessage(200), part);
					}
				}
			}
		}
		catch (HTTPException e) {
			throw e;
		}
		catch (Exception e) {
			throw new HTTPException(500, e);
		}
		return null;
	}
	
	private ComplexType buildRequestEnvelope(DefinedService service, double soapVersion, boolean isInput) {
		Structure envelope = new Structure();
		envelope.setName("Envelope");
		envelope.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), true));
		envelope.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), true));
		envelope.setProperty(new ValueImpl<Boolean>(new AttributeQualifiedDefaultProperty(), false));
		envelope.setProperty(new ValueImpl<Boolean>(new ElementQualifiedDefaultProperty(), true));
		if (soapVersion == 1.2) {
			envelope.setNamespace("http://www.w3.org/2003/05/soap-envelope");
		}
		// default is 1.1
		else {
			envelope.setNamespace("http://schemas.xmlsoap.org/soap/envelope/");
		}
		Structure header = new Structure();
		header.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), true));
		header.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), true));
		header.setName("Header");
		envelope.add(new ComplexElementImpl(header, envelope, new ValueImpl<Integer>(new MinOccursProperty(), 0)));
		
		Structure body = new Structure();
		body.setName("Body");
		body.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), true));
		body.setProperty(new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), true));
		body.setNamespace(envelope.getNamespace());
		try {
			if (isInput) {
				ComplexElementImpl element = new ComplexElementImpl(WSDLProvider.getInputName(service), service.getServiceInterface().getInputDefinition(), body, new ValueImpl<String>(NamespaceProperty.getInstance(), provider.getNamespace()));
				element.setMaintainDefaultValues(true);
				element.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), provider.getConfiguration().getAttributeQualified() != null && provider.getConfiguration().getAttributeQualified()),
					new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), provider.getConfiguration().getElementQualified() != null && provider.getConfiguration().getElementQualified()));
				body.add(element);
			}
			else {
				ComplexElementImpl element = new ComplexElementImpl(WSDLProvider.getOutputName(service), service.getServiceInterface().getOutputDefinition(), body, new ValueImpl<String>(NamespaceProperty.getInstance(), provider.getNamespace()));
				element.setMaintainDefaultValues(true);
				element.setProperty(new ValueImpl<Boolean>(AttributeQualifiedDefaultProperty.getInstance(), provider.getConfiguration().getAttributeQualified() != null && provider.getConfiguration().getAttributeQualified()),
					new ValueImpl<Boolean>(ElementQualifiedDefaultProperty.getInstance(), provider.getConfiguration().getElementQualified() != null && provider.getConfiguration().getElementQualified()));
				body.add(element);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		envelope.add(new ComplexElementImpl(body, envelope));
		return envelope;
	}
}
