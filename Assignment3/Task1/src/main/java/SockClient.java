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
        System.out.println("What would you like to do:\n1 - echo\n2 - add\n3 - string concatenation\n4 - CalculateMany\n5 - playlist\n6 - analyzer\n0 - quit");
        Scanner scanner = new Scanner(System.in);
        String choiceInput = scanner.nextLine();
        System.out.println(choiceInput);
        int choice = Integer.parseInt(choiceInput);
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
            System.out.println(message);
            json.put("type", "echo");
            json.put("data", message);
            break;
          case 2:
            System.out.println("Choose add, enter first number:");
            String num1 = scanner.nextLine();
            System.out.println(num1);
            json.put("type", "add");
            json.put("num1", num1);

            System.out.println("Enter second number:");
            String num2 = scanner.nextLine();
            System.out.println(num2);
            json.put("num2", num2);
            break;
          case 3:
            System.out.println("Choose string concatenation, enter first string:");
            String str1 = scanner.nextLine();
            System.out.println(str1);
            System.out.println("Enter second string:");
            String str2 = scanner.nextLine();
            System.out.println(str2);
            json.put("type", "stringconcatenation");
            json.put("string1", str1);
            json.put("string2", str2);
            break;
          case 4:
            JSONArray jsonList = new JSONArray();
            System.out.println("Enter the operation (add, multiply, average):");
            String operation = scanner.nextLine();
            System.out.println(operation);

            boolean cont = true;
              while (cont) {
                  System.out.println("Enter next number or 'd' when finished:");
                  String input = scanner.nextLine();
                  System.out.println(input);
                  
                  if (input.equalsIgnoreCase("d")) {
                    if (jsonList.length() >= 2){
                      cont = false;
                    } else {System.out.print("Numlist must contain at least two numbers.");}

                  } else {
                      try {
                          jsonList.put(Integer.parseInt(input));
                      } catch (NumberFormatException e) {
                          System.out.println("Not a valid number. Try again.");
                      }
                  }
                  System.out.print("NumList: {" );
                  for (int i = 0; i < jsonList.length(); i++) {
                    System.out.print(jsonList.getInt(i) + " " );
                  }
                  System.out.println("}");
                }

            json.put("type", "calculatemany");
            json.put("operation", operation);
            json.put("numList", jsonList);
            break;
          case 5:
            System.out.println("Playlist Manager. Enter action:\n1 - add\n2 - remove\n3 - list\n4 - clear");
            String actionInput = scanner.nextLine().trim();
            System.out.println(actionInput);
            String action = actionInput.toLowerCase();
            
            if (action.equals("1")) action = "add";
            else if (action.equals("2")) action = "remove";
            else if (action.equals("3")) action = "list";
            else if (action.equals("4")) action = "clear";
            
            json.put("type", "playlist");
            json.put("action", action);

            if (action.equals("add")) {
                System.out.println("Enter song title:");
                String song = scanner.nextLine();
                System.out.println(song);
                json.put("song", song);
                
                System.out.println("Enter artist name:");
                String artist = scanner.nextLine();
                System.out.println(artist);
                json.put("artist", artist);
            } else if (action.equals("remove")) {
                System.out.println("Enter song title to remove:");
                String song = scanner.nextLine();
                System.out.println(song);
                json.put("song", song);
            }
            break;
          case 6:
            System.out.println("Analyzer. What action do you want to perform?\n1 - wordcount\n2 - charcount\n3 - search");
            String analyzerInput = scanner.nextLine().trim();
            System.out.println(analyzerInput);
            String analyzerAction = analyzerInput.toLowerCase();
            
            if (analyzerAction.equals("1")) analyzerAction = "wordcount";
            else if (analyzerAction.equals("2")) analyzerAction = "charcount";
            else if (analyzerAction.equals("3")) analyzerAction = "search";
            
            System.out.println("Enter text:");
            String text = scanner.nextLine();
            System.out.println(text);
            json.put("type", "analyzer");
            json.put("action", analyzerAction);
            json.put("text", text);
            if (analyzerAction.equals("search")) {
                System.out.println("Enter the word to search for:");
                String findTerm = scanner.nextLine();
                System.out.println(findTerm);
                json.put("find", findTerm);
            }
            break;  
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
            } else if (res.getString("type").equals("analyzer")) {
              System.out.println("Action: " + res.getString("action"));
              
              if (res.has("count")) {
                  System.out.println("Count: " + res.getInt("count"));
              }
              
              if (res.has("positions")) {
                  JSONArray positions = res.getJSONArray("positions");
                  System.out.print("Found at indices: ");
                  for (int j = 0; j < positions.length(); j++) {
                      System.out.print(positions.getInt(j) + (j < positions.length() - 1 ? ", " : ""));
                  }
                  System.out.println();
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