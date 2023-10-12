import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientStub {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	private String user;
	private ReceiveMessagesThread receiveMessagesThread;
	
	public ClientStub(String user, ReceiveMessagesThread receiveMessagesThread) {
		this.user = user;
		this.receiveMessagesThread = receiveMessagesThread;
	}
	 
	public void writeUsersFile(String username, int port, String ipAddress) {
		String fileLine = username + "-" + ipAddress + ":" + port + "\n";
		StringBuilder sb = new StringBuilder();
		String line = null;
		boolean added = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = reader.readLine();
			System.out.println(line);
			if (line == null) {
				sb.append(fileLine);
				added = true;
			}
			while (line != null) {
				if (!line.split("-")[0].equals(username)) {
					sb.append(line + "\n");					
				}
				else {
					sb.append(fileLine);
					added = true;
				}
				line = reader.readLine(); 
			}
			if (!added) sb.append(fileLine);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
			writer.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public int talkTo(String username) {
		Scanner sc = new Scanner(System.in);
		String ipPort = getUserIpPort(username);
		if (ipPort == null) return -1;
		
		receiveMessagesThread.setUsername(username);
		String[] ipPortTokens = ipPort.split(":");
		
		try {
			Socket socket = new Socket(ipPortTokens[0], Integer.parseInt(ipPortTokens[1]));
			ObjectOutputStream out = Utils.gOutputStream(socket);
			
			while (true) {
				System.out.print("Enter your message: ");
				String message = sc.nextLine();
				if (message.equals(":q")) return 0;
				out.writeObject(this.user + "-" + message);		
				System.out.println("(Me)" + " - " + message);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public static String getUserIpPort(String username) {
		String line = null;
		String ipPort = null;
		try (BufferedReader in = new BufferedReader(new FileReader(new File(USERS_FILE)))){
			line = in.readLine();
			String[] tokens = line.split("-"); 
			if(tokens[0].equals(username))
				ipPort = tokens[1];
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ipPort;
	}
	

	

}
