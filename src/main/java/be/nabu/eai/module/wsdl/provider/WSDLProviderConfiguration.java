/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.wsdl.provider;

import java.nio.charset.Charset;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.services.api.DefinedService;

@XmlRootElement(name = "wsdlProvider")
public class WSDLProviderConfiguration {
	
	private SOAPVersion soapVersion;
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

	public SOAPVersion getSoapVersion() {
		return soapVersion;
	}
	public void setSoapVersion(SOAPVersion soapVersion) {
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
