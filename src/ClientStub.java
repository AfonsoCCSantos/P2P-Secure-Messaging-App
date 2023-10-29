import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Scanner;

public class ClientStub {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	private String user;
	private AcceptConnectionsThread accepterThread;
	private ObjectInputStream inFromServer;
	private ObjectOutputStream outToServer;
	private KeyStore keyStore;
	private PrivateKey privateKey;
	private Certificate certificate;
	
	public ClientStub(String user, AcceptConnectionsThread accepterThread, Socket talkToServer) {
		this.user = user;
		this.accepterThread = accepterThread;
		this.outToServer = Utils.gOutputStream(talkToServer);
		this.inFromServer = Utils.gInputStream(talkToServer);
	}

	public void keyStoreManage(String username, String keyStorePassword) {
		try {
			this.keyStore = KeyStore.getInstance("JCEKS");
			FileInputStream keyStoreFile = new FileInputStream("keystore." + username);
			this.keyStore.load(keyStoreFile, keyStorePassword.toCharArray());
			String alias = this.keyStore.aliases().nextElement();
			this.certificate = this.keyStore.getCertificate(alias);
			this.privateKey = (PrivateKey) this.keyStore.getKey(alias, keyStorePassword.toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | UnrecoverableKeyException e) {
			e.printStackTrace();
		}
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
		System.out.println(userToTalkPK);
		
		
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
				if (message.equals(":q")) return 0;
				outToClient.writeObject(this.user + "-" + message);		
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
