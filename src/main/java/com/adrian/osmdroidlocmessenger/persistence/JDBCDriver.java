package com.adrian.osmdroidlocmessenger.persistence;

import com.adrian.osmdroidlocmessenger.messagemanagement.LocationMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class JDBCDriver {
    private final String DB_URL = "jdbc:mysql://localhost:3306/location_messenger";
    private final String USER = "root";
    private final String PASSWORD = "";
    private ReentrantLock lock = new ReentrantLock();
    private Connection mySQLConnection;

    public JDBCDriver() throws SQLException {
        this.mySQLConnection = establishConnection();
        if(mySQLConnection != null) {
            System.out.println("[JDBC Driver] Connection to database is working");
        } else {
            throw new SQLException("Database Connection failed");
        }
        createTable(mySQLConnection);
    }

    public Connection establishConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            return null;
        }
    }

    public boolean closeConnection(Connection conn) {
        try {
            conn.close();
            return true;
        } catch (SQLException | NullPointerException e) {
            return false;
        }
    }

    public boolean createTable(Connection mySQLConnection) {
        String createTable = "CREATE TABLE IF NOT EXISTS messages" +
                "(MessageID VARCHAR(36) NOT NULL, " +
                "UserID VARCHAR(36) NOT NULL, " +
                "MessageText VARCHAR(280) NOT NULL , " +
                "Latitude DOUBLE NOT NULL, " +
                "Longitude DOUBLE NOT NULL, " +
                "LikeCount INT NOT NULL, " +
                "PRIMARY KEY (MessageID)) ENGINE = InnoDB; ";
        try {
            mySQLConnection.prepareStatement(createTable).executeUpdate();
            return true;
        } catch (SQLException | NullPointerException e) {
            return false;
        } finally {
            closeConnection(mySQLConnection);
        }
    }

    public boolean insertMessage(LocationMessage message) {
        lock.lock();
        try {
            mySQLConnection = establishConnection();
            String deleteOldMessages = "DELETE FROM messages WHERE UserID=?";

            PreparedStatement deleteStatement = mySQLConnection.prepareStatement(deleteOldMessages);
            deleteStatement.setString(1, message.getUserID());

            String insertAndDeleteOldMessages = "INSERT INTO messages (MessageID, UserID, MessageText, Latitude, Longitude, LikeCount) VALUES (?,?,?,?,?,?)";

            PreparedStatement insertStatement = mySQLConnection.prepareStatement(insertAndDeleteOldMessages);
            insertStatement.setString(1, message.getMessageID());
            insertStatement.setString(2, message.getUserID());
            insertStatement.setString(3, message.getText());
            insertStatement.setDouble(4, message.getLatitude());
            insertStatement.setDouble(5, message.getLongitude());
            insertStatement.setInt(6, message.getLikeCount());

            deleteStatement.executeUpdate();
            if (insertStatement.executeUpdate() > 0) {
                System.out.println("[JDBC Driver] Message has been inserted");
                return true;
            } else {
                return false;
            }
        } catch (SQLException | NullPointerException e) {
            System.err.println("[JDBC Driver] Exception while inserting a message: ");
            e.printStackTrace();
            return false;
        } finally {
            closeConnection(mySQLConnection);
            lock.unlock();
        }
    }

    public boolean incrementLikeCount(LocationMessage message) {
        lock.lock();
        try {
            mySQLConnection = establishConnection();
            String update = "UPDATE messages SET LikeCount=? WHERE MessageID=?";

            PreparedStatement statement = mySQLConnection.prepareStatement(update);
            statement.setInt(1, message.getLikeCount() + 1);
            statement.setString(2, message.getMessageID());

            if (statement.executeUpdate() > 0) {
                System.out.println("[JDBC Driver] Message has been updated");
                return true;
            } else {
                return false;
            }
        } catch (SQLException | NullPointerException e) {
            System.err.println("[JDBC Driver] Exception while updating a message: ");
            e.printStackTrace();
            return false;
        } finally {
            closeConnection(mySQLConnection);
            lock.unlock();
        }
    }

    public LocationMessage getMessage(String messageID) {
        lock.lock();
        try {
            mySQLConnection = establishConnection();
            String update = "SELECT * FROM messages WHERE MessageID = ?";
            PreparedStatement statement = mySQLConnection.prepareStatement(update);
            statement.setString(1, messageID);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new LocationMessage(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getDouble(4), resultSet.getDouble(5), resultSet.getInt(6));
            }
            resultSet.close();
            return null;
        } catch (SQLException | NullPointerException e) {
            System.err.println("[JDBC Driver] Exception while returning a message: ");
            e.printStackTrace();
            return null;
        } finally {
            closeConnection(mySQLConnection);
            lock.unlock();
        }
    }

    public ArrayList<LocationMessage> getAllMessages() {
        lock.lock();
        try {
            mySQLConnection = establishConnection();
            ArrayList<LocationMessage> messages = new ArrayList<LocationMessage>();
            String selectAll = "SELECT * FROM messages";

            ResultSet resultSet = mySQLConnection.prepareStatement(selectAll).executeQuery();
            while (resultSet.next()) {
                messages.add(new LocationMessage(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getDouble(4), resultSet.getDouble(5), resultSet.getInt(6)));
            }
            resultSet.close();
            System.out.println("[JDBC Driver] Messages have been returned");
            return messages;
        } catch (SQLException | NullPointerException e) {
            System.err.println("[JDBC Driver] Exception while returning all messages: ");
            e.printStackTrace();
            return null;
        } finally {
            closeConnection(mySQLConnection);
            lock.unlock();
        }
    }
}
