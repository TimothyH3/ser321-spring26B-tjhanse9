package taskone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import taskone.proto.Request;
import taskone.proto.Response;
import taskone.proto.TaskProto;

/**
 * Performer class handles client requests using JSON protocol.
 * This version uses JSON for serialization.
 */
public class Performer {
    private final Socket clientSocket;
    private final TaskList taskList;

    private InputStream inStream; // For proto
    private OutputStream outStream; // For proto

    private BufferedReader in; // For JSON
    private PrintWriter out; // For JSON

    public Performer(Socket clientSocket, TaskList taskList) {
        this.clientSocket = clientSocket;
        this.taskList = taskList;
    }

    /**
     * Main method to process client requests.
     * Reads requests, processes them, and sends responses.
     */
    public void doPerform() {
        try {
            inStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
            in = new BufferedReader(new InputStreamReader(inStream));
            out = new PrintWriter(outStream, true);

            // Use either JSON or Proto from the examples below,
            // and make matching changes in Client.java.

                /////////////////////////////////////////////////////////////////////////////
                            // Welcome JSON
                /////////////////////////////////////////////////////////////////////////////
/* // Send welcome message. You can keep this as JSON.
            JSONObject welcomeMessage = JsonUtils.createSuccessResponse("connect", "Connected to Task Management Server");
            out.println(welcomeMessage);*/
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome JSON
                /////////////////////////////////////////////////////////////////////////////



                /////////////////////////////////////////////////////////////////////////////
                            // Welcome Proto
                /////////////////////////////////////////////////////////////////////////////
            Response.Builder protoResp = Response.newBuilder().setType(Response.ResponseType.SUCCESS).setMessage("Connected to Proto Task Management Server");
            protoResp.build().writeDelimitedTo(outStream);
                /////////////////////////////////////////////////////////////////////////////
                            // End Welcome Proto
                /////////////////////////////////////////////////////////////////////////////




            // Process requests
            while (true) {
                Request request = Request.parseDelimitedFrom(inStream); // Read as Proto.
                if (request == null) break;

                // We intentionally skip error handling here to focus on Proto conversion.
                // This may fail if the request is malformed or missing expected fields, which is ok this time.

                // Once you start changing things more and more to proto, the JSON parts might not work anymore.
                // That is fine, just do not call these requests until converted.
                // Start with the "add" request.

                System.out.println(request);
                enum RequestType {
                    UNKNOWN,
                    ADD,
                    LIST,
                    FINISH,
                    QUIT
                } 
                Request.RequestType type = request.getType();
                Response responseProto; 

                System.out.println(type);
                // Change all the following requests/responses to JSON here and in Client.java
                switch (type) {
                    case ADD:
                        responseProto = handleAdd(request); // would need to be changed to return ProtoRes and get ProtReq
                        break;
                    case LIST:
                        responseProto = handleList(request);
                        break;
                    case FINISH:
                        responseProto = handleFinish(request);
                        break;
                    case QUIT:
                        responseProto = handleQuit();
                        break;
                    default:
                        responseProto = Response.newBuilder().setType(Response.ResponseType.UNKNOWN).build();
                }

                responseProto.writeDelimitedTo(outStream);

                // If quit, break the loop
                if (type == Request.RequestType.QUIT) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private Response handleAdd(Request request) { // will need to change to not use JSON anymore - or make new method
        // Validation is intentionally removed so students can focus on Proto conversion.
        // These comments show what production-style validation would look like. You can also delete all these
        // if they annoy you since it makes it harder to read it

//        // Validate required fields
//        if (!request.has("description")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'description' field");
//        }
//        if (!request.has("category")) {
//            return JsonUtils.createErrorResponse("add", "Missing 'category' field");
//        }

        String description = request.getDescription();
        String category = request.getCategory();

//        // Validate description not empty
//        if (description.trim().isEmpty()) {
//            return JsonUtils.createErrorResponse("add", "Description cannot be empty");
//        }
//
//        // Validate category value
//        if (!category.equals("work") && !category.equals("personal") && !category.equals("school") && !category.equals("other")) {
//            return JsonUtils.createErrorResponse("add", "Invalid category value. Must be 'work', 'personal', 'school', or 'other'");
//        }

        // Add task
        Task task = taskList.addTask(description, category); // Assume valid input for this starter version.

        TaskProto taskProto = TaskProto.newBuilder()
                .setId(task.getId())
                .setDescription(task.getDescription())
                .setCategory(task.getCategory())
                .setFinished(task.isFinished())
                .build();

        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setTask(taskProto)
                .build();
    }


    private Response handleList(Request request) {
        // Get filter (defaults to "all")
        String filter = request.getFilter();
        if (filter.isEmpty()) filter = "all";

        List<Task> tasks;
        switch (filter) {
            case "all":
                tasks = taskList.listAllTasks();
                break;
            case "pending":
                tasks = taskList.listPendingTasks();
                break;
            case "finished":
                tasks = taskList.listFinishedTasks();
                break;
            default:
                return Response.newBuilder()
                        .setType(Response.ResponseType.ERROR)
                        .setMessage("Invalid filter value. Must be 'all', 'pending', or 'finished'")
                        .build();
        }

        taskone.proto.TaskList.Builder listBuilder = taskone.proto.TaskList.newBuilder();
        for (Task task : tasks) {
            listBuilder.addTasks(TaskProto.newBuilder()
                    .setId(task.getId())
                    .setDescription(task.getDescription())
                    .setCategory(task.getCategory())
                    .setFinished(task.isFinished())
                    .build());
        }
        listBuilder.setCount(tasks.size());

        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setTaskList(listBuilder.build())
                .build();
    }

    private Response handleFinish(Request request) {
        // Validation intentionally skipped.

        int id = request.getId();
        // Mark task as finished
        boolean success = taskList.finishTask(id);

        if (success) {
            return Response.newBuilder()
                    .setType(Response.ResponseType.SUCCESS)
                    .setMessage("Task #" + id + " marked as finished")
                    .build();
        } else {
            return Response.newBuilder()
                    .setType(Response.ResponseType.ERROR)
                    .setMessage("Task not found with ID: " + id)
                    .build();
        }
    }

    private Response handleQuit() {
        return Response.newBuilder()
                .setType(Response.ResponseType.SUCCESS)
                .setMessage("Goodbye!")
                .build();
    }
}