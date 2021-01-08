package com.adrian.osmdroidlocmessenger.servermanagement;

import com.adrian.osmdroidlocmessenger.persistence.JDBCDriver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerApp {
    private static final int PORT = 7800;
    private static ExecutorService pool = Executors.newFixedThreadPool(1024);

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            JDBCDriver driver = new JDBCDriver();

            while (true) {
                try {
                    System.out.println("[SERVER] Waiting for a client");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[SERVER] Client connected");
                    ClientThread clientThread = new ClientThread(clientSocket, driver);
                    pool.execute(clientThread);
                } catch (Exception e) {
                    System.err.println("[SERVER] Client Thread Failure");
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Socket Error: ");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[SERVER] JDBCDriver Error: ");
            e.printStackTrace();
        }
    }
}
