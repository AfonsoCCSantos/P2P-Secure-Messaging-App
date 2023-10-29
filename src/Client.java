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
import java.util.Scanner;

public class Client {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
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
		
		var assymEncryptionObjs = keyStoreManage(username, args[1]);
		int portNumber = Utils.generatePortNumber();
		String ipAddress = Utils.getIpAddress();
		
		showMenu();
		Socket talkToServer = connectToServerSocket();
		AcceptConnectionsThread accepterThread = new AcceptConnectionsThread(portNumber, assymEncryptionObjs.getPrivateKey());
		ClientStub clientStub = new ClientStub(username, accepterThread, talkToServer, assymEncryptionObjs);
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
			}
		}
	} 
	
	private static boolean validateUsername(String userName) {
		return !userName.contains("-");
	}
	
	private static void showMenu() {
		System.out.println("Commands:");
		System.out.println("talkTo <username> - Allows you to send messages to the user named username");
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
			FileInputStream keyStoreFile = new FileInputStream("keystore." + username);
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
}
