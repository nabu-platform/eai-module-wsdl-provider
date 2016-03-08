package be.nabu.eai.module.wsdl.provider;

import java.nio.charset.Charset;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "wsdlProvider")
public class WSDLProviderConfiguration {
	
	private Double soapVersion;
	private List<DefinedService> services;
	private String path;
	private List<String> roles;
	private Charset charset;
	private String namespace;
	private Boolean importTypes;
	private Boolean elementQualified, attributeQualified;

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedService> getServices() {
		return services;
	}
	public void setServices(List<DefinedService> services) {
		this.services = services;
	}

	public Double getSoapVersion() {
		return soapVersion;
	}
	public void setSoapVersion(Double soapVersion) {
		this.soapVersion = soapVersion;
	}
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public Boolean getImportTypes() {
		return importTypes;
	}
	public void setImportTypes(Boolean importTypes) {
		this.importTypes = importTypes;
	}
	public Boolean getElementQualified() {
		return elementQualified;
	}
	public void setElementQualified(Boolean elementQualified) {
		this.elementQualified = elementQualified;
	}
	public Boolean getAttributeQualified() {
		return attributeQualified;
	}
	public void setAttributeQualified(Boolean attributeQualified) {
		this.attributeQualified = attributeQualified;
	}

}
