package be.nabu.eai.module.wsdl.provider;

public enum SOAPVersion {
	SOAP_1_1(1.1),
	SOAP_1_2(1.2);
	
	private double version;

	private SOAPVersion(double version) {
		this.version = version;
	}
	public double getVersion() {
		return version;
	}
}
