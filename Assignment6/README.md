# GRPC Services and Registry

The following folder contains a Registry.jar which includes a Registering service where Nodes can register to allow clients to find them and use their implemented GRPC services. 

Some more detailed explanations will follow and please also check the build.gradle file

## Run things locally without registry
To run see also video. To run locally and without Registry which you should do for the beginning

First Terminal

    gradle runNode

Second Terminal

    gradle runClient

## Run things locally with registry

First terminal

    gradle runRegistryServer

Second terminal

    gradle runNode -PregOn=true 

Third Terminal

    gradle runClient -PregOn=true

### gradle runRegistryServer
Will run the Registry node on localhost (arguments are possible see gradle). This node will run and allows nodes to register themselves. 

The Server allows Protobuf, JSON and gRPC. We will only be using gRPC

### gradle runNode
Will run a node with services. The starter code includes Echo and Joke services as examples. You will need to implement and add the Converter and Library services.

For the Library service: A books.txt file is provided with initial book data (format: title|author|isbn, one per line). Your server should load this on first run and create library_data.json for persistence.

The node registers itself on the Registry. You can change the host and port the node runs on and this will register accordingly with the Registry

### gradle runClient
Will run a client which will call the services from the node, it talks to the node directly not through the registry. At the end the client does some calls to the Registry to pull the services, this will be needed later.

### gradle runDiscovery
Will create a couple of threads with each running a node with services in JSON and Protobuf. This is just an example and not needed for assignment 6. 

### gradle testProtobufRegistration
Registers the protobuf nodes from runDiscovery and do some calls. 

### gradle testJSONRegistration
Registers the json nodes from runDiscovery and do some calls. 

### gradle test
Runs the test cases. The starter code includes example tests for Joke and Echo in ServerTest.java. You need to add your own tests for Converter and Library services in the same file.

IMPORTANT: Tests expect the server to be running first!
First run in one terminal:
    gradle runNode
Then in second terminal:
    gradle test

The tests connect to localhost:8000 by default.

To run in IDE:
- go about it like in the ProtoBuf assignment to get rid of errors
- all mains expect input, so if you want to run them in your IDE you need to provide the inputs for them, see build.gradle

---

### Project Description
This project implements a distributed gRPC services system with a service registry pattern. The system consists of multiple microservices that communicate via gRPC protocols, including:

- **Echo Service**: Simple parrot service for testing connectivity
- **Joke Service**: Provides and stores jokes
- **Converter Service**: Unit conversion system supporting length, weight, and temperature conversions
- **Library Service**: Book management system with borrowing, returning, and search capabilities
- **RottenTomatoes Service**: Movie database with ratings and reviews
- **Registry Service**: Service discovery and registration hub

The system demonstrates modern distributed systems architecture with service discovery, load balancing capabilities, and robust error handling. Each service is implemented following gRPC best practices with proper protobuf message definitions.

### How to Run

**Without Registry**

# Terminal 1 - Start the services node
gradle runNode

# Terminal 2 - Start the client
gradle runClient

**With Registry**

# Terminal 1 - Start the registry server
gradle runRegistryServer

# Terminal 2 - Start the services node with registry enabled
gradle runNode -PregOn=true

# Terminal 3 - Start the client with registry enabled
gradle runClient -PregOn=true

**Running Tests:**

# Terminal 1 - Start the server first
gradle runNode

# Terminal 2 - Run tests (requires server to be running)
gradle test

**Note**: The system uses default ports (Registry: 9003, Node: 9099). These can be modified in the build.gradle file if needed.

---

### Usage Guide

Starting the client, you'll see a main menu with available services:

=== Main Menu ===
1. Converter Service
2. Echo Service
3. Joke Service
4. Library Service
5. RottenTomatoes Service
0. Exit

**Converter Service:**
- **Length Conversion**: Convert between kilometers, miles, feet, and yards
- **Weight Conversion**: Convert between kilograms and pounds
- **Temperature Conversion**: Convert between Celsius and Fahrenheit
- *Required Input*: Select conversion type, choose source and target units, enter numeric value

**Echo Service:**
- Simple message echo for testing
- *Required Input*: Any text message

**Joke Service:**
- Get jokes: Request a specific number of jokes
- Set jokes: Add new jokes to the database
- *Required Input*: Number for joke count, text for new jokes

**Library Service:**
- List all books: Display entire library inventory
- Search books: Find books by title or author
- Borrow books: Borrow books by ISBN with borrower name
- Return books: Return borrowed books by ISBN
- *Required Input:*
  - To Search: Text query (title/author)
  - To Borrow: ISBN + borrower name
  - To Return: ISBN

**RottenTomatoes Service:**
- Search movies: Find movies by title or genre
- View all titles: Display complete movie database
- Add ratings: Rate movies (1-10 scale)
- View/add reviews: Read and write movie reviews
- *Required Input*: Search query, movie selection, rating numbers, review text

---

### Request Format Examples

This project uses gRPC with Protocol Buffers for client-server communication. Each service accepts simple one-line requests:

**Converter Service Request:**
```
convert(value=5.0, from_unit="kilometers", to_unit="miles")
```

**Library Service Requests:**
```
searchBooks(query="Tolkien")
borrowBook(isbn="978-0321765723", borrowerName="John")
returnBook(isbn="978-0321765723")
```

**RottenTomatoes Service Requests:**
```
searchMovies(query="Matrix")
addRating(title="The Matrix", rating=9)
addReview(title="The Matrix", review="Amazing movie!", reviewer="Joe")
```

**Echo Service Request:**
```
echo(message="Hello World")
```

**Joke Service Requests:**
```
getJokes(number=3)
setJoke(joke="Why don't scientists trust atoms? Because they make up everything!")
```

**Response Format:**
```
{isSuccess: true/false, data/error: "result or error message"}
```

---

### Fulfilled Requirements List

**Core Functionality Requirements:**
- [x] **gRPC Service Implementation**
- [x] **Protobuf Definitions**
- [x] **Service Registry**
- [x] **Unit Conversion**
- [x] **Library Management**

**Technical Requirements:**
- [x] **Error Handling**
- [x] **Input Validation**
- [x] **Data Persistence**
- [x] **Multi-service Architecture**
- [x] **Client Interface**
- [x] **Test Coverage**

**Conversion Service Specific:**
- [x] **Length Conversions**
- [x] **Weight Conversions**
- [x] **Temperature Conversions**
- [x] **Unit Validation**
- [x] **Error Messages**

**Library Service Specific:**
- [x] **Book Storage**
- [x] **Search Functionality**
- [x] **Borrowing System**
- [x] **Return System**
- [x] **Data Format**

**Custom Service Requirements (RottenTomatoes):**
- [x] **Protocol Design**
- [x] **Multiple Request Types**
- [x] **Required Inputs**
- [x] **Varying Response Data**
- [x] **Repeated Fields**
- [x] **Persistent Data**
- [x] **Client Integration**
- [x] **Server Implementation**