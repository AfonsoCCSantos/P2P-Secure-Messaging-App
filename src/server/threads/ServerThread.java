package server.threads;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.List;

import com.zaxxer.hikari.HikariDataSource;

import models.AttributeEncryptionObjects;
import server.ServerSkel;
import utils.Utils;

public class ServerThread extends Thread {
	
	private Socket socket;
	private AttributeEncryptionObjects attributeEncryptionObjects;
	private HikariDataSource dataSource;
	
	public ServerThread(Socket socket, AttributeEncryptionObjects attributeEncryptionObjects, HikariDataSource dataSource) {
		this.socket = socket;
		this.attributeEncryptionObjects = attributeEncryptionObjects;
		this.dataSource = dataSource;
	}
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		ObjectOutputStream out = Utils.gOutputStream(socket);
		ServerSkel serverSkel = new ServerSkel(attributeEncryptionObjects, in, out, dataSource);
		
		while(true) {
			try {
				String command = (String) in.readObject();
				switch (command) {
					case "LOGIN":
						serverSkel.loginUser();
						break;
//					case "WRITE_USERS_FILE":
//						String toWrite = (String) in.readObject();
//						String[] toWriteTokens = toWrite.split(" ");
//						System.out.println(toWrite);
//						serverSkel.writeUsersFile(toWriteTokens[0], Integer.parseInt(toWriteTokens[1]), toWriteTokens[2], toWriteTokens[3]);
//						break;
					case "GET_USER_IPPORT":
						String username = (String) in.readObject();
						String ipPort = serverSkel.getIpPort(username);
						out.writeObject(ipPort);
						break;
					case "GET_USER_PK":
						username = (String) in.readObject();
						PublicKey pk = serverSkel.getPublicKey(username);
						out.writeObject(pk);
						break;
					case "CREATE_GROUP":
						String topic = (String) in.readObject();
						username = (String) in.readObject();
						serverSkel.createNewGroup(topic, username);
						break;	
					case "JOIN_GROUP":
						topic = (String) in.readObject();
						username = (String) in.readObject();
						serverSkel.addUserToGroup(topic, username);
						break;		
					case "GET_GROUP_IP_PORTS":
						topic = (String) in.readObject();
						username = (String) in.readObject();
						List<String> list = serverSkel.getIpPortOfGroup(topic, username);
						out.writeObject(list);
						break;		
				}
			} catch (ClassNotFoundException | IOException e) {
				break;
			}
			
		}
		
		
	}
	
	
	

}
