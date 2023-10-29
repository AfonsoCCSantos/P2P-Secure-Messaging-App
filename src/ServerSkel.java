import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import models.Constants;

public class ServerSkel {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	private static final SecureRandom rndGenerator = new SecureRandom();
	ObjectInputStream in;
	ObjectOutputStream out;
	
	public ServerSkel(ObjectInputStream in, ObjectOutputStream out) {
		this.in = in;
		this.out = out;
	}
	
	public void loginUser() {
		try {
			String username = (String) in.readObject();
			
			//server verifies if user already exists and sends nonce
			boolean userExists = userExists(username);
			Long loginNonce = rndGenerator.nextLong();
			out.writeObject(userExists);
			out.writeObject(loginNonce);
			
			//receives signed nonce from client
			byte[] signedNonce = (byte[]) in.readObject();
			
			Signature signature = Signature.getInstance("MD5withRSA");
			
			if (!userExists)
				registerNewUser(signature, username, signedNonce, loginNonce);
			else
				loginKnownUser(signature, username, signedNonce, loginNonce);
			
		} catch (ClassNotFoundException | IOException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private static boolean userExists(String username) {
		String line = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = reader.readLine();
			while (line != null) {
				if (line.split("-")[0].equals(username)) {
					return true;				
				}
				line = reader.readLine(); 
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return false;
	}
	
	private void registerNewUser(Signature signature, String username, byte[] signedNonce, Long nonce) {
		//receives certificate and port and ip address from user
		Certificate userCertificate;
		try {
			userCertificate = (Certificate) in.readObject();
			String portIpAddress = (String) in.readObject();
			
			//verifies if nonce was signed with received certificate's public key
			PublicKey publicKey = userCertificate.getPublicKey();
			signature.initVerify(publicKey);
			signature.update(nonce.byteValue());
			
			if(!signature.verify(signedNonce)) {
				out.writeObject(Constants.REGISTRATION_FAILED);
				return;
			}
			
			//if public key is confirmed
			//save users certificate
			String userCertificateFile = username + ".cer";
			byte[] certificateEncoded = userCertificate.getEncoded();
			FileOutputStream fos = new FileOutputStream(userCertificateFile);
			fos.write(certificateEncoded);
			fos.close();
			
			//add user in users.txt
			String[] portIpAddressTokens = portIpAddress.split(" ");
			writeUsersFile(username, Integer.parseInt(portIpAddressTokens[0]), portIpAddressTokens[1], userCertificateFile);
			
			out.writeObject(Constants.REGISTRATION_SUCESSFUL);
		} catch (ClassNotFoundException | IOException | InvalidKeyException | SignatureException | CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loginKnownUser(Signature signature, String username, byte[] signedNonce, Long nonce) {
		try {
			//gets user certificate
			String userCertificateFile = username + ".cer";
			FileInputStream fis = new FileInputStream(userCertificateFile);
			CertificateFactory certFact = CertificateFactory.getInstance("X.509");
			Certificate userCertificate = certFact.generateCertificate(fis);
			
			//verifies if nonce was signed with received certificate's public key
			PublicKey publicKey = userCertificate.getPublicKey();
			signature.initVerify(publicKey);
			signature.update(nonce.byteValue());
			
			if(!signature.verify(signedNonce)) {
				out.writeObject(Constants.LOGIN_FAILED);
				return;
			}
			
			fis.close();
			
			//receives and updated user port and ipAddress in users.txt
			String portIpAddress = (String) in.readObject();
			String[] portIpAddressTokens = portIpAddress.split(" ");
			writeUsersFile(username, Integer.parseInt(portIpAddressTokens[0]), portIpAddressTokens[1], userCertificateFile);
			
			out.writeObject(Constants.LOGIN_SUCCESSFUL);
			
		} catch (ClassNotFoundException | IOException | InvalidKeyException | SignatureException | CertificateException e) {
			e.printStackTrace();
		}
	}

	public void writeUsersFile(String username, int port, String ipAddress, String userCertficateFile) {
		String fileLine = username + "-" + ipAddress + ":" + port + "-" + userCertficateFile +"\n";
		StringBuilder sb = new StringBuilder();
		String line = null;
		boolean added = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = reader.readLine();
			if (line == null) {
				sb.append(fileLine);
				added = true;
			}
			while (line != null) {
				if (!line.split("-")[0].equals(username)) {
					sb.append(line + "\n");					
				}
				else {
					sb.append(fileLine);
					added = true;
				}
				line = reader.readLine(); 
			}
			if (!added) sb.append(fileLine);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
			writer.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public PublicKey getPublicKey(String username) {
		PublicKey userToTalkPk = null;
		try {
			String userCertificateFile = username + ".cer";
			FileInputStream fis = new FileInputStream(userCertificateFile);
			CertificateFactory certFact = CertificateFactory.getInstance("X.509");
			Certificate userCertificate = certFact.generateCertificate(fis);
			
			userToTalkPk = userCertificate.getPublicKey();
		} catch (FileNotFoundException e) {
			return null;
		} catch (CertificateException e) {
			e.printStackTrace();
		}
		return userToTalkPk;
	}
	
	public String getIpPort(String username) {
		String line = null;
		String ipPort = null;
		try (BufferedReader in = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = in.readLine();
			while (line != null) {
				String[] tokens = line.split("-");
				if(tokens[0].equals(username)) {
					ipPort = tokens[1];
					break;
				}
				line = in.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ipPort;
	}

}
