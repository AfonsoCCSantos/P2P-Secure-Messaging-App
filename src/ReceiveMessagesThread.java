import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveMessagesThread extends Thread {
	
	private int port;	
	private String username;
	
	public ReceiveMessagesThread(int port) {
		this.port = port;	
		this.username = null;
	}
	
	public void setUsername(String username) {
		this.username = username;
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
				//O client pode estar numa conversa com alguem em especifico
				//Nesse caso só se mostra a mensagem se vier dessa pessoa
				
				ObjectInputStream in = Utils.gInputStream(socket);
				String message = (String) in.readObject();
				
				//A mensagem tem metadata a indicar quem a enviou
				//userName-mensagemEnviada
				String userName = message.split("-")[0];
				if (this.username == null || this.username.equals(userName)) {
					System.out.println("(" + userName + ")" + " - " + message.substring(userName.length()+1));
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
