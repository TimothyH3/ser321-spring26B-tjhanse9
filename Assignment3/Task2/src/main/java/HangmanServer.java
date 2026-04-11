import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Hangman Game Server - Student Starter Code
 *
 * Your task: Design the protocol and implement the game logic.
 *
 * What's provided:
 * - Resource loading (game stages, word list)
 * - Name handling as a complete example
 * - Basic server structure and routing
 *
 * What you need to implement:
 * - Complete protocol design (document in README.md)
 * - All game logic handlers (stubs provided below)
 */
public class HangmanServer {
    static Socket sock;
    static ObjectOutputStream os;
    static ObjectInputStream in;
    static int port = 8888;

    // Game state for current player - YOU WILL NEED THESE
    static String playerName = null;
    static String secretWord = null;
    static Set<Character> usedLetters = new HashSet<>();
    static int misses = 0;
    static int points = 0;
    static boolean inGame = false;
    static int hintsUsed = 0;

    // Leaderboard - list of game results (you can change this any way you want)
    static List<Map<String, Object>> leaderboard = new ArrayList<>();

    // Game ASCII art - 7 stages (0-6 misses allowed)
    // Loaded from resources/game_stages.txt
    static String[] GAME_STAGES = new String[7];

    // Word list - loaded from resource file
    static String[] WORDS;

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Expected arguments: <port(int)>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            System.out.println("Port must be an integer");
            System.exit(2);
        }

        // Load game resources
        loadGameStages();
        loadWords();

        try {
            ServerSocket serv = new ServerSocket(port);
            System.out.println("Hangman Server ready for connections on port " + port);

            while (true) {
                System.out.println("Server waiting for a connection");
                sock = serv.accept();
                System.out.println("Client connected");

                // Setup streams
                in = new ObjectInputStream(sock.getInputStream());
                OutputStream out = sock.getOutputStream();
                os = new ObjectOutputStream(out);

                // Initialize game state for new connection
                initGame();

                boolean connected = true;
                while (connected) {
                    String s = "";
                    try {
                        s = (String) in.readObject();
                    } catch (Exception e) {
                        System.out.println("Client disconnect");
                        connected = false;
                        continue;
                    }

                    JSONObject res = isValid(s);
                    if (res.has("ok")) {
                        sendResponse(res);
                        continue;
                    }

                    JSONObject req = new JSONObject(s);
                    res = testField(req, "type");
                    if (!res.getBoolean("ok")) {
                        res = noType(req);
                        sendResponse(res);
                        continue;
                    }

                    // Route to appropriate handler
                    String type = req.getString("type");
                    if (type.equals("name")) {
                        res = handleName(req);
                    // route client strings to correct handler
                    } else if (type.equals("gameState")) {
                        res = handleGameState(req);
                    } else if (type.equals("letter")) {
                        res = handleGuessLetter(req);
                    } else if (type.equals("word")) {
                        res = handleGuessWord(req);
                    } else if (type.equals("newGame")) {
                        res = handleNewGame(req);
                    } else if (type.equals("leaderboard")) {
                        res = handleLeaderboard(req);
                    } else if (type.equals("getHint")) {
                        res = handleGetHint(req);
                    } else if (type.equals("getGuessedLetters")) {
                        res = handleGetGuessedLetters(req);
                    } else if (type.equals("giveUp")) { 
                        res = handleGiveUp(req);
                    } else if (type.equals("quit")) {
                        res = handleQuit(req);
                        sendResponse(res);
                        connected = false;
                        continue;
                    } else {
                        res = wrongType(req);
                    }
                    sendResponse(res);
                }
                closeConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeConnection();
        }
    }

    /**
     * EXAMPLE IMPLEMENTATION: Set player name
     * This is provided as a complete example of request handling.
     * Use this as a reference for implementing other handlers.
     */
    static JSONObject handleName(JSONObject req) {
        System.out.println("Name request: " + req.toString());
        JSONObject res = testField(req, "name");
        if (!res.getBoolean("ok")) {
            return res;
        }

        String name = req.getString("name");
        if (name == null || name.trim().isEmpty()) {
            res = new JSONObject();
            res.put("ok", false);
            res.put("message", "Name cannot be empty");
            return res;
        }

        playerName = name.trim();
        res = new JSONObject();
        res.put("ok", true);
        res.put("type", "name");
        res.put("message", "Welcome " + playerName + "! Ready to play Hangman?");
        return res;
    }

    /**
     * Start New Game
     */
    static JSONObject handleNewGame(JSONObject req) {
        JSONObject res = new JSONObject();
        if (!inGame) {
            System.out.println("New game");

            // Generate a random word
            int randomIndex = (int) (Math.random() * WORDS.length);
            try {
                // actually set the secret word
                secretWord = WORDS[randomIndex];
                System.out.println("Selected word: " + secretWord);
            } catch (Exception e) {
                res.put("ok", false);
                res.put("message", "Error accessing word list");
                return res;
            }

            // Initialize game state
            usedLetters = new HashSet<>();
            misses = 0;
            points = 0;
            inGame = true;
            hintsUsed = 0;

            // Send response to client
            res.put("ok", true);
            res.put("type", "newGame");
            res.put("message", "New game started");
            res.put("misses", misses);
            res.put("points", points);
            res.put("stage", GAME_STAGES[0]);

            return res;
        } else {
            res.put("ok", false);
            res.put("message", "You are already in a game");
            return res;
        }
    }

    /**
     * Guess Letter
     */
    static JSONObject handleGuessLetter(JSONObject req) {
        // Validate request
        JSONObject testRes = testField(req, "letter");
        if (!testRes.getBoolean("ok")) {
            return testRes;
        }
        
        String letterStr = req.getString("letter");
        
        // prevent crash if string is empty
        if (letterStr.isEmpty()) {
            JSONObject res = new JSONObject();
            res.put("ok", false);
            res.put("message", "Invalid guess");
            return res;
        }
        
        char letterChar = letterStr.charAt(0);
        
        // check if letter has already been guessed
        if (usedLetters.contains(letterChar)) {
            JSONObject res = new JSONObject();
            res.put("ok", false);
            res.put("message", "Letter has already been guessed");
            return res;
        }
        
        JSONObject res = new JSONObject();
        
        // Check if word contains letter
        if (secretWord.indexOf(letterChar) >= 0) {
            usedLetters.add(letterChar);
            res.put("ok", true);
            res.put("type", "guessLetter");
            res.put("guessMessage", "Correct guess");
            res.put("letter", letterStr);

            // +5 points multiplied by how many times letter appears
            for (int i = 0; i < secretWord.length(); i++) {
                if (secretWord.charAt(i) == letterChar) {
                    points += 5;
                }
            }
            
            // check if all letters are found (size logic was flawed for words with duplicate letters)
            boolean allGuessed = true;
            for (int i = 0; i < secretWord.length(); i++) {
                if (!usedLetters.contains(secretWord.charAt(i))) {
                    allGuessed = false;
                    break;
                }
            }
            
            // if all letters have been guessed, game over
            if (allGuessed) {
                res = handleWinGame(res);
            }
            
            res.put("inGame", inGame);
            return res;
        } else {
            misses++;
            // if 6 misses have been made, game over
            if (misses >= 6) {
                res = handleLoseGame(res);
            } else {
                usedLetters.add(letterChar);
                // Send response to client
                res.put("ok", true);
                res.put("type", "guessLetter");
                res.put("guessMessage", "Incorrect guess");
                res.put("letter", letterStr);
                points--;
            }
            res.put("inGame", inGame);
            return res;
        }
    }

    /**
     * Guess Word
     */
    static JSONObject handleGuessWord(JSONObject req) {
        JSONObject testRes = testField(req, "word");
        if (!testRes.getBoolean("ok")) {
            return testRes;
        }  
        JSONObject res = new JSONObject();
        if (secretWord.equals(req.getString("word"))) {
            res.put("guessMessage", "Correct!");
            res = handleWinGame(res);
            res.put("inGame", inGame);
            return res;
        } else {
            misses += 2;
            // if 6 misses have been made, game over
            if (misses >= 6) {
                res = handleLoseGame(res);
            } else {
                res.put("ok", true);
                res.put("type", "guessWord");
                res.put("guessMessage", req.getString("word") + " is incorrect. ");
                res.put("message", "This counts as 2 misses.");
            }
            res.put("inGame", inGame);
            return res;
        }
    }

    /**
     * Game state
     */
    static JSONObject handleGameState(JSONObject req) {
        JSONObject res = new JSONObject();
        String progress = "";
        
        if (secretWord != null) {
            for (int i = 0; i < secretWord.length(); i++) {
                if (usedLetters.contains(secretWord.charAt(i))) {
                    progress += secretWord.charAt(i);
                } else {
                    progress += "_";
                }
                progress += " ";
            }
        }
        
        res.put("ok", true);
        res.put("type", "gameState");
        // bounds check just in case
        res.put("message", GAME_STAGES[Math.min(misses, 6)]);
        res.put("progress", progress);
        res.put("misses", misses);
        res.put("points", points);
        res.put("hintsUsed", hintsUsed);
        return res;
    }


    /**
     * Get Hint, costs 8 points
     */
    static JSONObject handleGetHint(JSONObject req) {
        JSONObject res = new JSONObject();
        points -= 8;
        hintsUsed ++;
        res.put("ok", true);
        res.put("type", "getHint");
        String hint = "";
        
        if (secretWord != null) {
            for (int i = 0; i < secretWord.length(); i++) {
                if (!usedLetters.contains(secretWord.charAt(i))) {
                    hint += secretWord.charAt(i);
                    break;
                }
            }
        }
        res.put("message", "Hint: " + hint);
        return res;
    }

    /**
     * Handle Win game, updates leaderboard
     */
    static JSONObject handleWinGame(JSONObject res) {
        res.put("ok", true);
        res.put("type", "winGame");
        res.put("message", "You won!");
        res.put("secretWord", secretWord);
        res.put("misses", misses);
        // client string parse looks for 'hintsUsed' not 'hintUsed'
        res.put("hintsUsed", hintsUsed);
        //20 points for winning
        points += 20;
        //10 points if no hints were used
        if (hintsUsed == 0) {
            points += 10;
        }
        res.put("points", points);
        inGame = false;
        //update leaderboard
        leaderboard.add(Map.of("name", playerName != null ? playerName : "Anonymous", "points", points));
        sortLeaderboard();
        return res;
    }

    /**
     * Handle Lose game, updates leaderboard
     */
    static JSONObject handleLoseGame(JSONObject res) {
        res.put("ok", true);
        res.put("type", "loseGame");
        res.put("message", "You lost!");
        res.put("secretWord", secretWord);
        res.put("misses", misses);
        res.put("hintsUsed", hintsUsed);
        res.put("points", points);
        inGame = false;
        leaderboard.add(Map.of("name", playerName != null ? playerName : "Anonymous", "points", points));
        sortLeaderboard();
        return res;
    }

    /**
     * Get guessed letters
     */
    static JSONObject handleGetGuessedLetters(JSONObject req) {
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "getGuessedLetters");
        res.put("message", "Guessed letters: " + usedLetters);
        return res;
    }
    
    /**
     * Give up
     */
    static JSONObject handleGiveUp(JSONObject req) {
        inGame = false;
        JSONObject res = new JSONObject();
        res.put("ok", true);
        res.put("type", "giveUp");
        res.put("message", "You gave up!");
        res.put("word", secretWord);
        res.put("inGame", inGame);
        res.put("points", points);
        return res;
    }

    /**
     * Handle Leaderboard
     */
    static JSONObject handleLeaderboard(JSONObject req) {
        JSONObject res = new JSONObject();
        if (leaderboard.size() == 0) {
            res.put("ok", true);
            res.put("type", "leaderboard");
            res.put("message", "Leaderboard is empty");
            return res;
        }
        res.put("ok", true);
        res.put("type", "leaderboard");
        res.put("leaderboard", leaderboard);
        return res;
    }


    /**
     * Quit handler
     */
    static JSONObject handleQuit(JSONObject req) {
        System.out.println("Quit request: " + req.toString());
        JSONObject res = new JSONObject();
        
        res.put("ok", true);
        res.put("type", "quit");
        res.put("message", "Goodbye " + (playerName != null ? playerName : "player") + "!");

        return res;
    }

    /**
     * Helper: Initialize game state for new connection
     */
    static void initGame() {
        playerName = null;
        secretWord = null;
        usedLetters = new HashSet<>();
        misses = 0;
        points = 0;
        inGame = false;
    }

    /**
     * Helper: Check if field exists in request
     */
    static JSONObject testField(JSONObject req, String key) {
        JSONObject res = new JSONObject();
        if (!req.has(key)) {
            res.put("ok", false);
            res.put("message", "Field '" + key + "' does not exist in request");
            return res;
        }
        return res.put("ok", true);
    }

    /**
     * Helper: Validate JSON
     */
    static JSONObject isValid(String json) {
        try {
            new JSONObject(json);
        } catch (JSONException e) {
            try {
                new JSONArray(json);
            } catch (JSONException ne) {
                JSONObject res = new JSONObject();
                res.put("ok", false);
                res.put("message", "Request is not valid JSON");
                return res;
            }
        }
        return new JSONObject();
    }

    /**
     * Error: no type field
     */
    static JSONObject noType(JSONObject req) {
        System.out.println("No type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "No request type was given");
        return res;
    }

    /**
     * Error: wrong type
     */
    static JSONObject wrongType(JSONObject req) {
        System.out.println("Wrong type request: " + req.toString());
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "Type '" + req.optString("type") + "' is not supported");
        return res;
    }

    /**
     * Load game ASCII art stages from resource file
     */
    static void loadGameStages() {
        try {
            InputStream is = HangmanServer.class.getResourceAsStream("/game_stages.txt");
            if (is == null) {
                System.err.println("Error: game_stages.txt not found in resources");
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuilder currentStage = new StringBuilder();
            int stageIndex = 0;

            while ((line = reader.readLine()) != null) {
                if (line.equals("---")) {
                    GAME_STAGES[stageIndex++] = "\n" + currentStage.toString();
                    currentStage = new StringBuilder();
                } else if (!line.startsWith("STAGE")) {
                    currentStage.append(line).append("\n");
                }
            }
            // Add final stage
            if (currentStage.length() > 0 && stageIndex < 7) {
                GAME_STAGES[stageIndex] = "\n" + currentStage.toString();
            }
            reader.close();
            System.out.println("Loaded " + (stageIndex + 1) + " game stages");
        } catch (Exception e) {
            System.err.println("Error loading game stages: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Load word list from resource file
     */
    static void loadWords() {
        try {
            WORDS = loadWordList("/words.txt");
            System.out.println("Loaded " + WORDS.length + " words");
        } catch (Exception e) {
            System.err.println("Error loading word list: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Helper: Load a single word list from file
     */
    static String[] loadWordList(String filename) throws IOException {
        InputStream is = HangmanServer.class.getResourceAsStream(filename);
        if (is == null) {
            throw new IOException("Word list file not found: " + filename);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        List<String> words = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty()) {
                words.add(line.toLowerCase());
            }
        }
        reader.close();

        return words.toArray(new String[0]);
    }

    /**
     * Write response to client
     */
    static void sendResponse(JSONObject res) {
        try {
            os.writeObject(res.toString());
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close connection
     */
    static void closeConnection() {
        try {
            if (os != null) os.close();
            if (in != null) in.close();
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sort the leaderboard ArrayList
     */
    static void sortLeaderboard() {
        Collections.sort(leaderboard, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> map1, Map<String, Object> map2) {
                Integer points1 = (Integer) map1.get("points");
                Integer points2 = (Integer) map2.get("points");
                return points2.compareTo(points1);
            }
        });
    }
}