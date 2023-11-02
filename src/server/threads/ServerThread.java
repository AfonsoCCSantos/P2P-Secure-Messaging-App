package server.threads;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;

import server.ServerSkel;
import utils.Utils;

public class ServerThread extends Thread {
	
	private Socket socket;
	
	public ServerThread(Socket socket) {
		this.socket = socket;
	}
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		ObjectOutputStream out = Utils.gOutputStream(socket);
		ServerSkel serverSkel = new ServerSkel(in, out);
		
		while(true) {
			try {
				String command = (String) in.readObject();
				switch (command) {
					case "LOGIN":
						serverSkel.loginUser();
						break;
					case "WRITE_USERS_FILE":
						String toWrite = (String) in.readObject();
						String[] toWriteTokens = toWrite.split(" ");
						System.out.println(toWrite);
						serverSkel.writeUsersFile(toWriteTokens[0], Integer.parseInt(toWriteTokens[1]), toWriteTokens[2], toWriteTokens[3]);
						break;
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
				}
			} catch (ClassNotFoundException | IOException e) {
				break;
			}
			
		}
		
		
	}
	
	
	

}
