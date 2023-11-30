import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import client.ClientStub;
import client.threads.AcceptConnectionsThread;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import models.AbeObjects;
import models.AssymetricEncryptionObjects;
import models.PBEEncryptionObjects;
import models.SSEObjects;
import utils.Utils;
import utils.models.ByteArray;

public class Client {
	
	private static final int SERVER_PORT_NUMBER = 6789;
	private static final String hmac_alg = "HmacSHA1";
	private static final SecureRandom rndGenerator = new SecureRandom();
	private static String pathToSSEObjects;
	
	public static void main(String[] args) {
		Scanner inputReader = new Scanner(System.in);
		if (args.length < 3) {
			System.err.println("You need to provide an username, the keystore's password and the password to save the messages.");
			System.exit(-1);
		}
		
		String username = args[0];
		if (!validateUsername(username)) {
			System.err.println("Your username can not contain the - character");
			System.exit(-1);
		}
		
		pathToSSEObjects = username + "/sseObjects.txt";
		String pathToPBEParams = username + "/pbeParams.txt";
		String pathToAbeObjects = username + "/abeObjects.txt";
		
		//For PBE Encryption of saved messages
		SecretKey conversationsKey = getPasswordKey(args[2]);
		AlgorithmParameters params = getAlgorithmParameters(conversationsKey, pathToPBEParams);
		PBEEncryptionObjects pbeEncryptionObjs = new PBEEncryptionObjects(conversationsKey, params);
		
		AbeObjects abeObjects = getAbeObjects(pathToAbeObjects);
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:" + username + "/client.db");
		HikariDataSource dataSource = new HikariDataSource(config);
		
		createTables(dataSource);
		
		var assymEncryptionObjs = keyStoreManage(username, args[1]);
		int portNumber = Utils.generatePortNumber();
		String ipAddress = Utils.getIpAddress();
		
		showMenu();
		Socket talkToServer = connectToServerSocket();
		AcceptConnectionsThread accepterThread = new AcceptConnectionsThread(portNumber, assymEncryptionObjs.getPrivateKey(),
																			 dataSource, pbeEncryptionObjs, abeObjects);
		//For searchable encryption------------------------------------------------------
		//First check if there is already a file with the necessary objects.
		Path path = Paths.get(pathToSSEObjects);
		SSEObjects sseObjects = null;
		if (Files.exists(path)) {
			sseObjects = Utils.deserializeSSEObjectFromFile(pathToSSEObjects);
		}
		else {
			byte[] sk_bytes = new byte[20];
			byte[] iv_bytes = new byte[16];
			rndGenerator.nextBytes(sk_bytes);
			rndGenerator.nextBytes(iv_bytes);
			SecretKeySpec sk = new SecretKeySpec(sk_bytes, hmac_alg);
			HashMap<String,Integer> counters = new HashMap<>(100);
			Map<ByteArray,ByteArray> index = new HashMap<ByteArray,ByteArray>(1000);
			sseObjects = new SSEObjects(iv_bytes, counters, sk, index);
			Utils.serializeSSEObjectToFile(sseObjects, pathToSSEObjects);
		}
		
		ClientStub clientStub = new ClientStub(username, accepterThread, talkToServer, assymEncryptionObjs, 
											   dataSource, sseObjects, pbeEncryptionObjs, abeObjects);
		clientStub.login(username, ipAddress, portNumber);
		accepterThread.start();
		
		while(true) {
			String command = inputReader.nextLine();
			String[] tokens = command.split(" ");
			
			switch(tokens[0]) {
				case "talkTo":
					int resultCode = clientStub.talkTo(tokens[1]);
					switch (resultCode) {
						case -1:
							System.out.println("This user does not exist.");	
							break;
						case 0:
							System.out.println();
							showMenu();
							break;
					}
					break;
				case "createGroup":
					resultCode = clientStub.createGroup(tokens[1]);
					switch (resultCode) {
						case -2:
							System.out.println("This group already exists.");	
							System.out.println();
							System.out.println("Messages: ");
							break;
						case -1:
							System.out.println("An error occurred while creating the group");
							System.out.println();
							System.out.println("Messages: ");
							break;
						case 0:
							System.out.println("Group was created!");
							System.out.println();
							System.out.println("Messages: ");
							//showMenu();
							break;
					}
					break;
				case "joinGroup":
					resultCode = clientStub.joinGroup(tokens[1]);
					switch (resultCode) {
						case -1:
							System.out.println("This group does not exist.");	
							break;
						case 0:
							System.out.println("You joined the group!");
							System.out.println();
							System.out.println("Messages: ");
							//showMenu();
							break;
					}
					break;	
				case "talkToGroup":
					resultCode = clientStub.talkToGroup(tokens[1]);
					switch (resultCode) {
						case -1:
							System.out.println("The group does not exist.");	
							break;
						case 0:
							System.out.println();
							showMenu();
							break;
					}
					break;
				case "searchKeyword":
					String toDisplay = clientStub.searchKeyword(tokens[1]);
					if (toDisplay != null) {
						System.out.println("Results: ");
						System.out.println(toDisplay);
					}
					else {
						System.out.println("No results found. ");
					}
					System.out.println();
					break;
				case "quit":
					clientStub.logout(pathToSSEObjects, pathToAbeObjects);
					System.out.println("Bye!");
					dataSource.close();
					System.exit(0);
					break;
			}
		}
	} 

