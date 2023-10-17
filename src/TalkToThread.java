import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class TalkToThread extends Thread {
	
	private Socket socket;
	private AcceptConnectionsThread accepterThread;
	
	public TalkToThread(Socket inSocket, AcceptConnectionsThread accepterThread) {
		this.socket = inSocket;
		this.accepterThread = accepterThread;
	}	
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		try {
			while (true) {
				String message = (String) in.readObject();
				//A mensagem tem metadata a indicar quem a enviou
				//userName-mensagemEnviada
				String userName = message.split("-")[0];
				if (accepterThread.getUsername() == null || accepterThread.getUsername().equals(userName))
					System.out.println("(" + userName + ")" + " - " + message.substring(userName.length()+1));
			}	
		} catch (ClassNotFoundException | IOException e) {
			//Do Nothing
		}
	}
}
