import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveMessagesThread extends Thread {
	
	private int port;	
	
	public ReceiveMessagesThread(int port) {
		this.port = port;		
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
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				//O client pode estar numa conversa com alguem em especifico
				//Nesse caso só se mostra a mensagem se vier dessa pessoa
				String message = reader.readLine();
				//A mensagem tem metadata a indicar quem a enviou
				//userName-mensagemEnviada
				String userName = message.split("-")[0];
				System.out.println(message.substring(userName.length()+1));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
