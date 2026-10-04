import java.sql.*;

public class App {
    public static void main(String[] args) {

        System.out.println("Hello World!");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:roomservice.db")) {
            System.out.println("Connected to SQLite!");
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}