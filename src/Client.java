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
		
		Utils.createFile(USERS_FILE);
		
		int portNumber = Utils.generatePortNumber();
		String ipAddress = Utils.getIpAddress();
		ClientStub.writeUsersFile(username, portNumber, ipAddress);
		
		showMenu();
		initialiseReceiveSocket(portNumber);
	}

	private static void initialiseReceiveSocket(int portNumber) {
		ReceiveMessagesThread receiveMessagesThread = new ReceiveMessagesThread(portNumber);
		receiveMessagesThread.start();
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
