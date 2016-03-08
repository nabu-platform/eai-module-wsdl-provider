package be.nabu.eai.module.wsdl.provider;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.NamespaceProperty;
import be.nabu.libs.wsdl.api.Style;
import be.nabu.libs.wsdl.api.Transport;
import be.nabu.libs.wsdl.api.Use;
import be.nabu.libs.wsdl.formatter.WSDLFormatter;
import be.nabu.libs.wsdl.parser.impl.BindingImpl;
import be.nabu.libs.wsdl.parser.impl.BindingOperationImpl;
import be.nabu.libs.wsdl.parser.impl.MessageImpl;
import be.nabu.libs.wsdl.parser.impl.MessagePartImpl;
import be.nabu.libs.wsdl.parser.impl.OperationImpl;
import be.nabu.libs.wsdl.parser.impl.PortTypeImpl;
import be.nabu.libs.wsdl.parser.impl.ServiceImpl;
import be.nabu.libs.wsdl.parser.impl.ServicePortImpl;
import be.nabu.libs.wsdl.parser.impl.WSDLDefinitionImpl;
import be.nabu.utils.xml.XMLUtils;

public class WSDLProvider extends JAXBArtifact<WSDLProviderConfiguration> implements WebFragment {

	private Map<String, EventSubscription<?, ?>> subscriptions = new HashMap<String, EventSubscription<?, ?>>();
	private Map<String, String> wsdls = new HashMap<String, String>();
	
	public WSDLProvider(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "wsdl-provider.xml", WSDLProviderConfiguration.class);
	}

	@Override
	public void start(WebApplication artifact, String path) throws IOException {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			stop(artifact, path);
		}
		String wsdlPath = getFullPath(artifact, path);
		// TODO
		synchronized(subscriptions) {
			WSDLFragmentListener listener = new WSDLFragmentListener(this, artifact, path);
			EventSubscription<HTTPRequest, HTTPResponse> subscription = artifact.getDispatcher().subscribe(HTTPRequest.class, listener);
			subscription.filter(HTTPServerUtils.limitToPath(wsdlPath));
			subscriptions.put(key, subscription);
		}
	}

	private String getFullPath(WebApplication artifact, String path) throws IOException {
		String wsdlPath = artifact.getServerPath();
		if (path != null && !path.isEmpty() && !path.equals("/")) {
			if (!wsdlPath.endsWith("/")) {
				wsdlPath += "/";
			}
			wsdlPath += path.replaceFirst("^[/]+", "");
		}
		if (getConfiguration().getPath() != null) {
			wsdlPath += "/" + getConfiguration().getPath().replaceFirst("^[/]+", "");
		}
		return wsdlPath;
	}
	
	public Charset getCharset() {
		try {
			return getConfiguration().getCharset() == null ? Charset.forName("UTF-8") : getConfiguration().getCharset();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public double getSoapVersion() {
		try {
			return getConfiguration().getSoapVersion() == null ? 1.1 : getConfiguration().getSoapVersion();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getWSDL(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (!wsdls.containsKey(key)) {
			synchronized(this) {
				if (!wsdls.containsKey(key)) {
					try {
						WSDLDefinitionImpl definition = new WSDLDefinitionImpl();
						definition.setTargetNamespace(getNamespace());
						TypeRegistryImpl registry = new TypeRegistryImpl();
						PortTypeImpl portType = new PortTypeImpl();
						String wsdlName = getId().replaceAll("^.*\\.", "");
						portType.setName(wsdlName + "Port");
						portType.setDefinition(definition);
						
						BindingImpl binding = new BindingImpl();
						binding.setDefinition(definition);
						binding.setName(wsdlName + "Binding");
						binding.setPortType(portType);
						binding.setTransport(Transport.HTTP);
						for (DefinedService service : getConfiguration().getServices()) {
							String name = service.getId().replaceAll("^.*\\.", "");
							OperationImpl operation = new OperationImpl();
							operation.setName(name);
							operation.setDefinition(definition);
							
							MessagePartImpl inputPart = new MessagePartImpl();
							inputPart.setDefinition(definition);
							inputPart.setName(name);
							ComplexElementImpl inputElement = new ComplexElementImpl(name, service.getServiceInterface().getInputDefinition(), null, new ValueImpl<String>(NamespaceProperty.getInstance(), getNamespace()));
							inputPart.setElement(inputElement);
							MessageImpl inputMessage = new MessageImpl();
							inputMessage.setDefinition(definition);
							inputMessage.setName(name);
							inputMessage.getParts().add(inputPart);
							operation.setInput(inputMessage);
							definition.getMessages().add(inputMessage);
							
							MessagePartImpl outputPart = new MessagePartImpl();
							outputPart.setDefinition(definition);
							outputPart.setName(name + "Response");
							ComplexElementImpl outputElement = new ComplexElementImpl(name + "Response", service.getServiceInterface().getOutputDefinition(), null, new ValueImpl<String>(NamespaceProperty.getInstance(), getNamespace()));
							outputPart.setElement(outputElement);
							MessageImpl outputMessage = new MessageImpl();
							outputMessage.setDefinition(definition);
							outputMessage.setName(name + "Response");
							outputMessage.getParts().add(outputPart);
							operation.setOutput(outputMessage);
							definition.getMessages().add(outputMessage);
							// TODO: populate registry
							
							BindingOperationImpl bindingOperation = new BindingOperationImpl();
							bindingOperation.setDefinition(definition);
							bindingOperation.setStyle(Style.DOCUMENT);
							bindingOperation.setUse(Use.LITERAL);
							bindingOperation.setOperation(operation);
							if (getSoapVersion() == 1.1) {
								bindingOperation.setSoapAction(service.getId());
							}
							binding.getOperations().add(bindingOperation);
							registry.register(inputElement);
							registry.register(outputElement);
							
							portType.getOperations().add(operation);
						}
						definition.getPortTypes().add(portType);
						definition.setRegistry(registry);
						definition.getBindings().add(binding);
						
						ServiceImpl service = new ServiceImpl();
						service.setDefinition(definition);
						service.setName(wsdlName + "Service");
						ServicePortImpl servicePort = new ServicePortImpl();
						servicePort.setBinding(binding);
						servicePort.setDefinition(definition);
						servicePort.setName(wsdlName + "Port");
						VirtualHostArtifact virtualHost = artifact.getConfiguration().getVirtualHost();
						boolean secure = virtualHost.getConfiguration().getServer().getConfiguration().getKeystore() != null;
						Integer port = virtualHost.getConfiguration().getServer().getConfiguration().getPort();
						String endpoint = secure ? "https://" : "http://";
						String host = virtualHost.getConfiguration().getHost();
						if (host == null) {
							host = InetAddress.getLocalHost().getHostName();
						}
						endpoint += host;
						if (port != null) {
							endpoint += ":" + port;
						}
						endpoint += getFullPath(artifact, path);
						servicePort.setEndpoint(endpoint);
						service.getPorts().add(servicePort);
						definition.getServices().add(service);
						
						WSDLFormatter formatter = new WSDLFormatter();
						Document format = formatter.format(definition);
						this.wsdls.put(key, XMLUtils.toString(format, true, true));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return wsdls.get(key);
	}

	public String getNamespace() {
		try {
			String namespace = getConfiguration().getNamespace();
			if (namespace == null || namespace.trim().isEmpty()) {
				namespace = getId();
			}
			return namespace;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void stop(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					subscriptions.get(key).unsubscribe();
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}

	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return subscriptions.containsKey(getKey(artifact, path));
	}

	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
}