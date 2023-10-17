import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AcceptConnectionsThread extends Thread {
	
	private int port;	
	private String username;
	
	public AcceptConnectionsThread(int port) {
		this.port = port;	
		this.username = null;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);  
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			Socket socket;
			try {
				socket = serverSocket.accept();
				TalkToThread newTalkToThread = new TalkToThread(socket, this);
				newTalkToThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
