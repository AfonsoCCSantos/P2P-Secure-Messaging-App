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
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.zaxxer.hikari.HikariDataSource;

import utils.models.ByteArray;
import client.threads.AcceptConnectionsThread;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeyEncapsulationSerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import models.AssymetricEncryptionObjects;
import models.AuthenticatedMessage;
import models.Constants;
import models.Message;
import models.SSEObjects;
import utils.DatabaseUtils;
import utils.EncryptionUtils;
import utils.SSEUtils;
import utils.Utils;

public class ClientStub{
	
	private static final SecureRandom rndGenerator = new SecureRandom();
	
	private String user;
	private AcceptConnectionsThread accepterThread;
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	private PrivateKey privateKey;
	private Certificate certificate;
	private PairingKeySerParameter attributesKey;
	private PairingKeySerParameter publicAttributesKey;
	private HikariDataSource dataSource;
	//For searchable encryption
	private SSEObjects sseObjects;
	
	public ClientStub(String user, AcceptConnectionsThread accepterThread, Socket talkToServer,
					  AssymetricEncryptionObjects assymEncObjects, HikariDataSource dataSource,
					  SSEObjects sseObjects) {
		this.user = user;
		this.accepterThread = accepterThread;
		this.outToServer = Utils.gOutputStream(talkToServer);
		this.inFromServer = Utils.gInputStream(talkToServer);
		this.privateKey = assymEncObjects.getPrivateKey();
		this.certificate = assymEncObjects.getCertificate();
		this.dataSource = dataSource;
		//For searchable encryption------------------------------------------
		this.sseObjects = sseObjects;
		this.accepterThread.setSseObjects(sseObjects);
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
			
			//if we want to exchange something before the convo
			//for example symmetric key for mac
			
			//generate a secret key to use for MAC
			byte[] skBytes = new byte[32];
			rndGenerator.nextBytes(skBytes);
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec macKey = new SecretKeySpec(skBytes, "HmacSHA256");
			mac.init(macKey);
			outToClient.writeObject(macKey);
				
			System.out.println("--------------------------");
			System.out.println("Chat with: " + username);
			System.out.println("--------------------------");
			
			DatabaseUtils.createEntryInConversations(username, dataSource);
			while (true) {
				String typedMessage = sc.nextLine();
				if (typedMessage.equals(":q")) {
					accepterThread.setUsername(null);
					return 0;					
				} 
				String message = this.user + "-" + typedMessage;
				String encryptedMessage = EncryptionUtils.rsaEncrypt(message, userToTalkPK);
				Message messageToSend = new Message(false, encryptedMessage);
				AuthenticatedMessage authenticatedMessage = createAuthenticatedMessage(mac, messageToSend);
				outToClient.writeObject(authenticatedMessage);	
				DatabaseUtils.registerMessageInConversations(username, dataSource, message);
				for (String keyword : typedMessage.split(" ")) {
					SSEUtils.update(keyword, username, sseObjects);
				}
			}
		} catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int talkToGroup(String topic) {
		Scanner sc = new Scanner(System.in);
		
		try {
			outToServer.writeObject("GET_GROUP_IP_PORTS");
			outToServer.writeObject(topic);
			outToServer.writeObject(user);
			List<String> list = (List<String>) inFromServer.readObject();
			if (list == null) return -1;
			List<ObjectOutputStream> socketOutstreamList = new ArrayList<>();
			
			accepterThread.setTopic(topic);
			
			System.out.println("--------------------------");
			System.out.println("Chat Group : " + topic);
			System.out.println("--------------------------");
			
			for(String ipPort : list) {
				String[] ipPortTokens = ipPort.split(":");
				Socket socket = new Socket(ipPortTokens[0], Integer.parseInt(ipPortTokens[1]));
				ObjectOutputStream outToClient = Utils.gOutputStream(socket);
				socketOutstreamList.add(outToClient);
			}
			
			Long groupId = getTopicId(topic);
			
			//encapsulate session key
			KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
			String[] attributes = new String[] {groupId.toString()};
			PairingKeyEncapsulationSerPair encapsulationPair = engine.encapsulation(publicAttributesKey, attributes); 
			byte[] sessionKey = encapsulationPair.getSessionKey();
			
			SecretKey k1 = new SecretKeySpec(Arrays.copyOfRange(sessionKey, 0, 16), "AES");
			IvParameterSpec iv = EncryptionUtils.generateIv();
			
			byte[] skBytes = new byte[32];
			rndGenerator.nextBytes(skBytes);
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec macKey = new SecretKeySpec(skBytes, "HmacSHA256");
			mac.init(macKey);
			for (ObjectOutputStream outToClient : socketOutstreamList) {
				outToClient.writeObject(macKey);
			}
			
			DatabaseUtils.createEntryInConversations(topic, dataSource);
			while (true) {
				String typedMessage = sc.nextLine();
				if (typedMessage.equals(":q")) {
					accepterThread.setTopic(null);
					return 0;					
				} 
				
				//encrypt message
				String message = topic + ":" + this.user + "-" + typedMessage;
				String encrypted = EncryptionUtils.aesEncrypt(message, k1, iv);
				
				for (ObjectOutputStream outToClient : socketOutstreamList) {
					Message messageToSend = new Message(true, encrypted, encapsulationPair.getHeader(), iv.getIV(), groupId);
					AuthenticatedMessage authenticatedMessage = createAuthenticatedMessage(mac, messageToSend);
					outToClient.writeObject(authenticatedMessage);	
				}
				DatabaseUtils.registerMessageInConversations(topic, dataSource, this.user + "-" + typedMessage);
				for (String keyword : typedMessage.split(" ")) {
					SSEUtils.update(keyword, topic, sseObjects);
				}
			}
						
		} catch (IOException | ClassNotFoundException | NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public int createGroup(String topic) {
		try {
			//tells server to create new group with topic and username
			List<Long> groupsIds = getGroupsIds();
			
			outToServer.writeObject("CREATE_GROUP");
			outToServer.writeObject(topic);
			outToServer.writeObject(user);
			outToServer.writeObject(groupsIds);
			
			//receive code of operation
			// -2 if group already existed; -1 other error
			Long groupId = (Long) inFromServer.readObject();
			boolean failed = groupId < 0;
						
			//return -1 if fail
			if(failed) return groupId.intValue();	
			
			insertGroup(groupId, topic);
			
			//se sucesso recebe chave
			this.attributesKey = (PairingKeySerParameter) inFromServer.readObject();
			this.publicAttributesKey = (PairingKeySerParameter) inFromServer.readObject();
			this.accepterThread.setAttributesKey(this.attributesKey);
			this.accepterThread.setPublicAttributesKey(this.publicAttributesKey);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private List<Long> getGroupsIds() {
		List<Long> groupIdsList = new ArrayList<>();
		try (Connection connection = dataSource.getConnection()) {
            String selectSql = "SELECT group_id FROM groups";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        Long id = resultSet.getLong("group_id");
                        groupIdsList.add(id);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return groupIdsList;
	}
	
	private Long getTopicId(String topic) {
		Long groupId = null;
		try (Connection connection = dataSource.getConnection()) {
            String selectSql = "SELECT group_id FROM groups WHERE group_name = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
            	preparedStatement.setString(1, topic);
            	
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        groupId = resultSet.getLong("group_id");
                    } else {
		                System.out.println("User not in group with topic " + topic);
		            }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return groupId;
	}
	
	public void insertGroup(long groupId, String topic) {
		try (Connection connection = dataSource.getConnection()) {
			String insertSql = "INSERT INTO groups (group_id, group_name) VALUES (?, ?)";
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
	            preparedStatement.setLong(1, groupId);
	            preparedStatement.setString(2, topic);

	            int rowsAffected = preparedStatement.executeUpdate();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
	public int joinGroup(String topic) {
		try {
			//tells server to add to group with topic and username
			List<Long> groupsIds = getGroupsIds();
			outToServer.writeObject("JOIN_GROUP");
			outToServer.writeObject(topic);
			outToServer.writeObject(user);
			outToServer.writeObject(groupsIds);
			
			//receive code of operation
			Long groupId = (Long) inFromServer.readObject();
						
			boolean failed = groupId == -1;
			//return -1 if fail
			if(failed) return -1;	
			
			insertGroup(groupId, topic);
			
			//se sucesso recebe chave
			this.attributesKey = (PairingKeySerParameter) inFromServer.readObject();
			this.publicAttributesKey = (PairingKeySerParameter) inFromServer.readObject();
			this.accepterThread.setAttributesKey(this.attributesKey);
			this.accepterThread.setPublicAttributesKey(this.publicAttributesKey);
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
	
	public String searchKeyword(String keyword) {
		Set<String> docsWhereKeywordAppears = SSEUtils.search(keyword, sseObjects);
		StringBuilder sb = new StringBuilder(); //To build the message that will appears on the screen
		sb.append("\n");
		//Here we have the documents where the keywords appear, we will have to select the messages saved for that document
		//and check what messages have this keyword inside.
		if (docsWhereKeywordAppears == null) return null;
		try (Connection connection = dataSource.getConnection()) {
			for (String document : docsWhereKeywordAppears) {
				sb.append("Results in conversation '" + document + "':\n");
				String selectSql = "SELECT conversation_messages FROM conversations WHERE conversation_name = ?";
	            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
	            	preparedStatement.setString(1, document);
	            	String messagesOfConvo = null;
	                try (ResultSet resultSet = preparedStatement.executeQuery()) {
	                	if (resultSet.next()) {
	                		messagesOfConvo = resultSet.getString("conversation_messages");
	                		String[] messagesInSeparate = messagesOfConvo.split(";");
	                		for (String message : messagesInSeparate) {
	                			if (message.contains(keyword)) 
	                				sb.append("\t" + message + "\n");
	                		}
	                    } 
	                }
	            }
	            sb.append("\n");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	private AuthenticatedMessage createAuthenticatedMessage(Mac mac, Message messageToSend) {
		byte[] messageAsBytes = Utils.serializeObject(messageToSend);
		byte[] messageMac = mac.doFinal(messageAsBytes);
		AuthenticatedMessage authenticatedMessage = new AuthenticatedMessage(messageToSend, messageMac);
		return authenticatedMessage;
	}
}
