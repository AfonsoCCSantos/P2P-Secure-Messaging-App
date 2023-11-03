package client.threads;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.SecretKey;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import models.Message;
import utils.EncryptionUtils;
import utils.Utils;

public class TalkToThread extends Thread {
	
	private Socket socket;
	private AcceptConnectionsThread accepterThread;
	private PrivateKey privateKey;
	
	public TalkToThread(Socket inSocket, AcceptConnectionsThread accepterThread, PrivateKey privateKey) {
		this.socket = inSocket;
		this.accepterThread = accepterThread;
		this.privateKey = privateKey;
	}	
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		try {
			while (true) {
				Message messageReceived = (Message) in.readObject();
				String message = messageReceived.getMessage();
				//A mensagem tem metadata a indicar quem a enviou
				if (messageReceived.isGroup()) {
					//topic:userName-mensagemEnviada
					String[] tokens = message.split(":");
					String topic = tokens[0];
					String userName = tokens[1].split("-")[0];
					String text = message.substring(topic.length()+userName.length()+2);
					
					PairingCipherSerParameter encapsulationPairHeader = messageReceived.getEncapsulationPairHeader();
					byte[] ivBytes = messageReceived.getIv();
					IvParameterSpec iv = new IvParameterSpec(ivBytes);
					
					KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
					String[] attributes = new String[] {topic};
					byte[] sessionKey = engine.decapsulation(accepterThread.getPublicAttributesKey(), accepterThread.getAttributesKey(), attributes, encapsulationPairHeader);
					SecretKey k = new SecretKeySpec(Arrays.copyOfRange(sessionKey, 0, 16), "AES");
					String decrypted = EncryptionUtils.aesDecrypt(text, k, iv);
					
					if ((accepterThread.getTopic() == null && accepterThread.getUsername() == null) || (accepterThread.getTopic() != null && accepterThread.getTopic().equals(topic)))
						System.out.println("(" + topic +":" + userName + ")" + " - " + decrypted);
				}
				else {
					//userName-mensagemEnviada
					String userName = message.split("-")[0];
					String text = message.substring(userName.length()+1);
					String decryptedText = EncryptionUtils.rsaDecrypt(text, this.privateKey);
					if ((accepterThread.getTopic() == null && accepterThread.getUsername() == null) || (accepterThread.getUsername() != null && accepterThread.getUsername().equals(userName)))
						System.out.println("(" + userName + ")" + " - " + decryptedText);
				}
			}	
		} catch (ClassNotFoundException | IOException | InvalidCipherTextException e) {
			//Do Nothing
		}
	}
}
