package client.threads;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.InvalidCipherTextException;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import models.AuthenticatedMessage;
import models.Message;
import models.PBEEncryptionObjects;
import models.SSEObjects;
import models.AbeObjects;
import utils.DatabaseUtils;
import utils.EncryptionUtils;
import utils.SSEUtils;
import utils.Utils;

public class TalkToThread extends Thread {
	
	private Socket socket;
	private AcceptConnectionsThread accepterThread;
	private PrivateKey privateKey;
	private SSEObjects sseObjects;
	private PBEEncryptionObjects pbeEncryptionObjs;
	
	public TalkToThread(Socket inSocket, AcceptConnectionsThread accepterThread, PrivateKey privateKey,
			            SSEObjects sseObjects, PBEEncryptionObjects pbeEncryptionObjs) {
		this.socket = inSocket;
		this.accepterThread = accepterThread;
		this.privateKey = privateKey;
		this.sseObjects = sseObjects;
		this.pbeEncryptionObjs = pbeEncryptionObjs;
	}	
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		boolean alreadyCheckedDatabaseEntry = false;
		
		//if you want to exchange something before the convo
		//for example a symmetric key for the convo or mac
		
		SecretKeySpec keyMac = null;
		Mac mac = null;
		try {
			keyMac = (SecretKeySpec) in.readObject();
			mac = Mac.getInstance("HmacSHA256");
			mac.init(keyMac);
		} catch (ClassNotFoundException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
			System.out.println("Failure setting up message integrity verification");
			return;
		}
		
		try {
			while (true) {
				String text = null;
				String conversationName = null;
				String username = null;
				AuthenticatedMessage authMessageReceived = (AuthenticatedMessage) in.readObject();
				Message messageReceived = authMessageReceived.getMessage();
				byte[] messageAsBytes = Utils.serializeObject(messageReceived);
				byte[] messageMac = mac.doFinal(messageAsBytes);
				if (!Arrays.equals(messageMac, authMessageReceived.getMac())) {
					System.out.println("Message was corrupted.");
					return;
				}
				String message = messageReceived.getMessage();
				//A mensagem tem metadata a indicar quem a enviou
				if (messageReceived.isGroup()) {
					//topic:userName-mensagemEnviada
					Long groupId = messageReceived.getGroupId();
					
					PairingCipherSerParameter encapsulationPairHeader = messageReceived.getEncapsulationPairHeader();
					byte[] ivBytes = messageReceived.getIv();
					IvParameterSpec iv = new IvParameterSpec(ivBytes);
					KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
					String[] attributes = new String[] {groupId.toString()};
					AbeObjects abeObjects = accepterThread.getAbeObjects();
					byte[] sessionKey = engine.decapsulation(abeObjects.getPublicAttributesKey(), abeObjects.getAttributesKey(), attributes, encapsulationPairHeader);
					SecretKey k = new SecretKeySpec(Arrays.copyOfRange(sessionKey, 0, 16), "AES");
					String decrypted = EncryptionUtils.aesDecrypt(message, k, iv);
					
					String[] tokens = decrypted.split(":");
					conversationName = tokens[0];
					String userName = tokens[1].split("-")[0];
					username = userName;
					text = decrypted.substring(conversationName.length()+userName.length()+2);
					
					if ((accepterThread.getTopic() == null && accepterThread.getUsername() == null) || (accepterThread.getTopic() != null && accepterThread.getTopic().equals(conversationName)))
						System.out.println("(" + conversationName +":" + userName + ")" + " - " + text);
				}
				
				else {
					//userName-mensagemEnviada
					String decryptedText = EncryptionUtils.rsaDecrypt(message, this.privateKey);
					conversationName = decryptedText.split("-")[0];
					username = conversationName;
					text = decryptedText.substring(conversationName.length()+1);
					if ((accepterThread.getTopic() == null && accepterThread.getUsername() == null) || (accepterThread.getUsername() != null && accepterThread.getUsername().equals(conversationName)))
						System.out.println("(" + conversationName + ")" + " - " + text);
				}
				
				if (!alreadyCheckedDatabaseEntry) {
					DatabaseUtils.createEntryInConversations(conversationName, accepterThread.getDataSource());
					alreadyCheckedDatabaseEntry = true;
				}
				
				String messageToSave = username + "-" + text;
				DatabaseUtils.registerMessageInConversations(conversationName, accepterThread.getDataSource(), messageToSave, pbeEncryptionObjs);
				for (String keyword : text.split(" ")) {
					SSEUtils.update(keyword, conversationName, sseObjects);
				}
			}	
		} catch (ClassNotFoundException | IOException | InvalidCipherTextException e) {
			//Do Nothing
		}
	}

	
	
}
