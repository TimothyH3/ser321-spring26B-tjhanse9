import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONObject;

/**
 * Hangman Game Client - Student Starter Code
 *
 * Your task: Implement the protocol communication for all game features.
 *
 * What's provided:
 * - Complete menu structure with different game states
 * - Name handling as a complete example
 * - Some method stubs as examples
 *
 * What you need to implement:
 * - Protocol requests/responses for all game operations
 * - Proper response handling and display
 */
public class HangmanClient {
    static Socket sock;
    static ObjectOutputStream oos;
    static ObjectInputStream in;

    static Scanner scanner = new Scanner(System.in);
    static boolean inGame = false;
    static boolean hasName = false;
    static String playerName = "";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            sock = new Socket(host, port);
            oos = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());

            System.out.println("========================================");
            System.out.println("       WELCOME TO HANGMAN GAME!         ");
            System.out.println("========================================");
            System.out.println();

            boolean running = true;
            while (running) {
                if (!hasName) {
                    running = showInitialMenu();
                } else if (!inGame) {
                    running = showMainMenu();
                } else {
                    running = showGameMenu();
                }
                System.out.println();
            }

            closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initial menu - before name is set
     */
    static boolean showInitialMenu() {
        System.out.println("----------------------------------------");
        System.out.println("  1. Set Your Name");
        System.out.println("  2. Quit");
        System.out.println("----------------------------------------");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                setName();
                return true;
            case "2":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Main menu - after name set, no active game
     */
    static boolean showMainMenu() {
        System.out.println("----------------------------------------");
        System.out.println("MAIN MENU:");
        System.out.println("  1. Start New Game");
        System.out.println("  2. View Leaderboard");
        System.out.println("  3. Quit");
        System.out.println("----------------------------------------");
        System.out.print("Enter choice: ");

        String choice = scanner.nextLine().trim();
        switch (choice) {
            case "1":
                startGame();
                return true;
            case "2":
                displayLeaderboard();
                return true;
            case "3":
                quit();
                return false;
            default:
                System.out.println("Invalid choice. Please try again.");
                return true;
        }
    }

    /**
     * Game menu - during active game
     * Natural input: just type letter/word to guess
     * Commands: 1, 2, 3, 4, 0 for special actions
     */
    static boolean showGameMenu() {
        // Always display game state for convienience
        displayGameState();
        System.out.println("Type a letter or word to guess");
        System.out.println("Or choose:");
        System.out.println("  1 - Show game state");
        System.out.println("  2 - See guessed letters");
        System.out.println("  3 - Get a hint (-8 points)");
        System.out.println("  4 - Give up (return to main menu)");
        System.out.println("  0 - Quit game");
        System.out.println("----------------------------------------");
        System.out.print("Your input: ");
        String input = scanner.nextLine().trim();

        // Handle commands
        switch (input) {
            case "1":
                // displayGameState();
                //placeholder since displayGameState() is already called
                return true;
            case "2":
                displayGuessedLetters();
                return true;
            case "3":
                getHint();
                return true;
            case "4":
                giveUp();
                return true;
            case "0":
                quit();
                return false;
            default:
                break;
        }

        if (input.isEmpty()) {
            System.out.println("Please enter a letter, word, or command.");
            return true;
        }

        // Single character = letter guess, multiple = word guess
        if (input.length() == 1) {
            guessLetter(input);
        } else {
            guessWord(input);
        }

        return true;
    }

    /**
     * Send request to guess a word
     */
    private static void guessWord(String input) {
        JSONObject request = new JSONObject();
        request.put("type", "word");
        request.put("word", input);
        JSONObject response = sendRequest(request);
        
        // make sure response isn't null before proceeding
        if (response == null) return;

        System.out.println(response.optString("guessMessage", ""));
        System.out.println(response.optString("message", ""));
        
        // use getBoolean instead of getString to prevent casting crash
        if (response.has("inGame") && !response.getBoolean("inGame")) {
            System.out.println("The word was: " + response.getString("secretWord"));
            System.out.println("Score: " + response.getInt("points"));
            System.out.println("Misses: " + response.getInt("misses"));
            System.out.println("Hints Used: " + response.getInt("hintsUsed"));
            System.out.println("Thanks for playing!");
            inGame = false;
        }
    }

    /**
     * Send request to guess a letter
     */
    private static void guessLetter(String input) {
        JSONObject request = new JSONObject();
        request.put("type", "letter");
        request.put("letter", input);
        JSONObject response = sendRequest(request);

        if (response == null) return;

        System.out.println(response.optString("guessMessage", ""));
        System.out.println(response.optString("message", ""));
        
        if (response.has("inGame") && !response.getBoolean("inGame")) {
            System.out.println("The word was: " + response.getString("secretWord"));
            System.out.println("Score: " + response.getInt("points"));
            System.out.println("Misses: " + response.getInt("misses"));
            System.out.println("Hints Used: " + response.getInt("hintsUsed"));
            System.out.println("Thanks for playing!");
            inGame = false;
        }
    }

    /**
     * Get a hint
     */
    private static void getHint() {
        JSONObject request = new JSONObject();
        request.put("type", "getHint");
        JSONObject response = sendRequest(request);
        
        if (response != null) {
            System.out.println(response.getString("message"));
        }
    }

    /**
     * Give up - return to main menu
     */
    static void giveUp() {
        JSONObject request = new JSONObject();
        System.out.print("\nAre you sure you want to give up? (yes/no): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes") || confirm.equals("y")) {
            request.put("type", "giveUp");
            JSONObject response = sendRequest(request);
            
            if (response == null) return;

            inGame = response.getBoolean("inGame");
            System.out.println("\nYou gave up! The word was: " + response.getString("word"));
            
            // grab points safely if server sent them
            if (response.has("points")) {
                System.out.println("Score: " + response.getInt("points"));
            }
        } else {
            System.out.println("\nContinuing game...");
        }
    }

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     * This is provided as a complete example.
     * Use this as a reference for implementing other methods.
     */
    static void setName() {
        System.out.print("\nEnter your name: ");
        String name = scanner.nextLine().trim();

        // Create request according to YOUR protocol design
        JSONObject request = new JSONObject();
        request.put("type", "name");
        request.put("name", name);

        // Send request and get response
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                hasName = true;
                playerName = name;
                System.out.println("\n" + response.getString("message"));
                System.out.println();
            } else {
                System.out.println("Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Start new game
     */
    static void startGame() {
        JSONObject request = new JSONObject();
        request.put("type", "newGame");
        JSONObject response = sendRequest(request);
        if (response != null) {
            if (response.getBoolean("ok")) {
                inGame = true;
                System.out.println("\n" + response.getString("message"));
                System.out.println();
            } else {
                System.out.println("Error: " + response.getString("message"));
            }
        }
    }

    /**
     * Print game state
     */
    static void displayGameState() {
        // request and display game state from server
        JSONObject request = new JSONObject();
        request.put("type", "gameState");
        JSONObject response = sendRequest(request);
        
        if (response == null) return;

        System.out.println("\n----------------------------------------");
        System.out.println(response.getString("message"));
        System.out.println("\n" + response.getString("progress") + "\n");
        System.out.println("Score: " + response.getInt("points") + "  |  Misses: " 
                            + response.getInt("misses") + "  |  Hints Used: " 
                            + response.getInt("hintsUsed"));
        System.out.println("\n----------------------------------------");
    }

    /**
     * Print guessed letters
     */
    static void displayGuessedLetters() {
        // request and display guessed letters from server
        JSONObject request = new JSONObject();
        request.put("type", "getGuessedLetters");
        JSONObject response = sendRequest(request);
        
        if (response == null) return;

        System.out.println("\n----------------------------------------");
        System.out.println(response.getString("message"));
        System.out.println("\n----------------------------------------");
    }


    /**
     * Quit game
     */
    static boolean quit() {
        JSONObject request = new JSONObject();
        request.put("type", "quit");

        JSONObject response = sendRequest(request);
        if (response != null && response.getBoolean("ok")) {
            System.out.println("\n" + response.getString("message"));
            System.out.println("Thanks for playing!");
        }
        return false; // Stop the main loop
    }

    /**
     * Helper: Send request and receive response
     * This handles the basic communication pattern
     */
    static JSONObject sendRequest(JSONObject request) {
        try {
            String req = request.toString();
            oos.writeObject(req);
            oos.flush();

            String res = (String) in.readObject();
            return new JSONObject(res);
        } catch (Exception e) {
            System.out.println("Error communicating with server: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close connection
     */
    static void closeConnection() {
        try {
            if (oos != null) oos.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Display leaderboard - reads from a requested List<Map<String, Object>> leaderboard
     */
    private static void displayLeaderboard() {
        JSONObject request = new JSONObject();
        request.put("type", "leaderboard");
        
        JSONObject response = sendRequest(request);
        
        // gracefully handle missing or empty board
        if (response == null || !response.has("leaderboard")) {
            System.out.println("\n----------------------------------------");
            System.out.println(response != null && response.has("message") ? response.getString("message") : "No leaderboard available");
            System.out.println("----------------------------------------");
            return;
        }
        
        System.out.println("\n----------------------------------------");
        for (int i = 0; i < response.getJSONArray("leaderboard").length(); i++) {
            JSONObject entry = response.getJSONArray("leaderboard").getJSONObject(i);
            Map<String, Object> map = entry.toMap();
            String name = (String) map.get("name");
            int score = (int) map.get("points");
            System.out.println((i + 1) + ". " + name + ": " + score);
        }
        System.out.println("\n----------------------------------------");
    }
}