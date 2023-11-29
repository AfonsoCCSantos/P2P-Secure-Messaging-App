package utils;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import models.SSEObjects;
import utils.models.ByteArray;

public class SSEUtils {
	
	private static final String hmac_alg = "HmacSHA1";
	private static final String cipher_alg = "AES";
	
	public static void update(String keyword, String documentName, SSEObjects sseObjects) {
		try {
			Mac hmac = Mac.getInstance(hmac_alg);
			Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
			//generate keys k1 and k2 from keyword and sk by using a PRF
			byte[] w1 = keyword.concat("1").getBytes();
			byte[] w2 = keyword.concat("2").getBytes();
			hmac.init(sseObjects.getSk());
			byte[] k1Bytes = hmac.doFinal(w1);
			byte[] k2Bytes = hmac.doFinal(w2);
			byte[] k2Bytes16 = Arrays.copyOf(k2Bytes, 16);
			SecretKeySpec key1 = new SecretKeySpec(k1Bytes, hmac_alg);
			SecretKeySpec key2 = new SecretKeySpec(k2Bytes16, cipher_alg);
			
			//get the counter c for keyword from counters, or set it at 0 if not found
			Integer counter = sseObjects.getCounters().get(keyword);
			if (counter == null) counter = 0;
			BigInteger c = BigInteger.valueOf(counter);
			
			//calculate the index label l through a PRF and using k1 as key and c as plaintext
			hmac.init(key1);
			ByteArray indexLabelL = new ByteArray(hmac.doFinal(c.toByteArray()));
			
			//calculate the index value d through a symmetric-key cipher and using k2 as key and docId as plaintext
			IvParameterSpec ivSSE = new IvParameterSpec(sseObjects.getBytesIvSSE());
			aes.init(Cipher.ENCRYPT_MODE, key2, ivSSE);
			ByteArray indexValueD = new ByteArray(aes.doFinal(documentName.getBytes()));
			
			//send l and d to update the index
			sseObjects.getIndex().put(indexLabelL, indexValueD);
			
			//increment counter c and update it in counters
			sseObjects.getCounters().put(keyword, ++counter);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Set<String> search(String keyword, SSEObjects sseObjects) {
		Set<String> results = null;
		try {
			Mac hmac = Mac.getInstance(hmac_alg);
			Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] w1 = keyword.concat("1").getBytes();
			byte[] w2 = keyword.concat("2").getBytes();
			hmac.init(sseObjects.getSk());
			byte[] k1Bytes = hmac.doFinal(w1);
			byte[] k2Bytes = hmac.doFinal(w2);
			
			//start with counter 0
			BigInteger c = BigInteger.valueOf(0);
			
			//calculate index label with counter and k1, through a PRF
			SecretKeySpec key1 = new SecretKeySpec(k1Bytes, hmac_alg);
			k2Bytes = Arrays.copyOf(k2Bytes, 16);
			SecretKeySpec key2 = new SecretKeySpec(k2Bytes, cipher_alg);
			hmac.init(key1);
			ByteArray indexLabel = new ByteArray(hmac.doFinal(c.toByteArray()));
			//access the index with the label
			//if entry not found, stop
			//if entry found, decrypt it with k2 through cipher
			//add decrypted docId to list of results and increment counter
			//repeat
			results = new HashSet<String>();
			IvParameterSpec ivSSE = new IvParameterSpec(sseObjects.getBytesIvSSE());
			while (true) {
				ByteArray val = sseObjects.getIndex().get(indexLabel);
				if (val == null) break;
				aes.init(Cipher.DECRYPT_MODE, key2, ivSSE);
				byte[] decrypted = aes.doFinal(val.getArr());
				results.add(new String(decrypted));
				c = c.add(BigInteger.valueOf(1));
				indexLabel = new ByteArray(hmac.doFinal(c.toByteArray()));
			}
			return results;
		} catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return results;
	}
}
