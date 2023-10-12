import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Random;

public class Utils {
	
	public static int generatePortNumber() {
		int minPort = 49162;
		int maxPort = 65525;
		Random rd = new Random();
		return rd.nextInt(maxPort - minPort + 1) + minPort;
	}
	
	public static String getIpAddress() {
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
	
	public static void createFile(String fileName) {
		File file = new File(fileName);
		try {
			file.createNewFile();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

}
