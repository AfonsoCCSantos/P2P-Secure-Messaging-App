import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class ServerSkel {
	
	private static final String USERS_FILE = "users.txt"; //userName-ip:port
	
	public void writeUsersFile(String username, int port, String ipAddress) {
		String fileLine = username + "-" + ipAddress + ":" + port + "\n";
		StringBuilder sb = new StringBuilder();
		String line = null;
		boolean added = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(USERS_FILE)))) {
			line = reader.readLine();
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

}
