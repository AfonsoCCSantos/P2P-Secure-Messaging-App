import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.security.PrivateKey;

import utils.EncryptionUtils;
import utils.Utils;

public class TalkToThread extends Thread {
	
	private Socket socket;
	private AcceptConnectionsThread accepterThread;
	private PrivateKey privateKey;
	
	public TalkToThread(Socket inSocket, AcceptConnectionsThread accepterThread, PrivateKey privateKey) {
		this.socket = inSocket;
		this.accepterThread = accepterThread;
		this.privateKey = privateKey;
	}	
	
	public void run() {
		ObjectInputStream in = Utils.gInputStream(socket);
		try {
			while (true) {
				String message = (String) in.readObject();
				//A mensagem tem metadata a indicar quem a enviou
				//userName-mensagemEnviada
				String userName = message.split("-")[0];
				String text = message.substring(userName.length()+1);
				String decryptedText = EncryptionUtils.rsaDecrypt(text, this.privateKey);
				if (accepterThread.getUsername() == null || accepterThread.getUsername().equals(userName))
					System.out.println("(" + userName + ")" + " - " + decryptedText);
			}	
		} catch (ClassNotFoundException | IOException e) {
			//Do Nothing
		}
	}
}
