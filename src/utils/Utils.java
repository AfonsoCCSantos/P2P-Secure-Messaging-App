package utils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import models.AbeObjects;
import models.Message;
import models.SSEObjects;

public class Utils {
	
	public static int generatePortNumber() {
		int minPort = 49162;
		int maxPort = 65525;
		Random rd = new Random();
		return rd.nextInt(maxPort - minPort + 1) + minPort;
	}
	
	public static String getIpAddress() {
		return "127.0.0.1";
	}
	
	public static ObjectOutputStream gOutputStream(Socket socket) {
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return outStream;
    }
	
	public static ObjectInputStream gInputStream(Socket socket) {
        ObjectInputStream inStream = null;
        try {
            inStream = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return inStream;
    }
	
	public static void createFile(String fileName) {
		File file = new File(fileName);
		try {
			file.createNewFile();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static byte[] readFromFile(String filePath) {
		try {
			FileInputStream fis = new FileInputStream(filePath);
            int fileSize = (int) fis.available();
            byte[] data = new byte[fileSize];
            fis.read(data);
            fis.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
		return null;
	}
	
	public static void writeToFile(String filePath, byte[] data) {
		try {
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static byte[] serializeObject(Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
	
	public static void serializeSSEObjectToFile(SSEObjects obj, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        } catch (IOException e) {
        	return;
        }
    }
	
	public static void serializeAbeObjectsToFile(AbeObjects obj, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(obj);
        } catch (IOException e) {
        	return;
        }
    }
	
	public static SSEObjects deserializeSSEObjectFromFile(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (SSEObjects) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
        	e.printStackTrace();
            return null;
        }
    }
	
	public static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getTables(null, null, tableName, null);

        return resultSet.next();
    }
}
