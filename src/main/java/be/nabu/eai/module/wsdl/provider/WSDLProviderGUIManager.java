package be.nabu.eai.module.wsdl.provider;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class WSDLProviderGUIManager extends BaseJAXBGUIManager<WSDLProviderConfiguration, WSDLProvider> {

	public WSDLProviderGUIManager() {
		super("WSDL Provider", WSDLProvider.class, new WSDLProviderManager(), WSDLProviderConfiguration.class);
	}

	public String getCategory() {
		return "Web";
	}
	
	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected WSDLProvider newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new WSDLProvider(entry.getId(), entry.getContainer(), entry.getRepository());
	}

}
