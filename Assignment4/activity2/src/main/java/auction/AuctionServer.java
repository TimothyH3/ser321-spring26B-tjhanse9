package auction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import buffers.AuctionItem;
import buffers.AuctionResult;
import buffers.GameResult;
import buffers.Leaderboard;
import buffers.PlayerBid;
import buffers.PlayerStatus;
import buffers.Request;
import buffers.Response;

/**
 * Auction Game Server - Players compete against bot opponents.
 * Each player plays independently against 3 bots.
 */
public class AuctionServer {
    private static final int DEFAULT_PORT = 8889;
    private static final String SCORES_FILE = "scores.txt";

    private static final int initialGold = 150;

    // Shared leaderboard
    private static LeaderboardManager leaderboard;

    // Track connected player names (to prevent duplicates)
    private static final Set<String> activePlayerNames = new HashSet<>();

    // Grading mode flag
    private static boolean gradingMode = false;

    // Bot opponent name pool
    private static final String[] BOT_NAMES = {
            "Alaric", "Brynn", "Cedric", "Daphne",
            "Elara", "Finn", "Gwen", "Hugo",
            "Isolde", "Jasper"
    };
    private static final Random botNameRandom = new Random();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--grading")) {
                gradingMode = true;
                System.out.println("Running in grading mode (deterministic results)");
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i]);
                }
            }
        }

        // Initialize leaderboard
        leaderboard = new LeaderboardManager(SCORES_FILE);
        System.out.println("Leaderboard loaded with " + leaderboard.size() + " scores");

        Executor pool = Executors.newFixedThreadPool(20);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Auction Server started on port " + port);
            System.out.println("Waiting for connections...");

            int clientId = 0;
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientId++;
                    
                    System.out.println("Client " + clientId + " connected from " +
                            clientSocket.getInetAddress().getHostAddress());

                    pool.execute(new ClientWorker(clientSocket, clientId));

                } catch (IOException e) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientWorker implements Runnable {
        private final Socket clientSocket;
        private final int clientId;

        public ClientWorker(Socket socket, int id) {
            this.clientSocket = socket;
            this.clientId = id;
        }

        @Override
        public void run() {
            String playerName = null;
            PlayerGameState gameState = null;

            try {
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();

                System.out.println("[Client " + clientId + "] Handler started");

                // Send initial welcome
                sendWelcome(out, "Welcome to the Auction Game! Please set your name.");

                // Read and process requests
                Request request;
                while ((request = Request.parseDelimitedFrom(in)) != null) {
                    Request.RequestType type = request.getType();
                    System.out.println("[Client " + clientId + "] Received: " + type);

                    Response response = null;

                    switch (type) {
                        case REGISTER:
                            String[] result = handleRegister(request);
                            playerName = result[0];
                            String message = result[1];
                            if (playerName != null) {
                                response = buildWelcome("Welcome, " + playerName + "! You have " + initialGold + " gold. " +
                                        "Type 'join' to start playing against bot opponents!");
                            } else {
                                response = buildError(message);
                            }
                            break;

                        case JOIN:
                            if (playerName == null) {
                                response = buildError("You must register a name first.");
                                break;
                            }

                            gameState = new PlayerGameState(playerName, gradingMode);
                            Item firstItem = gameState.getCurrentItem();
                            AuctionItem protoItem = itemToProto(firstItem);

                            PlayerStatus playerStatus = PlayerStatus.newBuilder()
                                    .setPlayerName(playerName)
                                    .setGoldRemaining(gameState.getGold())
                                    .setItemsValue(0)
                                    .setTotalScore(gameState.getGold())
                                    .build();

                            response = Response.newBuilder()
                                    .setType(Response.ResponseType.GAME_JOINED)
                                    .setOk(true)
                                    .setMessage("Game started! Here is the first item.")
                                    .setNextItem(protoItem)
                                    .setPlayerStatus(playerStatus)
                                    .build();
                            break;

                        case BID:
                            if (gameState == null) {
                                response = buildError("You must join a game first.");
                                break;
                            }

                            int itemId = request.getItemId();
                            int bidAmount = request.getBidAmount();

                            String validationError = gameState.validateBid(itemId, bidAmount);
                            if (validationError != null) {
                                response = buildError(validationError);
                                break;
                            }

                            Item currentItem = gameState.getCurrentItem();
                            int reservePrice = currentItem.getMinValue() / 2;

                            int actualPlayerBid = bidAmount == -1 ? 0 : bidAmount;

                            int bot1Bid = gameState.getBot1().decideBid(currentItem, reservePrice);
                            int bot2Bid = gameState.getBot2().decideBid(currentItem, reservePrice);
                            int bot3Bid = gameState.getBot3().decideBid(currentItem, reservePrice);

                            class Bidder implements Comparable<Bidder> {
                                String name; int bid;
                                Bidder(String n, int b) { name = n; bid = b; }
                                public int compareTo(Bidder o) {
                                    if (this.bid != o.bid) return Integer.compare(o.bid, this.bid);
                                    return this.name.compareTo(o.name);
                                }
                            }

                            List<Bidder> bidders = new ArrayList<>();
                            bidders.add(new Bidder(playerName, actualPlayerBid));
                            bidders.add(new Bidder(gameState.getBot1().getName(), bot1Bid));
                            bidders.add(new Bidder(gameState.getBot2().getName(), bot2Bid));
                            bidders.add(new Bidder(gameState.getBot3().getName(), bot3Bid));

                            Collections.sort(bidders);
                            Bidder winner = bidders.get(0);

                            if (winner.name.equals(playerName)) {
                                gameState.awardItemToPlayer(currentItem, winner.bid);
                            } else if (winner.name.equals(gameState.getBot1().getName())) {
                                gameState.getBot1().awardItem(currentItem, winner.bid);
                            } else if (winner.name.equals(gameState.getBot2().getName())) {
                                gameState.getBot2().awardItem(currentItem, winner.bid);
                            } else if (winner.name.equals(gameState.getBot3().getName())) {
                                gameState.getBot3().awardItem(currentItem, winner.bid);
                            }

                            AuctionResult.Builder arBuilder = AuctionResult.newBuilder()
                                    .setItem(itemToProto(currentItem))
                                    .setActualValue(currentItem.getActualValue())
                                    .setWinnerName(winner.name)
                                    .setWinningBid(winner.bid);

                            for (Bidder b : bidders) {
                                arBuilder.addAllBids(PlayerBid.newBuilder()
                                        .setPlayerName(b.name)
                                        .setBidAmount(b.bid)
                                        .build());
                            }

                            boolean hasNext = gameState.moveToNextItem();

                            PlayerStatus updatedStatus = PlayerStatus.newBuilder()
                                    .setPlayerName(playerName)
                                    .setGoldRemaining(gameState.getGold())
                                    .setItemsValue(gameState.getInventoryValue())
                                    .setTotalScore(gameState.getPlayerScore())
                                    .addAllItemsWon(gameState.getItemNames())
                                    .build();

                            Response.Builder bidRespBuilder = Response.newBuilder()
                                    .setType(Response.ResponseType.BID_RESULT)
                                    .setOk(true)
                                    .setMessage("Auction for " + currentItem.getName() + " finished!")
                                    .setResult(arBuilder.build())
                                    .setPlayerStatus(updatedStatus);

                            if (hasNext) {
                                bidRespBuilder.setNextItem(itemToProto(gameState.getCurrentItem()));
                            }

                            bidRespBuilder.build().writeDelimitedTo(out);

                            if (!hasNext) {
                                List<PlayerStatus> scores = new ArrayList<>();
                                scores.add(updatedStatus);
                                scores.add(botToStatus(gameState.getBot1()));
                                scores.add(botToStatus(gameState.getBot2()));
                                scores.add(botToStatus(gameState.getBot3()));

                                scores.sort((s1, s2) -> {
                                    if (s1.getTotalScore() != s2.getTotalScore()) {
                                        return Integer.compare(s2.getTotalScore(), s1.getTotalScore());
                                    }
                                    return s1.getPlayerName().compareTo(s2.getPlayerName());
                                });

                                String gameWinner = scores.get(0).getPlayerName();

                                int rank = leaderboard.addScore(playerName, gameState.getPlayerScore());

                                GameResult gr = GameResult.newBuilder()
                                        .addAllPlayerScores(scores)
                                        .setWinnerName(gameWinner)
                                        .setLeaderboardPosition(rank)
                                        .build();

                                Response gameOverResp = Response.newBuilder()
                                        .setType(Response.ResponseType.GAME_OVER)
                                        .setOk(true)
                                        .setMessage("Game Over!")
                                        .setGameResult(gr)
                                        .build();

                                gameOverResp.writeDelimitedTo(out);
                                gameState = null;
                            }
                            break;

                        case LEADERBOARD:
                            Leaderboard lb = Leaderboard.newBuilder()
                                    .addAllEntries(leaderboard.getTopScores(10))
                                    .build();
                            response = Response.newBuilder()
                                    .setType(Response.ResponseType.LEADERBOARD_RESPONSE)
                                    .setOk(true)
                                    .setMessage("Top 10 Global Leaderboard")
                                    .setLeaderboard(lb)
                                    .build();
                            break;

                        case QUIT:
                            response = handleQuit(gameState);
                            if (response != null) {
                                response.writeDelimitedTo(out);
                            }
                            return;

                        default:
                            response = buildError("Unknown request type");
                    }

                    if (response != null) {
                        response.writeDelimitedTo(out);
                    }
                }

                System.out.println("[Client " + clientId + "] Disconnected");

            } catch (IOException e) {
                System.err.println("[Client " + clientId + "] Error: " + e.getMessage());
            } finally {
                if (playerName != null) {
                    synchronized (activePlayerNames) {
                        activePlayerNames.remove(playerName);
                    }
                    System.out.println("[Client " + clientId + "] Removed player: " + playerName);
                }
                try {
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Helper to create a PlayerStatus from a BotOpponent
     */
    private static PlayerStatus botToStatus(BotOpponent bot) {
        return PlayerStatus.newBuilder()
                .setPlayerName(bot.getName())
                .setGoldRemaining(bot.getGold())
                .setItemsValue(bot.getInventoryValue())
                .setTotalScore(bot.getTotalScore())
                .addAllItemsWon(bot.getItemNames())
                .build();
    }

    /**
     * Handle REGISTER request - set player name.
     * Returns [playerName, errorMessage] - playerName is null if error.
     */
    private static String[] handleRegister(Request request) {
        String name = request.getName().trim();

        if (name.isEmpty()) {
            return new String[]{null, "Name cannot be empty"};
        }

        synchronized (activePlayerNames) {
            if (activePlayerNames.contains(name)) {
                return new String[]{null, "Name already taken. Please choose another."};
            }
            // Add new name
            activePlayerNames.add(name);
        }
        
        return new String[]{name, null};
    }

    /**
     * Handle QUIT request.
     */
    private static Response handleQuit(PlayerGameState gameState) {
        String message = "Thanks for playing!";
        if (gameState != null) {
            message += " Final score: " + gameState.getPlayerScore() + ".";
        }
        message += " Goodbye!";

        return Response.newBuilder()
                .setType(Response.ResponseType.FAREWELL)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: send welcome response.
     */
    private static void sendWelcome(OutputStream out, String message) throws IOException {
        buildWelcome(message).writeDelimitedTo(out);
    }

    /**
     * Helper: build welcome response.
     */
    private static Response buildWelcome(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.WELCOME)
                .setOk(true)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: build error response.
     */
    private static Response buildError(String message) {
        return Response.newBuilder()
                .setType(Response.ResponseType.ERROR)
                .setOk(false)
                .setMessage(message)
                .build();
    }

    /**
     * Helper: convert Item to protobuf AuctionItem.
     */
    private static AuctionItem itemToProto(Item item) {
        return AuctionItem.newBuilder()
                .setId(item.getId())
                .setName(item.getName())
                .setCategory(item.getCategory())
                .setMinValue(item.getMinValue())
                .setMaxValue(item.getMaxValue())
                .setReservePrice(item.getMinValue() / 2)
                .build();
    }

    /**
     * Helper: get random bot name.
     */
    private static String getRandomBotName() {
        return BOT_NAMES[botNameRandom.nextInt(BOT_NAMES.length)];
    }

    /**
     * Inner class to track player game state.
     */
    private static class PlayerGameState {
        private String playerName;
        private int gold;
        private List<Item> inventory;
        private List<Item> items;
        private int currentItemIndex;
        private BotOpponent bot1;
        private BotOpponent bot2;
        private BotOpponent bot3;

        public PlayerGameState(String playerName, boolean gradingMode) {
            this.playerName = playerName;
            this.gold = initialGold;
            this.inventory = new ArrayList<>();

            this.items = ItemLoader.loadItems(gradingMode);
            this.currentItemIndex = 0;

            Set<String> usedNames = new HashSet<>();
            this.bot1 = createUniqueBot(usedNames, gradingMode);
            this.bot2 = createUniqueBot(usedNames, gradingMode);
            this.bot3 = createUniqueBot(usedNames, gradingMode);
        }

        private BotOpponent createUniqueBot(Set<String> usedNames, boolean gradingMode) {
            String name;
            do {
                name = getRandomBotName();
            } while (usedNames.contains(name));
            usedNames.add(name);
            return new BotOpponent(name, gradingMode);
        }

        public String validateBid(int itemId, int bidAmount) {
            Item currentItem = getCurrentItem();

            if (currentItem.getId() != itemId) {
                return "Invalid item ID. Current item is #" + currentItem.getId();
            }

            if (bidAmount == -1) {
                return null;
            }

            if (bidAmount < 0) {
                return "Bid cannot be negative (use -1 to skip)";
            }

            if (bidAmount > gold) {
                return "Insufficient gold. You have " + gold + " gold.";
            }

            int reservePrice = currentItem.getMinValue() / 2;
            if (bidAmount > 0 && bidAmount < reservePrice) {
                return "Bid must meet reserve price of " + reservePrice + " gold.";
            }

            return null;
        }

        public void awardItemToPlayer(Item item, int bidAmount) {
            inventory.add(item);
            gold -= bidAmount;
        }

        public boolean moveToNextItem() {
            currentItemIndex++;
            return currentItemIndex < items.size();
        }

        public Item getCurrentItem() {
            return items.get(currentItemIndex);
        }

        public int getInventoryValue() {
            int total = 0;
            for (Item item : inventory) {
                total += item.getActualValue();
            }
            return total;
        }

        public int getPlayerScore() {
            return gold + getInventoryValue();
        }

        public List<String> getItemNames() {
            List<String> names = new ArrayList<>();
            for (Item item : inventory) {
                names.add(item.getName());
            }
            return names;
        }

        public String getPlayerName() { return playerName; }
        public int getGold() { return gold; }
        public List<Item> getInventory() { return new ArrayList<>(inventory); }
        public BotOpponent getBot1() { return bot1; }
        public BotOpponent getBot2() { return bot2; }
        public BotOpponent getBot3() { return bot3; }
    }
}