package utils;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import models.PBEEncryptionObjects;

public class EncryptionUtils {
	
	public static String rsaEncrypt(String message, PublicKey userToTalkPK) {
		String encryptedString = null;
		try {
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.ENCRYPT_MODE, userToTalkPK);
			byte[] encrypted = c.doFinal(message.getBytes());
			encryptedString = Base64.getEncoder().encodeToString(encrypted);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
			   | BadPaddingException e) {
			e.printStackTrace();
		}
		return encryptedString;
	}
	
	public static String rsaDecrypt(String message, PrivateKey privateKey) {
		String decryptedString = null;
		try {
			Cipher d = Cipher.getInstance("RSA");
			d.init(Cipher.DECRYPT_MODE, privateKey);
			byte[] decoded = Base64.getDecoder().decode(message);
			byte[] decryptedBytes = d.doFinal(decoded);
			decryptedString = new String(decryptedBytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return decryptedString;
	}
	
	public static IvParameterSpec generateIv() {
		byte[] iv = new byte[16];
		new SecureRandom().nextBytes(iv);
		return new IvParameterSpec(iv);
	}
	
	public static String encryptWithSecretKey(String message, PBEEncryptionObjects pbeEncryptionObjs) {
		String encrypted = null;
		try {
			Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			c.init(Cipher.ENCRYPT_MODE, pbeEncryptionObjs.getSecretKey(), pbeEncryptionObjs.getParams());
			byte[] encryptedBytes = c.doFinal(message.getBytes());
			encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return encrypted;
	}
	
	public static String decryptWithSecretKey(String message, PBEEncryptionObjects pbeEncryptionObjs) {
		String decrypted = null;
		try {
			Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			d.init(Cipher.DECRYPT_MODE, pbeEncryptionObjs.getSecretKey(), pbeEncryptionObjs.getParams());
			byte[] plainText = d.doFinal(Base64.getDecoder().decode(message));
			decrypted = new String(plainText);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return decrypted;
	}
	
	public static String aesEncrypt(String message,  SecretKey k, IvParameterSpec iv) {
		String encrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, k, iv);
			byte[] cipherText = cipher.doFinal(message.getBytes());
			encrypted = Base64.getEncoder().encodeToString(cipherText);
		} catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
		}
		return encrypted;
	}
	
	public static String aesDecrypt(String message,  SecretKey k, IvParameterSpec iv) {
		String decrypted = null;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, k, iv);
			byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(message));
			decrypted = new String(plainText);
		} catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}
		return decrypted;
	}
}
