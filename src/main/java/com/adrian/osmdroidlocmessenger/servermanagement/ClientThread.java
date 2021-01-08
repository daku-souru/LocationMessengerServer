package com.adrian.osmdroidlocmessenger.servermanagement;


import com.adrian.osmdroidlocmessenger.messagemanagement.LocationMessage;
import com.adrian.osmdroidlocmessenger.persistence.JDBCDriver;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientThread implements Runnable {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private JDBCDriver jdbcDriver;
    final String defaultResponse = "SUCCESS";

    public ClientThread(Socket socket, JDBCDriver jdbcDriver) {
        this.socket = socket;
        this.jdbcDriver = jdbcDriver;
    }

    @Override
    public void run() {
        try {
            listen(socket.getInputStream(), socket.getOutputStream(), jdbcDriver);
            socket.close();
        } catch (IOException | NullPointerException e) {
            System.err.println("[CLIENT THREAD] Socket exception: ");
            e.printStackTrace();
        }
    }

    void listen(InputStream is, OutputStream os, JDBCDriver jdbcDriver) {
        try {
            ois = new ObjectInputStream(is);
            oos = new ObjectOutputStream(os);
            String request = (String) ois.readObject();
            switch (request) {
                case "ADD_MESSAGE":
                    LocationMessage messageToBeAdded = (LocationMessage) ois.readObject();
                    if (jdbcDriver.insertMessage(messageToBeAdded)) {
                        oos.writeObject(defaultResponse);
                    } else {
                        oos.writeObject("Message couldn't be added (Database Error)");
                    }
                    break;
                case "GET_MESSAGES":
                    ArrayList<LocationMessage> messages = jdbcDriver.getAllMessages();
                    oos.writeObject(messages);
                    if (messages == null) {
                        oos.writeObject("Messages couldn't be fetched (Database Error)");
                    } else {
                        oos.writeObject(defaultResponse);
                    }
                    break;
                case "UPDATE_MESSAGE":
                    LocationMessage messageToBeUpdate = (LocationMessage) ois.readObject();
                    if (jdbcDriver.incrementLikeCount(messageToBeUpdate)) {
                        oos.writeObject(jdbcDriver.getMessage(messageToBeUpdate.getMessageID()));
                        oos.writeObject(defaultResponse);
                    } else {
                        oos.writeObject(null);
                        oos.writeObject("Message couldn't be liked (Database Error)");
                    }
                    break;
                default:
                    System.err.println("[CLIENT THREAD] Request not known: " + request);
                    break;
            }
        } catch (IOException e) {
            System.err.println("[CLIENT THREAD] Object Stream Exception: ");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("[CLIENT THREAD] LocationMessage Version not up-to-date / Unknown Object");
        } catch (NullPointerException e) {
            System.err.println("[CLIENT THREAD] Empty Request");
        } finally {
            try {
                oos.flush();
                oos.close();
                ois.close();
            } catch (NullPointerException | IOException e) {
                System.err.println("[CLIENT THREAD] Object Stream Exception: ");
                e.printStackTrace();
            }
            System.out.println("[CLIENT THREAD] Client disconnected");
        }
    }
}
