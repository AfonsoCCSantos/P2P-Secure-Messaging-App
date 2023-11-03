package models;

import java.io.Serializable;

import javax.crypto.spec.IvParameterSpec;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeyEncapsulationSerPair;

public class Message implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private boolean isGroup;
	private String message;
	private PairingCipherSerParameter encapsulationPairHeader;
	private byte[] ivBytes;
	
	public Message(boolean isGroup, String message, PairingCipherSerParameter encapsulationPairHeader,
			byte[] iv) {
		super();
		this.isGroup = isGroup;
		this.message = message;
		this.encapsulationPairHeader = encapsulationPairHeader;
		this.ivBytes = iv;
	}

	public boolean isGroup() {
		return isGroup;
	}

	public void setGroup(boolean isGroup) {
		this.isGroup = isGroup;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public PairingCipherSerParameter getEncapsulationPairHeader() {
		return encapsulationPairHeader;
	}

	public void setEncapsulationPairHeader(PairingCipherSerParameter encapsulationPairHeader) {
		this.encapsulationPairHeader = encapsulationPairHeader;
	}

	public byte[] getIv() {
		return ivBytes;
	}

	public void setIv(byte[] iv) {
		this.ivBytes = iv;
	}
}
