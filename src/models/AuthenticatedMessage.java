package models;

import java.io.Serializable;

public class AuthenticatedMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private Message message;
	private byte[] mac;
	
	public AuthenticatedMessage(Message message, byte[] mac) {
		this.message = message;
		this.mac = mac;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public byte[] getMac() {
		return mac;
	}

	public void setMac(byte[] mac) {
		this.mac = mac;
	}
}
