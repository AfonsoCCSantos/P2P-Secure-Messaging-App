import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Random;
import java.net.*;
import java.io.*;

public class Client {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("You need to provide an username.");
			System.exit(-1);
		}
		
		String username = args[0];
		if (!validateUsername(username)) {
			System.err.println("Your username can not contain the - character");
			System.exit(-1);
		}
		
		int portNumber = generatePortNumber();
		String ipAddress = getIpAddress();
		writeUsersFile(username, portNumber, ipAddress);
		
		showMenu();
		initialiseReceiveSocket(portNumber);
	}

	private static void initialiseReceiveSocket(int portNumber) {
		ReceiveMessagesThread receiveMessagesThread = new ReceiveMessagesThread(portNumber);
		receiveMessagesThread.start();
	}
	
	public static int generatePortNumber() {
		int minPort = 49162;
		int maxPort = 65525;
		Random rd = new Random();
		return rd.nextInt(maxPort - minPort + 1) + minPort;
	}
	
	private static void writeUsersFile(String username, int port, String ipAddress) {
		String fileLine = username + "-" + ipAddress + ":" + port + "\n";
		StringBuilder sb = new StringBuilder();
		String line = null;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = reader.readLine();
			if (line == null) sb.append(fileLine);
			while (line != null) {
				if (!line.split("-")[0].equals(username)) {
					sb.append(line);					
				}
				else {
					sb.append(fileLine);
				}
				line = reader.readLine(); 
			}
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
	
	private static boolean validateUsername(String userName) {
		return !userName.contains("-");
	}
	
	private static String getIpAddress() {
		URL whatismyip = null;
		String ip = null;
		try {
			whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
	        ip = in.readLine();
	        in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ip;
	}
	
	private static void showMenu() {
		System.out.println("Commands:");
		System.out.println("talkTo <username> - Allows you to send messages to the user named username");
		System.out.println();
		System.out.println("Messages: ");
    }
}
