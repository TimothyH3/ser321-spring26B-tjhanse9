package example.grpcclient;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;
import service.Book;
import service.BookListResponse;
import service.BookSearchRequest;
import service.BorrowRequest;
import service.BorrowResponse;
import service.LibraryGrpc;
import service.ReturnRequest;
import service.ReturnResponse;

class LibraryImpl extends LibraryGrpc.LibraryImplBase {
    
    private static final String BOOKS_JSON_FILE = "books.json";
    private static final String LIBRARY_DATA_FILE = "library_data.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private List<Book> books;
    
    public LibraryImpl() {
        super();
        initializeLibrary();
    }
    
    private void initializeLibrary() {
        books = new ArrayList<>();
        
        // check for library_data.json
        File libraryDataFile = new File(LIBRARY_DATA_FILE);
        if (libraryDataFile.exists()) {
            loadLibraryData();
        } else {
            // on first run, load from books.json and create library_data.json
            loadBooksFromFile();
            saveLibraryData();
        }
    }
    
    // load from books.json
    private void loadBooksFromFile() {
        File booksFile = new File(BOOKS_JSON_FILE);
        if (!booksFile.exists()) {
            System.err.println("Preloaded books.json file not found. Starting with empty library.");
            return;
        }
        
        try (FileReader reader = new FileReader(booksFile)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            JSONArray jsonArray = new JSONArray(content.toString());
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject bookJson = jsonArray.getJSONObject(i);
                Book book = Book.newBuilder()
                    .setTitle(bookJson.getString("title"))
                    .setAuthor(bookJson.getString("author"))
                    .setIsbn(bookJson.getString("isbn"))
                    .setIsBorrowed(false)
                    .setBorrowedBy("")
                    .setReturnBy("")
                    .build();
                books.add(book);
            }
        } catch (Exception e) {
            System.err.println("Error loading books.json file: " + e.getMessage());
        }
    }
    
    // load from library_data.json
    private void loadLibraryData() {
        try (FileReader reader = new FileReader(LIBRARY_DATA_FILE)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            JSONArray jsonArray = new JSONArray(content.toString());
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject bookJson = jsonArray.getJSONObject(i);
                Book book = Book.newBuilder()
                    .setTitle(bookJson.getString("title"))
                    .setAuthor(bookJson.getString("author"))
                    .setIsbn(bookJson.getString("isbn"))
                    .setIsBorrowed(bookJson.optBoolean("is_borrowed", false))
                    .setBorrowedBy(bookJson.optString("borrowed_by", ""))
                    .setReturnBy(bookJson.optString("return_by", ""))
                    .build();
                books.add(book);
            }
        } catch (Exception e) {
            System.err.println("Error loading library data: " + e.getMessage());
            // Fallback to initial books file
            loadBooksFromFile();
        }
    }
    
    private void saveLibraryData() {
        JSONArray jsonArray = new JSONArray();
        
        for (Book book : books) {
            JSONObject bookJson = new JSONObject();
            bookJson.put("title", book.getTitle());
            bookJson.put("author", book.getAuthor());
            bookJson.put("isbn", book.getIsbn());
            bookJson.put("is_borrowed", book.getIsBorrowed());
            bookJson.put("borrowed_by", book.getBorrowedBy());
            bookJson.put("return_by", book.getReturnBy());
            jsonArray.put(bookJson);
        }
        
        try (FileWriter writer = new FileWriter(LIBRARY_DATA_FILE)) {
            writer.write(jsonArray.toString()); 
        } catch (IOException e) {
            System.err.println("Error saving library data: " + e.getMessage());
        }
    }
    
    @Override
    public void listBooks(Empty request, StreamObserver<BookListResponse> responseObserver) {
        System.out.println("Received from client: List all books");
        BookListResponse.Builder response = BookListResponse.newBuilder();
        
        if (books.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("no books in library yet");
        } else {
            response.setIsSuccess(true);
            response.addAllBooks(books);
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void searchBooks(BookSearchRequest request, StreamObserver<BookListResponse> responseObserver) {
        System.out.println("Received from client: Search for books with query '" + request.getQuery() + "'");
        BookListResponse.Builder response = BookListResponse.newBuilder();
        String query = request.getQuery().toLowerCase().trim();
        
        if (query.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else {
            List<Book> searchResults = books.stream()
                .filter(book -> book.getTitle().toLowerCase().contains(query) || 
                        book.getAuthor().toLowerCase().contains(query))
                .collect(Collectors.toList());
            
            if (searchResults.isEmpty()) {
                response.setIsSuccess(false);
                response.setError("no books found matching query");
            } else {
                response.setIsSuccess(true);
                response.addAllBooks(searchResults);
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void borrowBook(BorrowRequest request, StreamObserver<BorrowResponse> responseObserver) {
        System.out.println("Received from client: Borrow book - ISBN '" + request.getIsbn() + "' by '" + request.getBorrowerName() + "'");
        BorrowResponse.Builder response = BorrowResponse.newBuilder();
        String isbn = request.getIsbn().trim();
        String borrowerName = request.getBorrowerName().trim();
        
        // check for missing fields
        if (isbn.isEmpty() || borrowerName.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else if (borrowerName.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("borrower name is required");
        } else {
            // search for book ISBN
            Book bookToBorrow = books.stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst()
                .orElse(null);
            
            // handle missing book
            if (bookToBorrow == null) {
                response.setIsSuccess(false);
                response.setError("book not found");
            // handle already borrowed book
            } else if (bookToBorrow.getIsBorrowed()) {
                response.setIsSuccess(false);
                response.setError("book is already borrowed");
            } else {
                // calculate due date
                LocalDate dueDate = LocalDate.now().plusDays(14);
                String returnBy = dueDate.format(DATE_FORMATTER);
                
                Book updatedBook = bookToBorrow.toBuilder()
                    .setIsBorrowed(true)
                    .setBorrowedBy(borrowerName)
                    .setReturnBy(returnBy)
                    .build();
                
                // update in list
                books.remove(bookToBorrow);
                books.add(updatedBook);
                saveLibraryData();
                
                response.setIsSuccess(true);
                response.setMessage("Book borrowed successfully. Due date: " + returnBy);
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void returnBook(ReturnRequest request, StreamObserver<ReturnResponse> responseObserver) {
        System.out.println("Received from client: Return book with ISBN '" + request.getIsbn() + "'");
        ReturnResponse.Builder response = ReturnResponse.newBuilder();
        String isbn = request.getIsbn().trim();
        
        if (isbn.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else {
            // search for book ISBN
            Book bookToReturn = books.stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst()
                .orElse(null);
            
            if (bookToReturn == null) {
                response.setIsSuccess(false);
                response.setError("book not found");
            } else if (!bookToReturn.getIsBorrowed()) {
                response.setIsSuccess(false);
                response.setError("book is not borrowed");
            } else {
                Book updatedBook = bookToReturn.toBuilder()
                    .setIsBorrowed(false)
                    .setBorrowedBy("")
                    .setReturnBy("")
                    .build();

                // update in list
                books.remove(bookToReturn);
                books.add(updatedBook);
                saveLibraryData();
                
                response.setIsSuccess(true);
                response.setMessage("Book returned successfully");
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
