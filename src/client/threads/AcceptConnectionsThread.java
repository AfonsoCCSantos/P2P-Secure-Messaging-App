package client.threads;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivateKey;

import com.zaxxer.hikari.HikariDataSource;

import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;

public class AcceptConnectionsThread extends Thread {
	
	private int port;	
	private String username;
	private String topic;
	private PrivateKey privateKey;
	private PairingKeySerParameter attributesKey;
	private PairingKeySerParameter publicAttributesKey;
	private HikariDataSource dataSource;
	
	public AcceptConnectionsThread(int port, PrivateKey privateKey, HikariDataSource dataSource) {
		this.port = port;	
		this.privateKey = privateKey;
		this.dataSource = dataSource;
		this.username = null;
		this.topic = null;
	}
	
	public HikariDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(HikariDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public PairingKeySerParameter getAttributesKey() {
		return attributesKey;
	}

	public void setAttributesKey(PairingKeySerParameter attributesKey) {
		this.attributesKey = attributesKey;
	}

	public PairingKeySerParameter getPublicAttributesKey() {
		return publicAttributesKey;
	}

	public void setPublicAttributesKey(PairingKeySerParameter publicAttributesKey) {
		this.publicAttributesKey = publicAttributesKey;
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
				TalkToThread newTalkToThread = new TalkToThread(socket, this, privateKey);
				newTalkToThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
