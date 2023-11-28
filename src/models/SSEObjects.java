package models;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import utils.models.ByteArray;

public class SSEObjects {
	
	private IvParameterSpec ivSSE;
	private Mac hmac;
	private Cipher aes;
	private HashMap<String,Integer> counters;
	private SecretKeySpec sk;
	private Map<ByteArray,ByteArray> index;
	
	public SSEObjects(IvParameterSpec ivSSE, Mac hmac, Cipher aes, HashMap<String, Integer> counters, SecretKeySpec sk,
			Map<ByteArray, ByteArray> index) {
		this.ivSSE = ivSSE;
		this.hmac = hmac;
		this.aes = aes;
		this.counters = counters;
		this.sk = sk;
		this.index = index;
	}

	public IvParameterSpec getIvSSE() {
		return ivSSE;
	}

	public void setIvSSE(IvParameterSpec ivSSE) {
		this.ivSSE = ivSSE;
	}

	public Mac getHmac() {
		return hmac;
	}

	public void setHmac(Mac hmac) {
		this.hmac = hmac;
	}

	public Cipher getAes() {
		return aes;
	}

	public void setAes(Cipher aes) {
		this.aes = aes;
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
