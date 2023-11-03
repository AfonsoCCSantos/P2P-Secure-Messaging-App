package models;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;

public class AttributeEncryptionObjects {
	
	PairingKeySerParameter publicKey;
	PairingKeySerParameter masterKey;
	KPABEEngine engine;;
	
	public AttributeEncryptionObjects(KPABEEngine engine, PairingKeySerParameter publicKey, PairingKeySerParameter masterKey) {
		this.engine = engine;
		this.publicKey = publicKey;
		this.masterKey = masterKey;
	}

	public KPABEEngine getEngine() {
		return engine;
	}
	
	public PairingKeySerParameter getPublicKey() {
		return publicKey;
	}
	
	public PairingKeySerParameter getMasterKey() {
		return masterKey;
	}

	public void setPublicKey(PairingKeySerParameter publicKey) {
		this.publicKey = publicKey;
	}

	public void setMasterKey(PairingKeySerParameter masterKey) {
		this.masterKey = masterKey;
	}
	
	public void setEngine(KPABEEngine engine) {
		this.engine = engine;
	}

}
