package example.grpcclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import service.AddMovieRequest;
import service.AddMovieResponse;
import service.Book;
import service.BookListResponse;
import service.BookSearchRequest;
import service.BorrowRequest;
import service.BorrowResponse; // needed to use Empty
import service.ClientRequest;
import service.ConversionRequest;
import service.ConversionResponse;
import service.ConverterGrpc;
import service.EchoGrpc;
import service.FindServerReq;
import service.FindServersReq;
import service.GetServicesReq;
import service.JokeGrpc;
import service.JokeReq;
import service.JokeRes;
import service.JokeSetReq;
import service.JokeSetRes;
import service.LibraryGrpc;
import service.MovieListResponse;
import service.MovieRequest;
import service.MovieSearchRequest;
import service.RatingRequest;
import service.RatingResponse;
import service.RegistryGrpc;
import service.ReturnRequest;
import service.ReturnResponse;
import service.ReviewListResponse;
import service.ReviewRequest;
import service.ReviewResponse;
import service.RottenTomatoesGrpc;
import service.ServerListRes;
import service.ServerResponse;
import service.ServicesListRes;
import service.SingleServerRes;

/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final ConverterGrpc.ConverterBlockingStub blockingStub5;
  private final LibraryGrpc.LibraryBlockingStub blockingStub6;
  private final RottenTomatoesGrpc.RottenTomatoesBlockingStub blockingStub7;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = RottenTomatoesGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    blockingStub5 = ConverterGrpc.newBlockingStub(channel);
    blockingStub6 = LibraryGrpc.newBlockingStub(channel);
    blockingStub7 = RottenTomatoesGrpc.newBlockingStub(channel);
  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void askServerToConvert(ConversionRequest request) {
    ConversionResponse response;
    try {
      response = blockingStub5.convert(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  private void listAllBooks() {
    System.out.println("\n=== All Books in Library ===");
    Empty request = Empty.newBuilder().build();
    BookListResponse response;
    try {
      response = blockingStub6.listBooks(request);
      if (response.getIsSuccess()) {
        if (response.getBooksCount() == 0) {
          System.out.println("No books available in the library.");
        } else {
          for (Book book : response.getBooksList()) {
            System.out.println("Title: " + book.getTitle());
            System.out.println("Author: " + book.getAuthor());
            System.out.println("ISBN: " + book.getIsbn());
            System.out.println("Status: " + (book.getIsBorrowed() ? "Borrowed by " + book.getBorrowedBy() + " (Due: " + book.getReturnBy() + ")" : "Available"));
            System.out.println("---");
          }
        }
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
    }
    System.out.println();
  }
  
  private void searchBooks(BufferedReader reader) {
    try {
      System.out.println("Enter search query (title or author): ");
      String query = reader.readLine().trim();
      if (query.isEmpty()) {
        System.out.println("Search query cannot be empty.");
        return;
      }
      
      System.out.println("\n=== Search Results ===");
      BookSearchRequest request = BookSearchRequest.newBuilder().setQuery(query).build();
      BookListResponse response;
      try {
        response = blockingStub6.searchBooks(request);
        if (response.getIsSuccess()) {
          if (response.getBooksCount() == 0) {
            System.out.println("No books found matching '" + query + "'.");
          } else {
            for (Book book : response.getBooksList()) {
              System.out.println("Title: " + book.getTitle());
              System.out.println("Author: " + book.getAuthor());
              System.out.println("ISBN: " + book.getIsbn());
              System.out.println("Status: " + (book.getIsBorrowed() ? "Borrowed by " + book.getBorrowedBy() + " (Due: " + book.getReturnBy() + ")" : "Available"));
              System.out.println("---");
            }
          }
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
    System.out.println();
  }
  
  private void borrowBook(BufferedReader reader) {
    try {
      System.out.println("Enter ISBN of the book to borrow: ");
      String isbn = reader.readLine().trim();
      if (isbn.isEmpty()) {
        System.out.println("ISBN cannot be empty.");
        return;
      }
      
      System.out.println("Enter your name: ");
      String borrowerName = reader.readLine().trim();
      if (borrowerName.isEmpty()) {
        System.out.println("Borrower name cannot be empty.");
        return;
      }
      
      BorrowRequest request = BorrowRequest.newBuilder()
          .setIsbn(isbn)
          .setBorrowerName(borrowerName)
          .build();
      
      BorrowResponse response;
      try {
        response = blockingStub6.borrowBook(request);
        if (response.getIsSuccess()) {
          System.out.println("Success: " + response.getMessage());
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
    System.out.println();
  }
  
  private void returnBook(BufferedReader reader) {
    try {
      System.out.println("Enter ISBN of the book to return: ");
      String isbn = reader.readLine().trim();
      if (isbn.isEmpty()) {
        System.out.println("ISBN cannot be empty.");
        return;
      }
      
      ReturnRequest request = ReturnRequest.newBuilder().setIsbn(isbn).build();
      
      ReturnResponse response;
      try {
        response = blockingStub6.returnBook(request);
        if (response.getIsSuccess()) {
          System.out.println("Success: " + response.getMessage());
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
    System.out.println();
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with 
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering) 
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs 
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       * 
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // Just doing some hard coded calls to the service node without using the
      // registry
      // create client
      Client client = new Client(channel, regChannel);
      // create UI
      UI ui = new UI();
      boolean running = true;


      // ask the user for input how many jokes the user wants
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      while (running) {
      
        // display menu with available services
        ui.mainMenu();
        // Get user input and handle the selection
        try {
          int input = Integer.parseInt(reader.readLine());
          switch (input) {
            case 1:
              // Converter
              while (input != 0) {
                ui.converterMenu();
                try {
                  input = Integer.parseInt(reader.readLine());
                  client.handleConverter(input, ui, reader);
                } catch (NumberFormatException e) {
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                }
              }
              break;
            case 2:
              // Echo
              System.out.println("Enter a message to send: ");
              message = reader.readLine();
              client.askServerToParrot(message);
              break;
            case 3:
              // Joke
              while (input != 0) {
                ui.jokeMenu();
                try {
                  input = Integer.parseInt(reader.readLine());
                  client.handleJoke(input, ui, reader);
                } catch (NumberFormatException e) {
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                }
              }
              break;
            case 4:
              // Library
              while (input != 0) {
                ui.libraryMenu();
                try {
                  input = Integer.parseInt(reader.readLine());
                  client.handleLibrary(input, ui, reader);
                } catch (NumberFormatException e) {
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                }
              }
              break;
            case 5:
              // RottenTomatoes
              while (input != 0) {
                ui.movieMenu();
                try {
                  input = Integer.parseInt(reader.readLine());
                  client.handleRottenTomatoes(input, ui, reader);
                } catch (NumberFormatException e) {
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                }
              }
              break;
            case 0:
              running = false;
              break;
            default:
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
              break;
          }
        } catch (NumberFormatException e) {
          System.out.println("Invalid selection. Please choose a number from the list. \n\n");
        }
      }
      

      // ############### Contacting the registry just so you see how it can be done

      // if (args[5].equals("true")) { 
      //   // Comment these last Service calls while in Activity 1 Task 1, they are not needed and wil throw issues without the Registry running
      //   // get thread's services
      //   client.getServices(); // get all registered services 

      //   // get parrot
      //   client.findServer("services.Echo/parrot"); // get ONE server that provides the parrot service
        
      //   // get all setJoke
      //   client.findServers("services.Joke/setJoke"); // get ALL servers that provide the setJoke service

      //   // get getJoke
      //   client.findServer("services.Joke/getJoke"); // get ALL servers that provide the getJoke service

      //   // does not exist
      //   client.findServer("random"); // shows the output if the server does not find a given service
      //   }

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      if (args[5].equals("true")) { 
        regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }

  private void handleConverter(int inputSelection, UI ui, BufferedReader reader) {
  ConversionRequest.Builder request = ConversionRequest.newBuilder();
  int fromSelection = -1;
  int toSelection = -1;
  switch (inputSelection) {
        case 1:
          // Handle length conversion
          while (fromSelection == -1) {
            ui.lengthMenu1();
            try {
              fromSelection = Integer.parseInt(reader.readLine());
              switch (fromSelection) {
                case 1:
                  request.setFromUnit("KILOMETERS");
                  break;
                case 2:
                  request.setFromUnit("MILES");
                  break;
                case 3:
                  request.setFromUnit("FEET");
                  break;
                case 4:
                  request.setFromUnit("YARDS");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  fromSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          while (toSelection == -1) {
            ui.lengthMenu2();
            try {
              toSelection = Integer.parseInt(reader.readLine());
              switch (toSelection) {
                case 1:
                  request.setToUnit("KILOMETERS");
                  break;
                case 2:
                  request.setToUnit("MILES");
                  break;
                case 3:
                  request.setToUnit("FEET");
                  break;
                case 4:
                  request.setToUnit("YARDS");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  toSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          System.out.println("Enter the length to convert: ");
          double length = -1;
          while (length == -1) {
            try {
              length = Double.parseDouble(reader.readLine());
              request.setValue(length);
            } catch (IOException e) {
              System.out.println("Invalid input. Please enter a valid number. \n\n");
            }
          }
          break;
        case 2:
          // Handle weight conversion
          while (fromSelection == -1) {
            ui.weightMenu1();
            try {
              fromSelection = Integer.parseInt(reader.readLine());
              switch (fromSelection) {
                case 1:
                  request.setFromUnit("KILOGRAMS");
                  break;
                case 2:
                  request.setFromUnit("POUNDS");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  fromSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          while (toSelection == -1) {
            ui.weightMenu2();
            try {
              toSelection = Integer.parseInt(reader.readLine());
              switch (toSelection) {
                case 1:
                  request.setToUnit("KILOGRAMS");
                  break;
                case 2:
                  request.setToUnit("POUNDS");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  toSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          System.out.println("Enter the weight to convert: ");
          double weight = -1;
          while (weight == -1) {
            try {
              weight = Double.parseDouble(reader.readLine());
              request.setValue(weight);
            } catch (IOException e) {
              System.out.println("Invalid input. Please enter a valid number. \n\n");
            }
          }
          break;
        
        case 3:
          // Handle temprature conversion
          while(fromSelection == -1) {
            ui.temperatureMenu1();
            try {
              fromSelection = Integer.parseInt(reader.readLine());
              switch (fromSelection) {
                case 1:
                  request.setFromUnit("CELSIUS");
                  break;
                case 2:
                  request.setFromUnit("FAHRENHEIT");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  fromSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          while(toSelection == -1) {
            ui.temperatureMenu2();
            try {
              toSelection = Integer.parseInt(reader.readLine());
              switch (toSelection) {
                case 1:
                  request.setToUnit("CELSIUS");
                  break;
                case 2:
                  request.setToUnit("FAHRENHEIT");
                  break;
                case 0:
                  return;
                default:
                  System.out.println("Invalid selection. Please choose a number from the list. \n\n");
                  toSelection = -1;
                  break;
              }
            } catch (IOException e) {
              System.out.println("Invalid selection. Please choose a number from the list. \n\n");
            }
          }
          System.out.println("Enter the temperature to convert: ");
          double temperature = -1;
          while(temperature == -1) {
            try {
              temperature = Double.parseDouble(reader.readLine());
              request.setValue(temperature);
            } catch (IOException e) {
              System.out.println("Invalid input. Please enter a valid number. \n\n");
            }
          }

          break;
        
        case 0:
          // Exit converter
          break;
        
        default:
          System.out.println("Invalid selection. Please choose a number from the list. \n\n");
          break;
      }
    askServerToConvert(request.build());
  }
  
  private void handleJoke(int input, UI ui, BufferedReader reader) {
    switch  (input) {
      case 1:
        // Get a joke
        int numJokes = -1;
        while(numJokes == -1) {
          try {
            System.out.println("How many jokes would you like?");
            numJokes = Integer.parseInt(reader.readLine());
          } catch (IOException e) {
            System.out.println("Invalid input. Please enter a valid number. \n\n");
          }
        }
        askForJokes(numJokes);
        break;
      case 2:
        // Set a joke
        System.out.println("Please enter your joke: ");
        String joke = "";
        try {
          joke = reader.readLine();
        } catch (IOException e) {
          System.out.println("Invalid input. Please enter a valid string. \n\n");
        }
        if (!joke.isEmpty()) {
          setJoke(joke);
        }
        else {
          System.out.println("Joke cannot be empty.");
        }
        break;
      case 0:
        return;
      default:
        System.out.println("Invalid selection. Please choose a number from the list. \n\n");
        break;
    }
  }
  
  private void handleLibrary(int input, UI ui, BufferedReader reader) {
    switch (input) {
      case 1:
        // List all books
        listAllBooks();
        break;
      case 2:
        // Search for books
        searchBooks(reader);
        break;
      case 3:
        // Borrow a book
        borrowBook(reader);
        break;
      case 4:
        // Return a book
        returnBook(reader);
        break;
      case 0:
        return;
      default:
        System.out.println("Invalid selection. Please choose a number from the list. \n\n");
        break;
    }
  }
  
  private void handleRottenTomatoes(int input, UI ui, BufferedReader reader) {
    switch (input) {
      case 1:
        // Search Movies and TV shows
        searchTitle(ui, reader);
        break;
      case 2:
        // View movies in database
        viewAllTitles();
        break;
      case 0:
        return;
      default:
        System.out.println("Invalid selection. Please choose a number from the list. \n\n");
        break;
    }
  }
  
  private void searchTitle(UI ui, BufferedReader reader) {
    try {
      System.out.println("Search for title or genre: ");
      String query = reader.readLine().trim();
      if (query.isEmpty()) {
        System.out.println("Search query cannot be empty.");
        return;
      }
      
      System.out.println("\n=== Search Results ===");
      MovieSearchRequest request = MovieSearchRequest.newBuilder().setQuery(query).build();
      MovieListResponse response;
      try {
        response = blockingStub7.searchMovies(request);
        if (response.getIsSuccess()) {
          if (response.getMoviesCount() == 0) {
            System.out.println("No movies found matching '" + query + "'.");
            // Show movie not found menu
            int choice = -1;
            while (choice == -1) {
              ui.movieNotFound();
              try {
                choice = Integer.parseInt(reader.readLine());
                switch (choice) {
                  case 1:
                    // Add this movie to database
                    addMovieFromSearch(query, reader);
                    return;
                  case 2:
                    // Search again
                    searchTitle(ui, reader);
                    return;
                  case 0:
                    return;
                  default:
                    System.out.println("Invalid selection. Please choose a number from the list.");
                    choice = -1;
                    break;
                }
              } catch (NumberFormatException e) {
                System.out.println("Invalid selection. Please choose a number from the list.");
                choice = -1;
              }
            }
          } else {
            if (response.getMoviesCount() == 1) {
              // If exactly one movie found, show details menu directly
              service.Movie foundMovie = response.getMoviesList().get(0);
              showMovieDetailsMenu(foundMovie, ui, reader);
            } else {
              // Display found movies when multiple results
              for (service.Movie movie : response.getMoviesList()) {
                System.out.println("Title: " + movie.getTitle());
                System.out.println("Year: " + movie.getYear());
                System.out.println("Genre: " + movie.getGenre());
                System.out.println("Rating: " + String.format("%.1f", movie.getOverallRating()) + 
                    " (" + movie.getRatingCount() + " ratings)");
                System.out.println("---");
              }
            }
          }
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
      System.out.println();
    } catch (IOException e) {
      System.err.println("Input error: " + e.getMessage());
    }
  }
  
  private void showMovieDetailsMenu(service.Movie movie, UI ui, BufferedReader reader) {
    System.out.println("Title: " + movie.getTitle());
    System.out.println("Year: " + movie.getYear());
    System.out.println("Genre: " + movie.getGenre());
    System.out.println("Rating: " + String.format("%.1f", movie.getOverallRating()) + 
        " (" + movie.getRatingCount() + " ratings)");
    
    int choice = -1;
    while (choice == -1) {
      ui.movieDetails();
      try {
        choice = Integer.parseInt(reader.readLine());
        switch (choice) {
          case 1:
            // Add a rating
            addRatingForMovie(movie.getTitle(), reader);
            return;
          case 2:
            // View reviews
            viewReviewsForMovie(movie.getTitle());
            return;
          case 3:
            // Leave a review
            addReviewForMovie(movie.getTitle(), reader);
            return;
          case 0:
            return;
          default:
            System.out.println("Invalid selection. Please choose a number from the list.");
            choice = -1;
            break;
        }
      } catch (NumberFormatException e) {
        System.out.println("Invalid selection. Please choose a number from the list.");
        choice = -1;
      } catch (IOException e) {
        System.out.println("Error reading input.");
        return;
      }
    }
  }
  
  private void addMovieFromSearch(String searchQuery, BufferedReader reader) {
    try {
      System.out.println("Adding movie for: " + searchQuery);
      System.out.println("Enter movie title: ");
      String title = reader.readLine().trim();
      if (title.isEmpty()) {
        title = searchQuery; // Use search query as title if empty
      }
      
      System.out.println("Enter movie year: ");
      int year = -1;
      while (year == -1) {
        try {
          year = Integer.parseInt(reader.readLine());
          if (year < 1800 || year > LocalDate.now().getYear() + 5) {
            System.out.println("Invalid year. Please enter a year between 1800 and " + (LocalDate.now().getYear() + 5));
            year = -1;
          }
        } catch (NumberFormatException e) {
          System.out.println("Invalid input. Please enter a valid year.");
        }
      }
      
      System.out.println("Enter movie genre: ");
      String genre = reader.readLine().trim();
      if (genre.isEmpty()) {
        System.out.println("Genre cannot be empty.");
        return;
      }
      
      AddMovieRequest request = AddMovieRequest.newBuilder()
          .setTitle(title)
          .setYear(year)
          .setGenre(genre)
          .build();
      
      AddMovieResponse response;
      try {
        response = blockingStub7.addMovie(request);
        if (response.getIsSuccess()) {
          System.out.println("Success: " + response.getMessage());
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
  }
  
  private void addRatingForMovie(String movieTitle, BufferedReader reader) {
    try {
      System.out.println("Enter your name: ");
      String userName = reader.readLine().trim();
      if (userName.isEmpty()) {
        System.out.println("Name cannot be empty.");
        return;
      }
      
      int rating = -1;
      while (rating < 1 || rating > 10) {
        try {
          System.out.println("Enter rating (1-10): ");
          rating = Integer.parseInt(reader.readLine());
          if (rating < 1 || rating > 10) {
            System.out.println("Rating must be between 1-10.");
          }
        } catch (NumberFormatException e) {
          System.out.println("Invalid input. Please enter a valid number.");
        }
      }
      
      RatingRequest request = RatingRequest.newBuilder()
          .setTitle(movieTitle)
          .setUserName(userName)
          .setRating(rating)
          .build();
      
      RatingResponse response;
      try {
        response = blockingStub7.addRating(request);
        if (response.getIsSuccess()) {
          System.out.println("Success: " + response.getMessage());
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
  }
  
  private void viewReviewsForMovie(String movieTitle) {
    MovieRequest request = MovieRequest.newBuilder().setTitle(movieTitle).build();
    ReviewListResponse response;
    try {
      response = blockingStub7.getMovieReviews(request);
      if (response.getIsSuccess()) {
        if (response.getReviewsCount() == 0) {
          System.out.println("No reviews found for this movie.");
        } else {
          System.out.println("\n=== Reviews for '" + movieTitle + "' ===");
          for (service.Review review : response.getReviewsList()) {
            System.out.println("User: " + review.getUserName());
            System.out.println("Rating: " + review.getRating() + "/10");
            System.out.println("Date: " + review.getDate());
            System.out.println("Review: " + review.getReviewText());
            System.out.println("---");
          }
        }
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
    }
  }
  
  private void addReviewForMovie(String movieTitle, BufferedReader reader) {
    try {
      System.out.println("Enter your name: ");
      String userName = reader.readLine().trim();
      if (userName.isEmpty()) {
        System.out.println("Name cannot be empty.");
        return;
      }
      
      int rating = -1;
      while (rating < 1 || rating > 10) {
        try {
          System.out.println("Enter rating (1-10): ");
          rating = Integer.parseInt(reader.readLine());
          if (rating < 1 || rating > 10) {
            System.out.println("Rating must be between 1-10.");
          }
        } catch (NumberFormatException e) {
          System.out.println("Invalid input. Please enter a valid number.");
        }
      }
      
      System.out.println("Enter your review: ");
      String reviewText = reader.readLine().trim();
      if (reviewText.isEmpty()) {
        System.out.println("Review cannot be empty.");
        return;
      }
      
      ReviewRequest request = ReviewRequest.newBuilder()
          .setTitle(movieTitle)
          .setUserName(userName)
          .setRating(rating)
          .setReviewText(reviewText)
          .build();
      
      ReviewResponse response;
      try {
        response = blockingStub7.addReview(request);
        if (response.getIsSuccess()) {
          System.out.println("Success: " + response.getMessage());
        } else {
          System.out.println("Error: " + response.getError());
        }
      } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error reading input.");
    }
  }
  
  private void viewAllTitles() {
    System.out.println("\n=== All Movies in Database ===");
    // Use special wildcard query to get all movies
    MovieSearchRequest request = MovieSearchRequest.newBuilder().setQuery("*").build();
    MovieListResponse response;
    try {
      response = blockingStub7.searchMovies(request);
      if (response.getIsSuccess()) {
        if (response.getMoviesCount() == 0) {
          System.out.println("No movies in database yet.");
        } else {
          for (service.Movie movie : response.getMoviesList()) {
            System.out.println("Title: " + movie.getTitle());
            System.out.println("Year: " + movie.getYear());
            System.out.println("Genre: " + movie.getGenre());
            System.out.println("Rating: " + String.format("%.1f", movie.getOverallRating()) + 
                " (" + movie.getRatingCount() + " ratings)");
            System.out.println("---");
          }
        }
      } else {
        System.out.println("Error: " + response.getError());
      }
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
    }
    System.out.println();
  }
}
