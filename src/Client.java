import java.util.Scanner;

public class Client {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	public static void main(String[] args) {
		Scanner inputReader = new Scanner(System.in);
		if (args.length == 0) {
			System.err.println("You need to provide an username.");
			System.exit(-1);
		}
		
		String username = args[0];
		if (!validateUsername(username)) {
			System.err.println("Your username can not contain the - character");
			System.exit(-1);
		}
		
		Utils.createFile(USERS_FILE);
		
		int portNumber = Utils.generatePortNumber();
		String ipAddress = Utils.getIpAddress();
		
		
		showMenu();
		ReceiveMessagesThread receiveMessagesThread = initialiseReceiveSocket(portNumber);
		ClientStub clientStub = new ClientStub(username, receiveMessagesThread); 
		clientStub.writeUsersFile(username, portNumber, ipAddress);
		
		while(true) {
			String command = inputReader.nextLine();
			String[] tokens = command.split(" ");
			
			switch(tokens[0]) {
				case "talkTo":
					int resultCode = clientStub.talkTo(tokens[1]);
					if (resultCode == -1) {
						System.out.println("This user does not exist.");						
					}
					break;
					
			}
			
		}
		
	} 

	private static ReceiveMessagesThread initialiseReceiveSocket(int portNumber) {
		ReceiveMessagesThread receiveMessages = new ReceiveMessagesThread(portNumber);
		receiveMessages.start();
		return receiveMessages;
	}
	
	private static boolean validateUsername(String userName) {
		return !userName.contains("-");
	}
	
	private static void showMenu() {
		System.out.println("Commands:");
		System.out.println("talkTo <username> - Allows you to send messages to the user named username");
		System.out.println();
		System.out.println("Messages: ");
    }
	
	
}
