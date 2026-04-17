package taskone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Single-threaded Task Management Server.
 * This server handles one client at a time.
 *
 * Students will convert this to:
 * - Part B: Multi-threaded server (one thread per client)
 * - Part C: Thread pool server (fixed number of threads)
 */
public class ThreadedServer {
    private static final int DEFAULT_PORT = 8888;
    private static TaskList taskList = new TaskList();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse port from command line if provided
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }

        System.out.println("Task Management Server starting on port " + port);
        System.out.println("Mode: Multi-threaded (handles multiple clients concurrently)");
        System.out.println("Waiting for clients...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                // Accept connection (blocks until client connects)
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Handle client request 
                Thread clientThread = new Thread(() -> {
                    try {
                        Performer performer = new Performer(clientSocket, taskList);
                        performer.doPerform();
                    } 
                    finally {
                        // Close connection
                        try {
                            if (clientSocket != null && !clientSocket.isClosed()) {
                                clientSocket.close();
                                System.out.println("Client disconnected");
                            }
                        } catch (IOException e) {
                            System.err.println("Error closing socket: " + e.getMessage());
                        }
                    }
                });
                
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}