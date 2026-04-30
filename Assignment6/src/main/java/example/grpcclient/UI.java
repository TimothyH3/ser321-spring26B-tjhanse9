package example.grpcclient;

//terminal interface that shows available services, prompts the user for inputs, and handles errors (like empty inputs or disconnected servers) gracefully without crashing.
public class UI {
    public void mainMenu() {
        System.out.println("Available services:");
        System.out.println("1. Converter");
        System.out.println("2. Echo");
        System.out.println("3. Joke");
        System.out.println("4. Library");
        System.out.println("5. RottenTomatoes");
        System.out.println("0. Exit");
        System.out.print("Select a service: ");
    }

    public void converterMenu() {
        System.out.println("Converter Menu:");
        System.out.println("1. Length");
        System.out.println("2. Weight");
        System.out.println("3. Temperature");
        System.out.println("0. Main Menu");
        System.out.print("Select a conversion type: ");
    }

    public void lengthMenu1() {
        System.out.println("Convert from:");
        System.out.println("1. Kilometers");
        System.out.println("2. Miles");
        System.out.println("3. Feet");
        System.out.println("4. Yards");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }
    
    public void lengthMenu2() {
        System.out.println("Convert to:");
        System.out.println("1. Kilometers");
        System.out.println("2. Miles");
        System.out.println("3. Feet");
        System.out.println("4. Yards");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }

    public void weightMenu1() {
        System.out.println("Convert from:");
        System.out.println("1. Kilograms");
        System.out.println("2. Pounds");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }
    
    public void weightMenu2() {
        System.out.println("Convert to:");
        System.out.println("1. Kilograms");
        System.out.println("2. Pounds");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }
    
    public void temperatureMenu1() {
        System.out.println("Convert from:");
        System.out.println("1. Celsius");
        System.out.println("2. Fahrenheit");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }
    
    public void temperatureMenu2() {
        System.out.println("Convert to:");
        System.out.println("1. Celsius");
        System.out.println("2. Fahrenheit");
        System.out.println("0. Cancel Conversion");
        System.out.print("Selection:");
    }
    
    public void jokeMenu() {
        System.out.println("Joke Service:");
        System.out.println("1. Get a joke");
        System.out.println("2. Set a joke");
        System.out.println("0. Main Menu");
        System.out.print("Selection: ");
    }
    
    public void libraryMenu() {
        System.out.println("Library Service:");
        System.out.println("1. List all books");
        System.out.println("2. Search for books");
        System.out.println("3. Borrow a book");
        System.out.println("4. Return a book");
        System.out.println("0. Main Menu");
        System.out.print("Selection: ");
    }
    
    public void movieMenu() {
        System.out.println("Welcome to Rotten Tomatoes:");
        System.out.println("1. Search Movies and TV shows");
        System.out.println("2. View movies in database");
        System.out.println("0. Main Menu");
        System.out.print("Selection: ");
    }
    
    public void movieDetails() {
        System.out.println("\n1. Add a rating");
        System.out.println("2. View reviews");
        System.out.println("3. Leave a review");
        System.out.println("0. Go back to main menu");
        System.out.print("Selection: ");
    }
    
    public void movieNotFound() {
        System.out.println("\nMovie not found:");
        System.out.println("1. Add this movie to database");
        System.out.println("2. Search again");
        System.out.println("0. Go back to main menu");
        System.out.print("Selection: ");
    }
}
