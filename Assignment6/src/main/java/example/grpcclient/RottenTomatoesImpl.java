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

import io.grpc.stub.StreamObserver;
import service.AddMovieRequest;
import service.AddMovieResponse;
import service.Movie;
import service.MovieListResponse;
import service.MovieRequest;
import service.MovieSearchRequest;
import service.RatingRequest;
import service.RatingResponse;
import service.Review;
import service.ReviewListResponse;
import service.ReviewRequest;
import service.ReviewResponse;
import service.RottenTomatoesGrpc;

class RottenTomatoesImpl extends RottenTomatoesGrpc.RottenTomatoesImplBase {
    
    private static final String MOVIES_JSON_FILE = "movies.json";
    private static final String MOVIE_DATA_FILE = "movie_data.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private List<Movie> movies;
    
    public RottenTomatoesImpl() {
        super();
        initializeMovieDatabase();
    }
    
    private void initializeMovieDatabase() {
        movies = new ArrayList<>();
        
        // check for movie_data.json
        File movieDataFile = new File(MOVIE_DATA_FILE);
        if (movieDataFile.exists()) {
            loadMovieData();
        } else {
            // on first run, load from movies.json and create movie_data.json
            loadMoviesFromFile();
            saveMovieData();
        }
    }
    
    // load from movies.json
    private void loadMoviesFromFile() {
        File moviesFile = new File(MOVIES_JSON_FILE);
        if (!moviesFile.exists()) {
            System.err.println("Preloaded movies.json file not found. Starting with empty database.");
            return;
        }
        
        try (FileReader reader = new FileReader(moviesFile)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            // Parse JSON format
            JSONArray jsonArray = new JSONArray(content.toString());
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject movieJson = jsonArray.getJSONObject(i);
                Movie movie = Movie.newBuilder()
                    .setTitle(movieJson.getString("title"))
                    .setYear(movieJson.getInt("year"))
                    .setGenre(movieJson.getString("genre"))
                    .setOverallRating(0.0)
                    .setRatingCount(0)
                    .build();
                movies.add(movie);
            }
        } catch (Exception e) {
            System.err.println("Error loading movies.json file: " + e.getMessage());
        }
    }
    
    // load from movie_data.json
    private void loadMovieData() {
        try (FileReader reader = new FileReader(MOVIE_DATA_FILE)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            JSONArray jsonArray = new JSONArray(content.toString());
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject movieJson = jsonArray.getJSONObject(i);
                Movie.Builder movieBuilder = Movie.newBuilder()
                    .setTitle(movieJson.getString("title"))
                    .setYear(movieJson.getInt("year"))
                    .setGenre(movieJson.getString("genre"))
                    .setOverallRating(movieJson.optDouble("overall_rating", 0.0))
                    .setRatingCount(movieJson.optInt("rating_count", 0));
                
                // Load reviews if they exist
                if (movieJson.has("reviews")) {
                    JSONArray reviewsArray = movieJson.getJSONArray("reviews");
                    for (int j = 0; j < reviewsArray.length(); j++) {
                        JSONObject reviewJson = reviewsArray.getJSONObject(j);
                        Review review = Review.newBuilder()
                            .setUserName(reviewJson.getString("user_name"))
                            .setRating(reviewJson.getInt("rating"))
                            .setReviewText(reviewJson.getString("review_text"))
                            .setDate(reviewJson.optString("date", ""))
                            .build();
                        movieBuilder.addReviews(review);
                    }
                }
                
                movies.add(movieBuilder.build());
            }
        } catch (Exception e) {
            System.err.println("Error loading movie data: " + e.getMessage());
            // Fallback to initial movies file
            loadMoviesFromFile();
        }
    }
    
    private void saveMovieData() {
        JSONArray jsonArray = new JSONArray();
        
        for (Movie movie : movies) {
            JSONObject movieJson = new JSONObject();
            movieJson.put("title", movie.getTitle());
            movieJson.put("year", movie.getYear());
            movieJson.put("genre", movie.getGenre());
            movieJson.put("overall_rating", movie.getOverallRating());
            movieJson.put("rating_count", movie.getRatingCount());
            
            // Save reviews
            JSONArray reviewsArray = new JSONArray();
            for (Review review : movie.getReviewsList()) {
                JSONObject reviewJson = new JSONObject();
                reviewJson.put("user_name", review.getUserName());
                reviewJson.put("rating", review.getRating());
                reviewJson.put("review_text", review.getReviewText());
                reviewJson.put("date", review.getDate());
                reviewsArray.put(reviewJson);
            }
            movieJson.put("reviews", reviewsArray);
            
            jsonArray.put(movieJson);
        }
        
        try (FileWriter writer = new FileWriter(MOVIE_DATA_FILE)) {
            writer.write(jsonArray.toString());
        } catch (IOException e) {
            System.err.println("Error saving movie data: " + e.getMessage());
        }
    }
    
    @Override
    public void searchMovies(MovieSearchRequest request, StreamObserver<MovieListResponse> responseObserver) {
        System.out.println("Received from client: Search for movies with query '" + request.getQuery() + "'");
        MovieListResponse.Builder response = MovieListResponse.newBuilder();
        String query = request.getQuery().toLowerCase().trim();
        
        if (query.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else if (query.equals("*")) {
            // Special case: return all movies
            if (movies.isEmpty()) {
                response.setIsSuccess(false);
                response.setError("no movies in database yet");
            } else {
                response.setIsSuccess(true);
                response.addAllMovies(movies);
            }
        } else {
            List<Movie> searchResults = movies.stream()
                .filter(movie -> movie.getTitle().toLowerCase().contains(query) || 
                              movie.getGenre().toLowerCase().contains(query))
                .collect(Collectors.toList());
            
            if (searchResults.isEmpty()) {
                response.setIsSuccess(false);
                response.setError("no movies found matching query");
            } else {
                response.setIsSuccess(true);
                response.addAllMovies(searchResults);
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void addMovie(AddMovieRequest request, StreamObserver<AddMovieResponse> responseObserver) {
        System.out.println("Received from client: Add movie '" + request.getTitle() + "' (" + request.getYear() + ")");
        AddMovieResponse.Builder response = AddMovieResponse.newBuilder();
        String title = request.getTitle().trim();
        int year = request.getYear();
        String genre = request.getGenre().trim();
        
        // check for missing fields
        if (title.isEmpty() || genre.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else if (year < 1800 || year > LocalDate.now().getYear() + 5) {
            response.setIsSuccess(false);
            response.setError("invalid year");
        } else {
            // check if movie already exists
            boolean exists = movies.stream()
                .anyMatch(movie -> movie.getTitle().equalsIgnoreCase(title));
            
            if (exists) {
                response.setIsSuccess(false);
                response.setError("movie already exists");
            } else {
                Movie newMovie = Movie.newBuilder()
                    .setTitle(title)
                    .setYear(year)
                    .setGenre(genre)
                    .setOverallRating(0.0)
                    .setRatingCount(0)
                    .build();
                
                movies.add(newMovie);
                saveMovieData();
                
                response.setIsSuccess(true);
                response.setMessage("Movie added successfully");
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void getMovieRating(MovieRequest request, StreamObserver<RatingResponse> responseObserver) {
        System.out.println("Received from client: Get rating for movie '" + request.getTitle() + "'");
        RatingResponse.Builder response = RatingResponse.newBuilder();
        String title = request.getTitle().trim();
        
        if (title.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else {
            Movie movie = movies.stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
            
            if (movie == null) {
                response.setIsSuccess(false);
                response.setError("movie not found");
            } else {
                response.setIsSuccess(true);
                if (movie.getRatingCount() == 0) {
                    response.setMessage("No ratings yet for '" + movie.getTitle() + "'");
                } else {
                    response.setMessage("Rating for '" + movie.getTitle() + "': " + 
                        String.format("%.1f", movie.getOverallRating()) + 
                        " (" + movie.getRatingCount() + " ratings)");
                }
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void getMovieReviews(MovieRequest request, StreamObserver<ReviewListResponse> responseObserver) {
        System.out.println("Received from client: Get reviews for movie '" + request.getTitle() + "'");
        ReviewListResponse.Builder response = ReviewListResponse.newBuilder();
        String title = request.getTitle().trim();
        
        if (title.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else {
            Movie movie = movies.stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
            
            if (movie == null) {
                response.setIsSuccess(false);
                response.setError("movie not found");
            } else if (movie.getReviewsCount() == 0) {
                response.setIsSuccess(false);
                response.setError("no reviews found for this movie");
            } else {
                response.setIsSuccess(true);
                response.addAllReviews(movie.getReviewsList());
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void addRating(RatingRequest request, StreamObserver<RatingResponse> responseObserver) {
        System.out.println("Received from client: Add rating for movie '" + request.getTitle() + "' by '" + request.getUserName() + "': " + request.getRating());
        RatingResponse.Builder response = RatingResponse.newBuilder();
        String title = request.getTitle().trim();
        String userName = request.getUserName().trim();
        int rating = request.getRating();
        
        // check for missing fields
        if (title.isEmpty() || userName.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else if (rating < 1 || rating > 10) {
            response.setIsSuccess(false);
            response.setError("rating must be between 1-10");
        } else {
            Movie movie = movies.stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
            
            if (movie == null) {
                response.setIsSuccess(false);
                response.setError("movie not found");
            } else {
                // Calculate new average rating
                double totalRating = movie.getOverallRating() * movie.getRatingCount() + rating;
                int newCount = movie.getRatingCount() + 1;
                double newAverage = totalRating / newCount;
                
                // update movie
                Movie updatedMovie = movie.toBuilder()
                    .setOverallRating(newAverage)
                    .setRatingCount(newCount)
                    .build();
                
                movies.remove(movie);
                movies.add(updatedMovie);
                saveMovieData();
                
                response.setIsSuccess(true);
                response.setMessage("Rating added successfully. New average: " + 
                    String.format("%.1f", newAverage) + " (" + newCount + " ratings)");
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
    
    @Override
    public void addReview(ReviewRequest request, StreamObserver<ReviewResponse> responseObserver) {
        System.out.println("Received from client: Add review for movie '" + request.getTitle() + "' by '" + request.getUserName() + "'");
        ReviewResponse.Builder response = ReviewResponse.newBuilder();
        String title = request.getTitle().trim();
        String userName = request.getUserName().trim();
        String reviewText = request.getReviewText().trim();
        int rating = request.getRating();
        
        // check for missing fields
        if (title.isEmpty() || userName.isEmpty() || reviewText.isEmpty()) {
            response.setIsSuccess(false);
            response.setError("missing field");
        } else if (rating < 1 || rating > 10) {
            response.setIsSuccess(false);
            response.setError("rating must be between 1-10");
        } else {
            Movie movie = movies.stream()
                .filter(m -> m.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
            
            if (movie == null) {
                response.setIsSuccess(false);
                response.setError("movie not found");
            } else {
                // Create new review
                Review newReview = Review.newBuilder()
                    .setUserName(userName)
                    .setRating(rating)
                    .setReviewText(reviewText)
                    .setDate(LocalDate.now().format(DATE_FORMATTER))
                    .build();
                
                // Add review to movie
                Movie updatedMovie = movie.toBuilder()
                    .addReviews(newReview)
                    .build();
                
                // Update rating (same logic as addRating)
                double totalRating = movie.getOverallRating() * movie.getRatingCount() + rating;
                int newCount = movie.getRatingCount() + 1;
                double newAverage = totalRating / newCount;
                
                updatedMovie = updatedMovie.toBuilder()
                    .setOverallRating(newAverage)
                    .setRatingCount(newCount)
                    .build();
                
                movies.remove(movie);
                movies.add(updatedMovie);
                saveMovieData();
                
                response.setIsSuccess(true);
                response.setMessage("Review added successfully");
            }
        }
        
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
