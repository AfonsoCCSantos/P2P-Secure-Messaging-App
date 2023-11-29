import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import models.AttributeEncryptionObjects;
import server.threads.ServerThread;
import utils.Utils;

public class Server {
	
	private static final int PORT_NUMBER = 6789;
	
	public static void main(String[] args) { //Port will be 6789
		ServerSocket serverSocket = initialiseSocket();
		
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:sqlite:serverFiles/serverDatabase.db");
		HikariDataSource dataSource = new HikariDataSource(config);
		
		createTables(dataSource);
		
		AttributeEncryptionObjects attributeEncryptionObjects = getAttributeKeys();
		while (true) {
			Socket inSocket;
			try {
				inSocket = serverSocket.accept();
				ServerThread newServerThread = new ServerThread(inSocket, attributeEncryptionObjects, dataSource);
				newServerThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static void createTables(HikariDataSource dataSource) {
		Statement statement;
		
		try (Connection connection = dataSource.getConnection()) {
			statement = connection.createStatement();
			if (!Utils.tableExists(connection, "users")) {
                statement.execute("CREATE TABLE users ("
                        + "username TEXT PRIMARY KEY,"
                        + "ip_port TEXT)");
            }
            if (!Utils.tableExists(connection, "groups")) {
                statement.execute("CREATE TABLE groups ("
                        + "group_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "group_name TEXT UNIQUE,"
                        + "members TEXT)");
            }
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ServerSocket initialiseSocket() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(PORT_NUMBER);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serverSocket;
	}
	
	private static AttributeEncryptionObjects getAttributeKeys() {
		KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
		PairingParameters pairingParameters = PairingFactory.getPairingParameters("params/a_160_512.properties");
		//Pairing pairing = PairingFactory.getPairing(pairingParameters);
		
		PairingKeySerPair keyPair = engine.setup(pairingParameters, 50);
		PairingKeySerParameter publicKey = keyPair.getPublic();
		PairingKeySerParameter masterKey = keyPair.getPrivate();
		
		return new AttributeEncryptionObjects(engine, publicKey, masterKey);
	}
}
