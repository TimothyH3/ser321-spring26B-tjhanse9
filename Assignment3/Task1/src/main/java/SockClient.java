
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 */
class SockClient {
  static Socket sock = null;
  static String host = "localhost";
  static int port = 8888;
  static OutputStream out;
  // Using and Object Stream here and a Data Stream as return. Could both be the same type I just wanted
  // to show the difference. Do not change these types.
  static ObjectOutputStream os;
  static DataInputStream in;
  public static void main (String args[]) {

    if (args.length != 2) {
      System.out.println("Expected arguments: <host(String)> <port(int)>");
      System.exit(1);
    }

    try {
      host = args[0];
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      connect(host, port); // connecting to server
      System.out.println("Client connected to server.");
      boolean requesting = true;
      while (requesting) {
        System.out.println("What would you like to do: 1 - echo, 2 - add, 3 - string concatenation, 4 - CalculateMany, 5 - playlist (0 to quit)");
        Scanner scanner = new Scanner(System.in);
        int choice = Integer.parseInt(scanner.nextLine());
        // You can assume the user put in a correct input, you do not need to handle errors here
        // You can assume the user inputs a String when asked and an int when asked. So you do not have to handle user input checking
        JSONObject json = new JSONObject(); // request object
        switch(choice) {
          case 0:
            System.out.println("Choose quit. Thank you for using our services. Goodbye!");
            requesting = false;
            break;
          case 1:
            System.out.println("Choose echo, which String do you want to send?");
            String message = scanner.nextLine();
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            String num1 = scanner.nextLine();
            json.put("type", "add");
            json.put("num1", num1);

            System.out.println("Enter second number:");
            String num2 = scanner.nextLine();
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose string concatenation, enter first string:");
            String str1 = scanner.nextLine();
            System.out.println("Enter second string:");
            String str2 = scanner.nextLine();
            json.put("type", "stringconcatenation");
            json.put("string1", str1);
            json.put("string2", str2);
            break;
          case 4:
            JSONArray jsonList = new JSONArray();
            System.out.println("Enter the operation:");
            String operation = scanner.nextLine();

            boolean cont = true;
            while (cont) {
                System.out.println("Enter a number or 'd' when finished:");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("d")) {
                    cont = false;
                } else {
                    try {
                        jsonList.put(Integer.parseInt(input));
                    } catch (NumberFormatException e) {
                        System.out.println("Not a valid number. Try again.");
                    }
                }
            }

            json.put("type", "calculatemany");
            json.put("operation", operation);
            json.put("numList", jsonList);
            break;
          case 5:
            System.out.println("Playlist Manager. Enter action (add, remove, list, clear):");
            String action = scanner.nextLine().trim().toLowerCase();
            
            json.put("type", "playlist");
            json.put("action", action);

            if (action.equals("add")) {
                System.out.println("Enter song title:");
                json.put("song", scanner.nextLine());
                System.out.println("Enter artist name:");
                json.put("artist", scanner.nextLine());
            } else if (action.equals("remove")) {
                System.out.println("Enter song title to remove:");
                json.put("song", scanner.nextLine());
            }
          }
        if(!requesting) {
          continue;
        }

        // write the whole message
        os.writeObject(json.toString());
        // make sure it wrote and doesn't get cached in a buffer
        os.flush();

        // handle the response
        // - not doing anything other than printing some things, make this better
        // !! you will most likely need to parse the response for the other 2 services!
        String i = (String) in.readUTF();
        JSONObject res = new JSONObject(i);
        System.out.println("Got response: " + res);
        if (res.getBoolean("ok")) {
          if (res.getString("type").equals("echo")) {
              System.out.println(res.getString("echo"));
          } else if (res.getString("type").equals("playlist")) {
              if (res.has("message")) {
                  System.out.println(">> " + res.getString("message"));
              }
              if (res.has("songs")) {
                  JSONArray songs = res.getJSONArray("songs");
                  if (songs.length() == 0) {
                      System.out.println(">> No songs currently in playlist");
                  } else {
                      System.out.println("\n Playlist:");
                      for (int j = 0; j < songs.length(); j++) {
                          JSONObject songObj = songs.getJSONObject(j);
                          System.out.println((j + 1) + ". " + songObj.getString("song") + " by " + songObj.getString("artist"));
                      }
                  }
              }
              if (res.has("songCount")) {
                  System.out.println(">> Total songs in playlist: " + res.getInt("songCount"));
              }

          } else {
              if (res.has("result")) {
                  System.out.println(res.get("result"));
              } else if (res.has("sum")) {
                  System.out.println(res.get("sum"));
              } else if (res.has("product")) {
                  System.out.println(res.get("product"));
              } else if (res.has("average")) {
                  System.out.println(res.get("average"));
              } else {
                  System.out.println("Success, but no known result key found.");
              }
          }
        } else {
          System.out.println(res.getString("message"));
        }
      }
      // want to keep requesting services so don't close connection
      //overandout();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void overandout() throws IOException {
    //closing things, could
    in.close();
    os.close();
    sock.close(); // close socked after sending
  }

  public static void connect(String host, int port) throws IOException {
    // open the connection
    sock = new Socket(host, port); // connect to host and socket on port 8888

    // get output channel
    out = sock.getOutputStream();

    // create an object output writer (Java only)
    os = new ObjectOutputStream(out);

    in = new DataInputStream(sock.getInputStream());
  }
}