package client.threads;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;

import com.zaxxer.hikari.HikariDataSource;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import models.AbeObjects;
import models.PBEEncryptionObjects;
import models.SSEObjects;

public class AcceptConnectionsThread extends Thread {
	
	private int port;	
	private String username;
	private String topic;
	private PrivateKey privateKey;
	private AbeObjects abeObjects;
	private HikariDataSource dataSource;
	private SSEObjects sseObjects;
	private PBEEncryptionObjects pbeEncryptionObjs;
	
	public AcceptConnectionsThread(int port, PrivateKey privateKey, HikariDataSource dataSource,
			                       PBEEncryptionObjects pbeEncryptionObjs, AbeObjects abeObjects) {
		this.port = port;	
		this.privateKey = privateKey;
		this.dataSource = dataSource;
		this.pbeEncryptionObjs = pbeEncryptionObjs;
		this.abeObjects = abeObjects;
		this.username = null;
		this.topic = null;
	}

	public PBEEncryptionObjects getPbeEncryptionObjs() {
		return pbeEncryptionObjs;
	}

	public void setPbeEncryptionObjs(PBEEncryptionObjects pbeEncryptionObjs) {
		this.pbeEncryptionObjs = pbeEncryptionObjs;
	}

	public HikariDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(HikariDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public AbeObjects getAbeObjects() {
		return abeObjects;
	}

	public void setAbeObjects(AbeObjects abeObjects) {
		this.abeObjects = abeObjects;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public SSEObjects getSseObjects() {
		return sseObjects;
	}

	public void setSseObjects(SSEObjects sseObjects) {
		this.sseObjects = sseObjects;
	}

	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);  
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			Socket socket;
			try {
				socket = serverSocket.accept();
				TalkToThread newTalkToThread = new TalkToThread(socket, this, privateKey, sseObjects, pbeEncryptionObjs);
				newTalkToThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
