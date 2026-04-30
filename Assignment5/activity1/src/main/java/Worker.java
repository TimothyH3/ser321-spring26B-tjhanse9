import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Worker {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Invalid arguments, please format as: \ngradle runWorker --args=\"<workerName> <host> <port>\"");
            return;
        }

        String workerName = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        // connect to leader
        System.out.println(workerName + " connecting to leader at " + host + ":" + port);

        try (Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected!");
            
            // send worker name to leader
            out.println(workerName);

            while (true) {
                // receive task from leader
                String task = in.readLine();
                
                if (task == null || task.equalsIgnoreCase("quit")) {
                    System.out.println("Session ended by leader.");
                    break;
                }
                
                System.out.println("\nTask received: " + task);
                System.out.print("> Enter your result: ");
                
                // result input from user
                String result = scanner.nextLine();
                out.println(result);
                System.out.println("Result submitted to leader.");

                // receive consensus announcement
                String consensusAnnouncement = in.readLine();
                System.out.println("Consensus announced: " + consensusAnnouncement);
                System.out.println("Waiting for next task...");
            }

        // failed connection
        } catch (Exception e) {
            System.out.println("Connection failed.");
            e.printStackTrace();
        }
    }
}