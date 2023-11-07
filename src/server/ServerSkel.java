package server;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;

import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import models.AttributeEncryptionObjects;
import models.Constants;
import models.Group;

public class ServerSkel {
	
	//private static final String USERS_FILE = "serverFiles/users.txt"; //userName-ip:port
	//private static final String GROUPS_FILE = "serverFiles/groups.txt"; //groupTpoic-owner;user1;user2;...
	
	private static final SecureRandom rndGenerator = new SecureRandom();
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private AttributeEncryptionObjects attributeEncryptionObjects;
	private HikariDataSource dataSource;
	
	public ServerSkel(AttributeEncryptionObjects attributeEncryptionObjects, ObjectInputStream in, ObjectOutputStream out, HikariDataSource dataSource) {
		this.in = in;
		this.out = out;
		this.attributeEncryptionObjects = attributeEncryptionObjects;
		this.dataSource = dataSource;
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
	
	public void createNewGroup(String topic, String username) {
		//write in new group in groups file
		int opCode = insertNewGroup(topic, username);
		try {
			//tells user if it succeeded
			Boolean success = opCode == 0;
			out.writeObject(success);
			
			if (!success) return;
			
			//generates new key to policy with topic
			String policy = topic;
			int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
			String[] rhos = ParserUtils.GenerateRhos(policy);
			PairingKeySerParameter secretKey = attributeEncryptionObjects.getEngine().keyGen(attributeEncryptionObjects.getPublicKey(), attributeEncryptionObjects.getMasterKey(), accessPolicy, rhos);
			out.writeObject(secretKey);
			out.writeObject(attributeEncryptionObjects.getPublicKey());
			
		} catch (IOException | PolicySyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public void addUserToGroup(String topic, String username) {
		//write in new group in groups file
		int opCode = insertMemberIntoGroup(topic, username);
		try {
			//tells user if it succeeded
			Boolean success = opCode == 0;
			out.writeObject(success);
			
			if (!success) return;
			
			//generates new key to policy with topic
			String policy = topic;
			int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
			String[] rhos = ParserUtils.GenerateRhos(policy);
			PairingKeySerParameter secretKey = attributeEncryptionObjects.getEngine().keyGen(attributeEncryptionObjects.getPublicKey(), attributeEncryptionObjects.getMasterKey(), accessPolicy, rhos);
			out.writeObject(secretKey);
			out.writeObject(attributeEncryptionObjects.getPublicKey());
			
		} catch (IOException | PolicySyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getIpPortOfGroup(String topic, String username) {
		String membersList = null;
		List<String> ipPorts = new ArrayList<String>();
		try (Connection connection = dataSource.getConnection()) {
            // Select members for the given group name
            String selectSql = "SELECT members FROM groups WHERE group_name = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                preparedStatement.setString(1, topic);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        membersList = resultSet.getString("members");
                        System.out.println("Members of group " + topic + ": " + membersList);
                    } else {
                        System.out.println("Group not found: " + topic);
                        return null;
                    }
                }
            }
    		String[] members = membersList.split(";");
    		for (int i = 0; i < members.length; i++) {
				if(!members[i].equals(username)) {
					String ipPort = getIpPort(members[i]);
    				ipPorts.add(ipPort);
				}
    		}
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return ipPorts;
	}

	private boolean userExists(String username) {
		try (Connection connection = dataSource.getConnection()) {
            // Retrieve the user's "ip_port" by username
            String selectSql = "SELECT ip_port FROM users WHERE username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        System.out.println("User exists");
                    } else {
                        System.out.println("User not found: " + username);
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return true;
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
			FileOutputStream fos = new FileOutputStream("serverFiles/" + userCertificateFile);
			fos.write(certificateEncoded);
			fos.close();
			
			//add user in users.txt
			String[] portIpAddressTokens = portIpAddress.split(" ");
			insertUser(username, Integer.parseInt(portIpAddressTokens[0]), portIpAddressTokens[1]);
			
			out.writeObject(Constants.REGISTRATION_SUCESSFUL);
		} catch (ClassNotFoundException | IOException | InvalidKeyException | SignatureException | CertificateEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loginKnownUser(Signature signature, String username, byte[] signedNonce, Long nonce) {
		try {
			//gets user certificate
			String userCertificateFile = "serverFiles/" +  username + ".cer";
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
			insertUser(username, Integer.parseInt(portIpAddressTokens[0]), portIpAddressTokens[1]);
			
			out.writeObject(Constants.LOGIN_SUCCESSFUL);
			
		} catch (ClassNotFoundException | IOException | InvalidKeyException | SignatureException | CertificateException e) {
			e.printStackTrace();
		}
	}

	public void insertUser(String username, int port, String ipAddress) {
		try (Connection connection = dataSource.getConnection()) {
			String insertSql = "INSERT INTO users (username, ip_port) VALUES (?, ?)";
			String ipPort = ipAddress + ":" + port;
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
	            preparedStatement.setString(1, username);
	            preparedStatement.setString(2, ipPort);

	            int rowsAffected = preparedStatement.executeUpdate();
	            if (rowsAffected == 1) {
	                System.out.println("User inserted successfully.");
	            } else {
	                System.out.println("User insertion failed.");
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
	}
	
	public int insertMemberIntoGroup(String topic, String username) {
		String members = null;
		try (Connection connection = dataSource.getConnection()) {
            // Select members for the given group name
            String selectSql = "SELECT members FROM groups WHERE group_name = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                preparedStatement.setString(1, topic);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        members = resultSet.getString("members");
                        System.out.println("Members of group " + topic + ": " + members);
                    } else {
                        System.out.println("Group not found: " + topic);
                        return -1;
                    }
                }
            }
    		members = members + ";" + username;
			String updateSql = "UPDATE Groups SET members = ? WHERE group_name = ?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(updateSql)) {
	            preparedStatement.setString(1, members);
	            preparedStatement.setString(2, topic);
	
	            int rowsAffected = preparedStatement.executeUpdate();
	            if (rowsAffected == 1) {
	                System.out.println("User inserted successfully into group.");
	            } else {
	                System.out.println("User insertion failed.");
	                return -1;
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return 0;
	}
	
	public int insertNewGroup(String topic, String username) {
		try (Connection connection = dataSource.getConnection()) {
			String insertSql = "INSERT INTO groups (group_name, members) VALUES (?, ?)";
			try (PreparedStatement preparedStatement = connection.prepareStatement(insertSql)) {
	            preparedStatement.setString(1, topic);
	            preparedStatement.setString(2, username);

	            int rowsAffected = preparedStatement.executeUpdate();
	            if (rowsAffected == 1) {
	                System.out.println("User inserted successfully.");
	            } else {
	                System.out.println("User insertion failed.");
	                return -1;
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return 0;
	}
	
	public PublicKey getPublicKey(String username) {
		PublicKey userToTalkPk = null;
		try {
			String userCertificateFile = "serverFiles/" + username + ".cer";
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
		String ipPort = null;
		try (Connection connection = dataSource.getConnection()) {
            // Retrieve the user's "ip_port" by username
            String selectSql = "SELECT ip_port FROM users WHERE username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
                preparedStatement.setString(1, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        ipPort = resultSet.getString("ip_port");
                        System.out.println("IP Port for user " + username + ": " + ipPort);
                    } else {
                        System.out.println("User not found: " + username);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
		return ipPort;
	}

}
