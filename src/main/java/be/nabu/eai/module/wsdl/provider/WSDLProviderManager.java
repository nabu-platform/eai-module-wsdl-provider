package be.nabu.eai.module.wsdl.provider;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class WSDLProviderManager extends JAXBArtifactManager<WSDLProviderConfiguration, WSDLProvider> {

	public WSDLProviderManager() {
		super(WSDLProvider.class);
	}

	@Override
	protected WSDLProvider newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new WSDLProvider(id, container, repository);
	}

}
