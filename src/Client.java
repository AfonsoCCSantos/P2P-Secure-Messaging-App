import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import client.ClientStub;
import client.threads.AcceptConnectionsThread;
import models.AssymetricEncryptionObjects;
import utils.Utils;

public class Client {
	
	private static final int SERVER_PORT_NUMBER = 6789;
	
	public static void main(String[] args) {
		Scanner inputReader = new Scanner(System.in);
		if (args.length < 2) {
			System.err.println("You need to provide an username and a password for the keystore.");
			System.exit(-1);
		}
		
		String username = args[0];
		if (!validateUsername(username)) {
			System.err.println("Your username can not contain the - character");
			System.exit(-1);
		}
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:" + username + "/client.db");
		HikariDataSource dataSource = new HikariDataSource(config);
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		createTables(connection);
		
		var assymEncryptionObjs = keyStoreManage(username, args[1]);
		int portNumber = Utils.generatePortNumber();
		String ipAddress = Utils.getIpAddress();
		
		showMenu();
		Socket talkToServer = connectToServerSocket();
		AcceptConnectionsThread accepterThread = new AcceptConnectionsThread(portNumber, assymEncryptionObjs.getPrivateKey());
		ClientStub clientStub = new ClientStub(username, accepterThread, talkToServer, assymEncryptionObjs, dataSource);
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
							System.out.println("NAO ENVIOU MENSAGEM.");	
							break;
						case 0:
							showMenu();
							break;
					}
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
    
    private static void createTables(Connection connection) {
		Statement statement;
		try {
			statement = connection.createStatement();
			statement.execute("DROP TABLE IF EXISTS groups");
			statement.execute("CREATE TABLE groups ("
                    + "group_id INTEGER PRIMARY KEY,"
                    + "group_name TEXT)");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
