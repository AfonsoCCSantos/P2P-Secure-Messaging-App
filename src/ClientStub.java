import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClientStub {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	public static void writeUsersFile(String username, int port, String ipAddress) {
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

}
