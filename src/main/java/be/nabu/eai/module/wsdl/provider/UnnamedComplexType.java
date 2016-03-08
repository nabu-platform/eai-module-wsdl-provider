package be.nabu.eai.module.wsdl.provider;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Group;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validator;

public class UnnamedComplexType implements ComplexType {

	private ComplexType originalType;

	public UnnamedComplexType(ComplexType originalType) {
		this.originalType = originalType;
	}
	
	@Override
	public String getName(Value<?>... values) {
		return null;
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return originalType.getNamespace(values);
	}

	@Override
	public boolean isList(Value<?>... properties) {
		return originalType.isList(properties);
	}

	@Override
	public Validator<?> createValidator(Value<?>... properties) {
		return originalType.createValidator(properties);
	}

	@Override
	public Validator<Collection<?>> createCollectionValidator(Value<?>... properties) {
		return originalType.createCollectionValidator(properties);
	}

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>... properties) {
		return originalType.getSupportedProperties(properties);
	}

	@Override
	public Type getSuperType() {
		return originalType.getSuperType();
	}

	@Override
	public Value<?>[] getProperties() {
		return originalType.getProperties();
	}

	@Override
	public Iterator<Element<?>> iterator() {
		return originalType.iterator();
	}

	@Override
	public Element<?> get(String path) {
		return originalType.get(path);
	}

	@Override
	public ComplexContent newInstance() {
		return originalType.newInstance();
	}

	@Override
	public Boolean isAttributeQualified(Value<?>... values) {
		return originalType.isAttributeQualified(values);
	}

	@Override
	public Boolean isElementQualified(Value<?>... values) {
		return originalType.isElementQualified(values);
	}

	@Override
	public Group[] getGroups() {
		return originalType.getGroups();
	}

}
