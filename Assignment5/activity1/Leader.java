import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Leader {
    // thread-safe queue for simultaneous worker responses
    static BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    static List<WorkerHandler> workers = new ArrayList<>();

    public static void main(String[] args) {
        // check for port
        if (args.length < 1) {
            System.out.println("Invalid port, please format as: \ngradle runLeader --args=\"<port>\"");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int workerCount = 0;
        int expectedWorkers = 3;

        // start server and wait for minimum workers
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Leader starting on port " + port);
            System.out.println("Waiting for " + expectedWorkers + " workers to connect...");

            // accept worker connections
            while (workerCount < expectedWorkers) {
                Socket socket = serverSocket.accept();
                WorkerHandler worker = new WorkerHandler(socket);
                workers.add(worker);
                worker.start();
                workerCount++;
                System.out.println("Connected workers:" + workerCount + " of " + expectedWorkers);
            }

            System.out.println("All workers connected. Starting consensus rounds...");
            Scanner scanner = new Scanner(System.in);
            int round = 1;

            // consensus loop
            while (true) {
                System.out.println("\nPlease enter an arithmetic task (or 'quit'): ");
                String task = scanner.nextLine();

                // check for quit
                if (task.equalsIgnoreCase("quit")) {
                    for (WorkerHandler worker : workers) {
                        worker.sendMessage("quit");
                    }
                    break;
                }
                
                // start new round
                System.out.println("Round " + round + ": Assigning task \"" + task + "\"");
                responseQueue.clear();

                // assign task to all workers
                for (WorkerHandler worker : workers) {
                    worker.sendMessage(task);
                }

                // collect responses
                List<String> validResponses = new ArrayList<>();
                for (int i = 0; i < expectedWorkers; i++) {
                    // 60 second timeout
                    String msg = responseQueue.poll(60, TimeUnit.SECONDS);
                    if (msg != null) {
                        String[] parts = msg.split(":", 2);
                        String workerName = parts[0];
                        String answer = parts[1];
                        
                        System.out.println("Received from " + workerName + ": " + answer);
                        if (!answer.equals("DISCONNECTED")) {
                            validResponses.add(answer);
                        }
                    } else {
                        System.out.println("Timed out waiting for a worker response.");
                    }
                }

                // count votes and find majority
                HashMap<String, Integer> voteCounts = new HashMap<>();
                for (String resp : validResponses) {
                    voteCounts.put(resp, voteCounts.getOrDefault(resp, 0) + 1);
                }

                int maxVotes = 0;
                String consensusAnswer = "";
                for (String key : voteCounts.keySet()) {
                    if (voteCounts.get(key) > maxVotes) {
                        maxVotes = voteCounts.get(key);
                        consensusAnswer = key;
                    }
                }

                // calculate consensus percentage for 50% requirement
                double percentage = (double) maxVotes / expectedWorkers;
                String finalAnnouncement;
                
                if (percentage >= 0.5 && maxVotes > 0) {
                    finalAnnouncement = "Consensus: " + consensusAnswer + " (" + maxVotes + "/" + expectedWorkers + " workers agreed)";
                    System.out.println(finalAnnouncement);
                } else {
                    finalAnnouncement = "No consensus reached. Vote distribution: " + voteCounts.toString();
                    System.out.println(finalAnnouncement);
                }

                System.out.println("Announcing consensus to all workers...");
                for (WorkerHandler w : workers) {
                    w.sendMessage(finalAnnouncement);
                }
                
                round++;
            }
        
        // generic exception handling
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // handle worker connections simultaneously
    static class WorkerHandler extends Thread {
        Socket socket;
        PrintWriter out;
        BufferedReader in;
        String workerName = "";

        public WorkerHandler(Socket socket) {
            this.socket = socket;
        }
        
        public void run() {
            // listen for responses from this worker
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // First thing the worker sends is its name
                workerName = in.readLine();
                System.out.println(workerName + " connected from " + socket.getInetAddress());

                // Constantly listen for answers from this worker
                while (true) {
                    String response = in.readLine();
                    // no response means worker disconnected
                    if (response == null) {
                        responseQueue.put(workerName + ":DISCONNECTED");
                        break;
                    }
                    // put the response in queue
                    responseQueue.put(workerName + ":" + response);
                }
            // if communication is interrupted send a disconnect message
            } catch (Exception e) {
                try {
                    responseQueue.put(workerName + ":DISCONNECTED");
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
        
        // message to worker
        public void sendMessage(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }
    }
}