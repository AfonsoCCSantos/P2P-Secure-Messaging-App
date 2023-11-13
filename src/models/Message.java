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
	private Long groupId;
	private byte[] mac;
	
	
	public Message(boolean isGroup, String message, PairingCipherSerParameter encapsulationPairHeader,
			byte[] iv, Long groupId) {
		super();
		this.isGroup = isGroup;
		this.message = message;
		this.encapsulationPairHeader = encapsulationPairHeader;
		this.ivBytes = iv;
		this.groupId = groupId;
	}
	
	public Message(boolean isGroup, String message, byte[] mac) {
		this.isGroup = isGroup;
		this.message = message;
		this.mac = mac;
	}
	
	public Long getGroupId() {
		return groupId;
	}


	public void setGroupId(Long groupId) {
		this.groupId = groupId;
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

	public byte[] getMac() {
		return mac;
	}

	public void setMac(byte[] mac) {
		this.mac = mac;
	}
}