	private static boolean validateUsername(String userName) {
		return !userName.contains("-");
	}
	
	private static void showMenu() {
		System.out.println("Commands:");
		System.out.println("talkTo <username> - Allows you to send messages to the user named username");
		System.out.println("talkToGroup <topic> - Allows you to send messages to the group with the given topic");
		System.out.println("createGroup <topic> - Allows you to create a new group with the given topic");
		System.out.println("joinGroup <topic> - Allows you to join the group with the given topic");
		System.out.println("searchKeyword <keyword> - Allows you to retrieve the messages where the given keyword appears");
		System.out.println("quit - Safely quits the application");
		System.out.println();
		System.out.println("Messages: ");
    }
    
    private static Socket connectToServerSocket() {
		Socket socket = null;
		try {
			socket = new Socket("127.0.0.1", SERVER_PORT_NUMBER);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socket;
	}
    
    public static AssymetricEncryptionObjects keyStoreManage(String username, String keyStorePassword) {
    	AssymetricEncryptionObjects toReturn = null;
		try {
			KeyStore keyStore = KeyStore.getInstance("JCEKS");
			FileInputStream keyStoreFile = new FileInputStream(username + "/keystore." + username);
			keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
			String alias = keyStore.aliases().nextElement();
			Certificate certificate = keyStore.getCertificate(alias);
			PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyStorePassword.toCharArray());
			toReturn = new AssymetricEncryptionObjects(keyStore, privateKey, certificate);
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException e) {
			e.printStackTrace();
		}
		return toReturn;
	}
    
    private static void createTables(HikariDataSource dataSource) {
		Statement statement;
		try (Connection connection = dataSource.getConnection()) {
			try {
				statement = connection.createStatement();
				if (!Utils.tableExists(connection, "groups")) {
	                statement.execute("CREATE TABLE groups ("
	                        + "group_id INTEGER PRIMARY KEY,"
	                        + "group_name TEXT)");
	            }
	            if (!Utils.tableExists(connection, "conversations")) {
	            	statement.execute("CREATE TABLE conversations ("
	                        + "conversation_name TEXT PRIMARY KEY,"
	                        + "conversation_messages TEXT)");
	            }
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}
    
    private static SecretKey getPasswordKey(String password) {
		byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99,
				(byte) 0x52, (byte) 0x3e, (byte) 0xea, (byte) 0xf2 };
		PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20);
		SecretKeyFactory kf = null;
		SecretKey key;
		try {
			kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
			key = kf.generateSecret(keySpec);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return key;
	}
    
    private static AbeObjects getAbeObjects(String pathtoAbeObjects) {
    	AbeObjects abeObjects = new AbeObjects();
    	Path path = Paths.get(pathtoAbeObjects);
    	if (Files.exists(path)) {
    		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pathtoAbeObjects))) {
    			abeObjects = (AbeObjects) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
    	}
    	return abeObjects;
    }
    
	private static AlgorithmParameters getAlgorithmParameters(SecretKey conversationsKey, String pathToPBEParams) {
		AlgorithmParameters params = null;
		try {
			Path path = Paths.get(pathToPBEParams);
			params = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");
			byte[] paramsBytes;
			if (Files.exists(path)) {
				paramsBytes = Utils.readFromFile(pathToPBEParams);
				params.init(paramsBytes);
			}
			else {
				Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
				c.init(Cipher.ENCRYPT_MODE, conversationsKey);
				paramsBytes = c.getParameters().getEncoded();
				params.init(paramsBytes);
				Utils.writeToFile(pathToPBEParams, paramsBytes);
			}
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException e) {
			e.printStackTrace();
		}
		return params;
	}
}
