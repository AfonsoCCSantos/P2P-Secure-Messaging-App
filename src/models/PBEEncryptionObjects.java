package models;

import java.security.AlgorithmParameters;

import javax.crypto.SecretKey;

public class PBEEncryptionObjects {
	
	private SecretKey secretKey;
	private AlgorithmParameters params;
	
	public PBEEncryptionObjects(SecretKey secretKey, AlgorithmParameters params) {
		this.secretKey = secretKey;
		this.params = params;
	}

	public SecretKey getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(SecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public AlgorithmParameters getParams() {
		return params;
	}

	public void setParams(AlgorithmParameters params) {
		this.params = params;
	}
	
}