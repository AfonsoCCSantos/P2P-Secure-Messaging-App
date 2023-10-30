import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import server.threads.ServerThread;
import utils.Utils;

public class Server {
	
	private static final int PORT_NUMBER = 6789;
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	public static void main(String[] args) { //Port will be 6789
		ServerSocket serverSocket = initialiseSocket();
		Utils.createFile(USERS_FILE);

		while (true) {
			Socket inSocket;
			try {
				inSocket = serverSocket.accept();
				ServerThread newServerThread = new ServerThread(inSocket);
				newServerThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static ServerSocket initialiseSocket() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverSocket;
	}
	
	
	
	

}
