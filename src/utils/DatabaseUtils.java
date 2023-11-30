package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.SecretKey;

import com.zaxxer.hikari.HikariDataSource;

import models.PBEEncryptionObjects;

public class DatabaseUtils {
	
	public static int createEntryInConversations(String conversationName, HikariDataSource dataSource) {
		String selectSql = "SELECT conversation_name FROM conversations WHERE conversation_name = ?";
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
	            preparedStatement.setString(1, conversationName);
	            try (ResultSet resultSet = preparedStatement.executeQuery()) {
	                if (resultSet.next()) {
	                	return 0;
	                } else {
	                    String insertSql = "INSERT INTO conversations (conversation_name, conversation_messages) VALUES (?,?)";
	                    try (PreparedStatement preparedStatementInsert = connection.prepareStatement(insertSql)) {
	                    	preparedStatementInsert.setString(1, conversationName);
	                    	preparedStatementInsert.setString(2, "");
	        	            int rowsAffected = preparedStatementInsert.executeUpdate();
	        	            if (rowsAffected == 1) return 0;
	        	            return -1;
	                    }
	                    
	                }
	            }
	        } catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
        return -1;
	}
	
	public static int registerMessageInConversations(String conversationName, HikariDataSource dataSource, String message,
													 PBEEncryptionObjects pbeEncryptionObjs) {
		String newMessages = null;
		String selectSql = "SELECT conversation_messages FROM conversations WHERE conversation_name = ?";
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
	            preparedStatement.setString(1, conversationName);
	            try (ResultSet resultSet = preparedStatement.executeQuery()) {
	            	message = EncryptionUtils.encryptWithSecretKey(message, pbeEncryptionObjs);
	                if (resultSet.next()) {
	                	newMessages = resultSet.getString("conversation_messages") + message + ";";
	                }
	                else {
						newMessages = message;
					}
	                String updateSql = "UPDATE conversations SET conversation_messages = ? WHERE conversation_name = ?";
	                    try (PreparedStatement preparedStatementInsert = connection.prepareStatement(updateSql)) {
	                    	preparedStatementInsert.setString(1, newMessages);
	                    	preparedStatementInsert.setString(2, conversationName);
	        	            int rowsAffected = preparedStatementInsert.executeUpdate();
	        	            if (rowsAffected == 1) return 0;
	        	            return -1;
	                    }
	            }
	        } catch (SQLException e) {
				e.printStackTrace();
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return -1;
	}

}
