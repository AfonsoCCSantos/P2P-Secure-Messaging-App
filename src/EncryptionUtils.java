import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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

}
