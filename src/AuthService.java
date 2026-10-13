import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    public static void login() {
        System.out.println("\n--- LOGIN ---");
        String username = InputHelper.readString("Username: ");
        String password = InputHelper.readString("Password: ");

        String sql = "SELECT id, full_name, role FROM users "
                   + "WHERE username = ? AND password = ? AND active = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Invalid username or password.");
                    return; // back to the welcome screen
                }
                Session.login(rs.getInt("id"), username,
                              rs.getString("full_name"), rs.getString("role"));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }

        System.out.println("\nWelcome, " + Session.fullName + " (" + Session.role + ")!");
        openMenuForRole();
        Session.logout();
        System.out.println("You have been logged out.");
    }

    private static void openMenuForRole() {
        switch (Session.role) {
            case "Admin":
                AdminMenu.run();
                break;
            case "Front Desk":
                FrontDeskMenu.run();
                break;
            case "Room Service":
                RoomServiceMenu.run();
                break;
            default:
                System.out.println("Unknown role: " + Session.role);
        }
    }

    public static void register() {
        System.out.println("\n--- REGISTER ---");
        String username = InputHelper.readString("Username: ");
        if (usernameTaken(username)) {
            System.out.println("Error: that username is already taken.");
            return; // back to the welcome screen
        }

        String password;
        while (true) {
            password = InputHelper.readString("Password (at least 4 characters): ");
            if (password.length() >= 4) {
                break;
            }
            System.out.println("Password is too short.");
        }

        String fullName = InputHelper.readString("Full name: ");
        System.out.println("Role: 1. Front Desk   2. Room Service");
        int r = InputHelper.readInt("Choose role: ", 1, 2);
        String role = (r == 1) ? "Front Desk" : "Room Service";

        String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();
            System.out.println("Registration successful! You can now log in.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static boolean usernameTaken(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return true; // stop registration if we can't check
        }
    }
}
