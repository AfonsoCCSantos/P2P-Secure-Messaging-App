package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.Scanner;

import javax.crypto.SecretKey;

import client.threads.AcceptConnectionsThread;
import models.AssymetricEncryptionObjects;
import models.Constants;
import utils.EncryptionUtils;
import utils.Utils;

public class ClientStub {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	private String user;
	private AcceptConnectionsThread accepterThread;
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	private KeyStore keyStore;
	private PrivateKey privateKey;
	private Certificate certificate;
	private SecretKey attributesKey;
	
	public ClientStub(String user, AcceptConnectionsThread accepterThread, Socket talkToServer,
					  AssymetricEncryptionObjects assymEncObjects) {
		this.user = user;
		this.accepterThread = accepterThread;
		this.outToServer = Utils.gOutputStream(talkToServer);
		this.inFromServer = Utils.gInputStream(talkToServer);
		this.keyStore = assymEncObjects.getKeystore();
		this.privateKey = assymEncObjects.getPrivateKey();
		this.certificate = assymEncObjects.getCertificate();
	}
	
	public void login(String username, String ipAddress, int portNumber) {
		try {
			outToServer.writeObject("LOGIN");
			outToServer.writeObject(username);
			
			boolean userExists = (boolean) inFromServer.readObject();
			byte serverNonce = ((Long) inFromServer.readObject()).byteValue();
			
			Signature signature = Signature.getInstance("MD5withRSA");
			signature.initSign(privateKey);
			signature.update(serverNonce);
			byte[] signedNonce = signature.sign();
			outToServer.writeObject(signedNonce);
			
			if(!userExists) {
				outToServer.writeObject(this.certificate);
			}
			
			outToServer.writeObject(portNumber + " " + ipAddress);
			Constants serverMessage = (Constants) inFromServer.readObject();
			if (serverMessage != Constants.LOGIN_SUCCESSFUL && serverMessage != Constants.REGISTRATION_SUCESSFUL) {
				System.out.println(serverMessage);
				System.exit(-1);
			}
		} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
			e.printStackTrace();
		}
	}
	
	public void registerInUsersFile(String username, String ipAddress, int portNumber) {
		try {
			String certificateFileName = username + ".cer";
			outToServer.writeObject("WRITE_USERS_FILE");
			outToServer.writeObject(username + " " + portNumber + " " + ipAddress + " " + certificateFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int talkTo(String username) {
		Scanner sc = new Scanner(System.in);
		
		String ipPort = getUserIpPort(username);
		if (ipPort == null) return -1;
		
		PublicKey userToTalkPK = getUserPublicKey(username);
		if (userToTalkPK == null) return -1;
		
		accepterThread.setUsername(username);
		String[] ipPortTokens = ipPort.split(":");
		
		try {
			Socket socket = new Socket(ipPortTokens[0], Integer.parseInt(ipPortTokens[1]));
			ObjectOutputStream outToClient = Utils.gOutputStream(socket);
			System.out.println("--------------------------");
			System.out.println("Chat with: " + username);
			System.out.println("--------------------------");
			while (true) {
				String message = sc.nextLine();
				String encryptedMessage = EncryptionUtils.rsaEncrypt(message, userToTalkPK);
				if (message.equals(":q")) return 0;
				outToClient.writeObject(this.user + "-" + encryptedMessage);		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int createGroup(String topic) {
		try {
			//tells server to create new group with topic and username
			outToServer.writeObject("CREATE_GROUP");
			outToServer.writeObject(topic);
			outToServer.writeObject(user);
			
			//receive code of operation
			Boolean success = (Boolean) inFromServer.readObject();
						
			//return -1 if fail
			if(!success) return -1;	
			//se sucesso recebe chave
//			this.attributesKey = (SecretKey) inFromServer.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int joinGroup(String topic) {
		try {
			//tells server to add to group with topic and username
			outToServer.writeObject("JOIN_GROUP");
			outToServer.writeObject(topic);
			outToServer.writeObject(user);
			
			//receive code of operation
			Boolean success = (Boolean) inFromServer.readObject();
						
			//return -1 if fail
			if(!success) return -1;	
			//se sucesso recebe chave
//			this.attributesKey = (SecretKey) inFromServer.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public PublicKey getUserPublicKey(String username) {
		PublicKey pk = null;
		try {
			outToServer.writeObject("GET_USER_PK");
			outToServer.writeObject(username);
			pk = (PublicKey) inFromServer.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return pk;
	}
	
	public String getUserIpPort(String username) {
		String ipPort = null;
		try {
			outToServer.writeObject("GET_USER_IPPORT");
			outToServer.writeObject(username);
			ipPort = (String) inFromServer.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return ipPort;
	}
}
