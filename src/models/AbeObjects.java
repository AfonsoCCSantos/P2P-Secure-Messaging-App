package models;

import java.io.Serializable;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;

public class AbeObjects implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private PairingKeySerParameter attributesKey;
	private PairingKeySerParameter publicAttributesKey;
	
	public AbeObjects() {
		
	}
	
	public AbeObjects(PairingKeySerParameter attributesKey, PairingKeySerParameter publicAttributesKey) {
		this.attributesKey = attributesKey;
		this.publicAttributesKey = publicAttributesKey;
	}

	public PairingKeySerParameter getAttributesKey() {
		return attributesKey;
	}

	public void setAttributesKey(PairingKeySerParameter attributesKey) {
		this.attributesKey = attributesKey;
	}

	public PairingKeySerParameter getPublicAttributesKey() {
		return publicAttributesKey;
	}

	public void setPublicAttributesKey(PairingKeySerParameter publicAttributesKey) {
		this.publicAttributesKey = publicAttributesKey;
	}
}
