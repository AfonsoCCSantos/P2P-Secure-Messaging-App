import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
				}
			} catch (ClassNotFoundException | IOException e) {
				break;
			}
			
		}
		
		
	}
	
	
	

}
