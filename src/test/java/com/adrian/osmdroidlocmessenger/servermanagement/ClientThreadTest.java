package com.adrian.osmdroidlocmessenger.servermanagement;

import com.adrian.osmdroidlocmessenger.messagemanagement.LocationMessage;
import com.adrian.osmdroidlocmessenger.persistence.JDBCDriver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class ClientThreadTest {

    OutputStream spyOutput;
    InputStream spyInput;
    ArrayList<LocationMessage> storedMessages;

    JDBCDriver mockDriver;
    ClientThread client;

    int numberOfMessages = 0;
    LocationMessage message;

    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream testErr = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void init() {
        System.setOut(new PrintStream(testOut));
        System.setErr(new PrintStream(testErr));

        message = new LocationMessage("test_message", "user_id", "text", 1, 1, 1);
        mockDriver = mock(JDBCDriver.class);


        spyInput = spy(InputStream.class);
        spyOutput = spy(OutputStream.class);


        storedMessages = new ArrayList<LocationMessage>();
        while (numberOfMessages < 100) {
            storedMessages.add(new LocationMessage("test", "text", 52.520645, 13.409779));
            numberOfMessages++;
        }
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        numberOfMessages = 0;
        storedMessages.clear();
    }

    @Test
    public void addMessageTest() {
        try {
            //"Client" setup
            ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(clientOutput);
            oos.writeObject("ADD_MESSAGE");
            oos.writeObject(message);
            oos.close();

            //"Server" setup (ByteArrayInputStream connected to "Client" output)
            ByteArrayOutputStream serverOutput = new ByteArrayOutputStream();
            Socket serverSocket = mock(Socket.class);
            when(serverSocket.getOutputStream()).thenReturn(serverOutput);
            when(serverSocket.getInputStream()).thenReturn(new ByteArrayInputStream(clientOutput.toByteArray()));
            when(mockDriver.insertMessage(any())).thenReturn(true);

            client = new ClientThread(serverSocket, mockDriver);
            //Message exchange simulation (Server reads Client REQUEST and LocationMessage and responds with a message)
            client.listen(serverSocket.getInputStream(), serverSocket.getOutputStream(), mockDriver);

            //"Client" reading "Server" output
            ByteArrayInputStream clientInput = new ByteArrayInputStream(serverOutput.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(clientInput);
            String serverMessage = (String) ois.readObject();

            //"Client" received the serverMessage
            assertEquals(serverMessage, client.defaultResponse);

            assertEquals(testOut.toString(), "[CLIENT THREAD] Client disconnected\r\n");
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }

    @Test
    public void getMessagesTest() {
        try {
            //"Client" setup
            ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(clientOutput);
            oos.writeObject("GET_MESSAGES");
            oos.close();

            //"Server" setup (ByteArrayInputStream connected to "Client" output)
            ByteArrayOutputStream serverOutput = new ByteArrayOutputStream();
            Socket serverSocket = mock(Socket.class);
            when(serverSocket.getOutputStream()).thenReturn(serverOutput);
            when(serverSocket.getInputStream()).thenReturn(new ByteArrayInputStream(clientOutput.toByteArray()));
            when(mockDriver.getAllMessages()).thenReturn(storedMessages);

            client = new ClientThread(serverSocket, mockDriver);
            //Message exchange simulation (Server reads Client REQUEST, responds with ArrayList and a message)
            client.listen(serverSocket.getInputStream(), serverSocket.getOutputStream(), mockDriver);

            //"Client" reading "Server" output
            ByteArrayInputStream clientInput = new ByteArrayInputStream(serverOutput.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(clientInput);
            ArrayList<LocationMessage> receivedArrayList = (ArrayList<LocationMessage>) ois.readObject();
            String serverMessage = (String) ois.readObject();

            //"Client" received the Arraylist and serverMessage
            assertEquals(receivedArrayList.size(), storedMessages.size());
            assertEquals(serverMessage, client.defaultResponse);

            assertEquals(testOut.toString(), "[CLIENT THREAD] Client disconnected\r\n");
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }

    @Test
    public void updateMessageTest() {
        try {
            //"Client" setup
            ByteArrayOutputStream clientOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(clientOutput);
            oos.writeObject("UPDATE_MESSAGE");
            oos.writeObject(message);
            oos.close();

            //"Server" setup (ByteArrayInputStream connected to "Client" output)
            ByteArrayOutputStream serverOutput = new ByteArrayOutputStream();
            Socket serverSocket = mock(Socket.class);
            when(serverSocket.getOutputStream()).thenReturn(serverOutput);
            when(serverSocket.getInputStream()).thenReturn(new ByteArrayInputStream(clientOutput.toByteArray()));
            when(mockDriver.incrementLikeCount(any())).thenReturn(true);
            when(mockDriver.getMessage(message.getMessageID())).thenReturn(new LocationMessage(message.getMessageID(), message.getUserID(), message.getText(), message.getLatitude(), message.getLongitude(), message.getLikeCount() + 1));

            client = new ClientThread(serverSocket, mockDriver);
            //Message exchange simulation (Server reads Client REQUEST and Message, responds with updated LocationMessage and a message)
            client.listen(serverSocket.getInputStream(), serverSocket.getOutputStream(), mockDriver);

            //"Client" reading "Server" output
            ByteArrayInputStream clientInput = new ByteArrayInputStream(serverOutput.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(clientInput);
            LocationMessage receivedUpdatedMessage = (LocationMessage) ois.readObject();
            String serverMessage = (String) ois.readObject();

            //"Client" received the Arraylist and serverMessage
            assertEquals(receivedUpdatedMessage.getLikeCount(), message.getLikeCount() + 1);
            assertEquals(serverMessage, client.defaultResponse);

            assertEquals(testOut.toString(), "[CLIENT THREAD] Client disconnected\r\n");
        } catch (IOException | ClassNotFoundException e) {
            fail();
        }
    }

    @Test
    public void inCaseOfExceptionTest() throws IOException {
        client = new ClientThread(null, null);
        try {
            client.listen(null, null, null);
        } catch (Exception e) {
            fail();
        }

        //handled all possible exceptions, printed error messages and stack traces
        assertTrue(testErr.toString().length() > 500);
        //didn't crash
        assertEquals(testOut.toString(), "[CLIENT THREAD] Client disconnected\r\n");
    }
}