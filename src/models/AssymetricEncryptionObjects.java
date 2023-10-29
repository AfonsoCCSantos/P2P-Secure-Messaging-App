package models;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;

public class AssymetricEncryptionObjects {
	
	private KeyStore keystore;
	private PrivateKey privateKey;
	private Certificate certificate;
	
	public AssymetricEncryptionObjects(KeyStore keystore, PrivateKey privateKey, Certificate certificate) {
		this.keystore = keystore;
		this.privateKey = privateKey;
		this.certificate = certificate;
	}

	public KeyStore getKeystore() {
		return keystore;
	}

	public void setKeystore(KeyStore keystore) {
		this.keystore = keystore;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public Certificate getCertificate() {
		return certificate;
	}

	public void setCertificate(Certificate certificate) {
		this.certificate = certificate;
	}
}
