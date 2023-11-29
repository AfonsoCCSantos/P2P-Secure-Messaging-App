package models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import utils.models.ByteArray;

public class SSEObjects implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private byte[] bytesIvSSE;
	private HashMap<String,Integer> counters;
	private SecretKeySpec sk;
	private Map<ByteArray,ByteArray> index;
	
	public SSEObjects(byte[] bytesIvSSE, HashMap<String, Integer> counters, SecretKeySpec sk,
			Map<ByteArray, ByteArray> index) {
		this.bytesIvSSE = bytesIvSSE;
		this.counters = counters;
		this.sk = sk;
		this.index = index;
	}

	public byte[] getBytesIvSSE() {
		return bytesIvSSE;
	}

	public void setBytesIvSSE(byte[] bytesIvSSE) {
		this.bytesIvSSE = bytesIvSSE;
	}

	public HashMap<String, Integer> getCounters() {
		return counters;
	}

	public void setCounters(HashMap<String, Integer> counters) {
		this.counters = counters;
	}

	public SecretKeySpec getSk() {
		return sk;
	}

	public void setSk(SecretKeySpec sk) {
		this.sk = sk;
	}

	public Map<ByteArray, ByteArray> getIndex() {
		return index;
	}

	public void setIndex(Map<ByteArray, ByteArray> index) {
		this.index = index;
	}
}
