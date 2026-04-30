# Assignment 5 - Distributed Systems and Consensus
Name: Timothy Hansen
asurite: tjhanse9

## Protocol Description
This algorithm is built around a Leader-Worker model using plain-text TCP sockets. The leader waits for  3 workers to connect and then begins the consensus process.


### Worker Initialization
Sent immediately upon a successful connection to identify the worker.
**(Worker -> Leader):**

Example: `Worker1`


### Assign Task
Leader broadcasts an arithmetic task to all connected workers simultaneously.
**(Leader -> Worker):**

Example: `23 + 19`


### Submit Result
Worker waits for user input answer, which is then sent back.
**(Worker -> Leader):**

Example: `42`


### Announce Consensus
The leader calculates the majority vote and broadcasts the result to all workers.
**Success Response (Leader -> Worker):**

```
Consensus: <Result> (<Votes>/<Total> workers agreed)

```

Example: `Consensus: 42 (3/3 workers agreed)`


**No-Majority Response (Leader -> Worker):**
```
No consensus reached. Vote distribution: <Distribution>

```

Example: `No consensus reached. Vote distribution: {42=1, 43=1, 44=1}`


### Quit / Shutdown
Sent by leader when the user decides to end the session.
**Request (Leader -> Worker):**

```
quit

```

## How to Compile and Run

Run the following commands from root directory:

For the Leader (listens on port 9000):
`gradle runLeader --args="9000"`

For the Workers (run each of these in a separate new terminal):
`gradle runWorker --args="Worker1 localhost 9000"`
`gradle runWorker --args="Worker2 localhost 9000"`
`gradle runWorker --args="Worker3 localhost 9000"`

## Consensus Algorithm Design

**Concurrency Design:**
The leader collects the responses using a linked blocking queue to allow workers to answer at the same time. The leader assigns each worker connection its own separate thread so that whenever any worker replies, its background thread immediately drops the answer in the shared queue, and the leader just pulls the first 3 answers it gets as soon as they are ready.

**Voting Rules:**
I decided to use a basic majority voting system. The leader tallies up the frequency of each answer in a HashMap.
• If an answer gets >= 50% of the total votes, it wins and consensus is reached.
• In the event of a tie, if there's exactly 50% consensus the algorithm picks the first answer it tallied as the winner.
• If no answer has at least a 50% majority, the leader sends "No consensus reached" and prints all the votes.

## Worker Failure Handling / Edge Cases
To avoid hanging from unresponsive workers, I added a 60-second timeout to the leader's queue. If a worker doesn't submit a response, a network issue occurs, or the worker disconnects, the leader logs a timeout for that worker and proceeds with the rest of the votes.
If a worker disconnects completely, the readLine() in the leader's thread for that worker catches the null or exception and puts a "DISCONNECTED" string into the queue, alerting the main thread to remove that vote and not wait for an answer from this worker.

## Diagnosed Issues
Something I decided to change during development was my implementation of the worker failure handling. At first I was relying solely on the socket connection, assuming that as long as the socket was open and no disconnects had occured I could just wait for all responses to be submitted. However, if the user walked away without submitting anything, the leader would be stuck indefinitely waiting in a 3rd response to be entered. To fix this I changed the leader's queue retrieval from a blocking take() to a poll(60, TimeUnit.SECONDS).

Also, my original implementation locked the session after exactly 3 workers connected for simplicity, but this prevented additional workers from joining after consensus rounds had started. I implemented a new system where a separate background thread continuously accepts connections even after the session begins. The consensus logic was updated to handle variable worker counts by calculating the current number of connected workers for each round instead of using a fixed number. This allows workers to join or leave the session dynamically while maintaining proper consensus calculations based on the actual participants.