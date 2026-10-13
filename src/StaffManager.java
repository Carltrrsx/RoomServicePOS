import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Admin: manage staff accounts.
public class StaffManager {

    private static class User {
        int id;
        String username;
        String fullName;
        String role;
        boolean active;
    }

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== MANAGE STAFF ACCOUNTS ===");
            System.out.println("1. Add staff");
            System.out.println("2. View staff");
            System.out.println("3. Edit staff");
            System.out.println("4. Delete staff");
            System.out.println("5. Deactivate / reactivate staff");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 5);
            try (Connection conn = Database.getConnection()) {
                switch (choice) {
                    case 1: add(conn); break;
                    case 2: view(conn); break;
                    case 3: edit(conn); break;
                    case 4: delete(conn); break;
                    case 5: toggle(conn); break;
                    case 0: back = true; break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static User find(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, username, full_name, role, active FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User u = new User();
                u.id = rs.getInt("id");
                u.username = rs.getString("username");
                u.fullName = rs.getString("full_name");
                u.role = rs.getString("role");
                u.active = rs.getInt("active") == 1;
                return u;
            }
        }
    }

    private static int activeAdmins(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'Admin' AND active = 1")) {
            return rs.getInt(1);
        }
    }

    private static String chooseRole() {
        System.out.println("Role: 1. Admin   2. Front Desk   3. Room Service");
        int r = InputHelper.readInt("Choose role: ", 1, 3);
        return new String[]{"Admin", "Front Desk", "Room Service"}[r - 1];
    }

    private static String readPassword(String prompt) {
        while (true) {
            String p = InputHelper.readString(prompt);
            if (p.length() >= 4) {
                return p;
            }
            System.out.println("Password must be at least 4 characters.");
        }
    }

    private static void add(Connection conn) throws SQLException {
        String username = InputHelper.readString("Username: ");
        if (find(conn, username) != null) {
            System.out.println("Error: that username already exists.");
            return;
        }
        String password = readPassword("Password (at least 4 characters): ");
        String fullName = InputHelper.readString("Full name: ");
        String role = chooseRole();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();
        }
        System.out.println("Staff account created.");
    }

    private static void view(Connection conn) throws SQLException {
        System.out.println("\n" + Fmt.fit("Username", 14) + Fmt.fit("Full name", 22)
            + Fmt.fit("Role", 14) + "Status");
        System.out.println(Fmt.line('-', 62));
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT username, full_name, role, active FROM users ORDER BY id")) {
            while (rs.next()) {
                System.out.println(Fmt.fit(rs.getString(1), 14) + Fmt.fit(rs.getString(2), 22)
                    + Fmt.fit(rs.getString(3), 14) + (rs.getInt(4) == 1 ? "Active" : "Inactive"));
            }
        }
        InputHelper.pause();
    }

    private static void edit(Connection conn) throws SQLException {
        String username = InputHelper.readString("Username to edit: ");
        User u = find(conn, username);
        if (u == null) {
            System.out.println("Error: user not found.");
            return;
        }
        System.out.println("Current: " + u.fullName + " (" + u.role + ")");
        String fullName = InputHelper.readString("New full name: ");
        String role = chooseRole();
        if (u.role.equals("Admin") && !role.equals("Admin") && u.active && activeAdmins(conn) <= 1) {
            System.out.println("Error: there must be at least one active Admin.");
            return;
        }
        String newPassword = InputHelper.readOptional("New password (press Enter to keep the old one): ");
        if (!newPassword.isEmpty() && newPassword.length() < 4) {
            System.out.println("Error: password must be at least 4 characters.");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE users SET full_name = ?, role = ?, password = COALESCE(?, password) WHERE id = ?")) {
            ps.setString(1, fullName);
            ps.setString(2, role);
            ps.setString(3, newPassword.isEmpty() ? null : newPassword);
            ps.setInt(4, u.id);
            ps.executeUpdate();
        }
        System.out.println("Staff account updated.");
    }

    private static void delete(Connection conn) throws SQLException {
        String username = InputHelper.readString("Username to delete: ");
        User u = find(conn, username);
        if (u == null) {
            System.out.println("Error: user not found.");
            return;
        }
        if (u.id == Session.userId) {
            System.out.println("Error: you cannot delete your own account.");
            return;
        }
        if (u.role.equals("Admin") && u.active && activeAdmins(conn) <= 1) {
            System.out.println("Error: there must be at least one active Admin.");
            return;
        }
        if (!InputHelper.readYesNo("Delete " + u.username + "?")) {
            System.out.println("Delete cancelled.");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setInt(1, u.id);
            ps.executeUpdate();
        }
        System.out.println("Staff account deleted.");
    }

    private static void toggle(Connection conn) throws SQLException {
        String username = InputHelper.readString("Username: ");
        User u = find(conn, username);
        if (u == null) {
            System.out.println("Error: user not found.");
            return;
        }
        if (u.id == Session.userId) {
            System.out.println("Error: you cannot deactivate your own account.");
            return;
        }
        if (u.active && u.role.equals("Admin") && activeAdmins(conn) <= 1) {
            System.out.println("Error: there must be at least one active Admin.");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            ps.setInt(1, u.active ? 0 : 1);
            ps.setInt(2, u.id);
            ps.executeUpdate();
        }
        System.out.println("Account is now " + (u.active ? "inactive." : "active."));
    }
}
