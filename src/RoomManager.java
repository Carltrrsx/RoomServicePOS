import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Admin: add, view, edit and delete rooms.
public class RoomManager {

    private static class Room {
        int id;
        String number;
        String status;
        boolean active;
    }

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== MANAGE ROOMS ===");
            System.out.println("1. Add room");
            System.out.println("2. View rooms");
            System.out.println("3. Edit room");
            System.out.println("4. Delete room");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 4);
            try (Connection conn = Database.getConnection()) {
                switch (choice) {
                    case 1: add(conn); break;
                    case 2: view(conn); break;
                    case 3: edit(conn); break;
                    case 4: delete(conn); break;
                    case 0: back = true; break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static Room find(Connection conn, String number) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, room_number, status, active FROM rooms WHERE room_number = ?")) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Room r = new Room();
                r.id = rs.getInt("id");
                r.number = rs.getString("room_number");
                r.status = rs.getString("status");
                r.active = rs.getInt("active") == 1;
                return r;
            }
        }
    }

    private static void add(Connection conn) throws SQLException {
        String number = InputHelper.readString("New room number: ");
        Room existing = find(conn, number);
        if (existing != null) {
            if (existing.active) {
                System.out.println("Error: room " + number + " already exists.");
                return;
            }
            if (InputHelper.readYesNo("Room " + number + " was removed before. Bring it back?")) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE rooms SET active = 1, status = 'Available' WHERE id = ?")) {
                    ps.setInt(1, existing.id);
                    ps.executeUpdate();
                }
                System.out.println("Room " + number + " is active again.");
            }
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO rooms (room_number) VALUES (?)")) {
            ps.setString(1, number);
            ps.executeUpdate();
        }
        System.out.println("Room " + number + " added (Available).");
    }

    private static void view(Connection conn) throws SQLException {
        System.out.println("\n" + Fmt.fit("Room", 10) + "Status");
        System.out.println(Fmt.line('-', 28));
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT room_number, status FROM rooms WHERE active = 1 ORDER BY room_number")) {
            while (rs.next()) {
                System.out.println(Fmt.fit(rs.getString(1), 10) + rs.getString(2));
            }
        }
        InputHelper.pause();
    }

    private static void edit(Connection conn) throws SQLException {
        String number = InputHelper.readString("Room number to edit: ");
        Room r = find(conn, number);
        if (r == null || !r.active) {
            System.out.println("Error: room not found.");
            return;
        }
        if (r.status.equals("Occupied")) {
            System.out.println("Error: an occupied room cannot be edited.");
            return;
        }
        String newNumber = InputHelper.readString("New room number: ");
        if (!newNumber.equals(r.number) && find(conn, newNumber) != null) {
            System.out.println("Error: room " + newNumber + " already exists.");
            return;
        }
        System.out.println("Status: 1. Available   2. Maintenance");
        int s = InputHelper.readInt("Choose status: ", 1, 2);
        String status = (s == 1) ? "Available" : "Maintenance";
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE rooms SET room_number = ?, status = ? WHERE id = ?")) {
            ps.setString(1, newNumber);
            ps.setString(2, status);
            ps.setInt(3, r.id);
            ps.executeUpdate();
        }
        System.out.println("Room updated.");
    }

    private static void delete(Connection conn) throws SQLException {
        String number = InputHelper.readString("Room number to delete: ");
        Room r = find(conn, number);
        if (r == null || !r.active) {
            System.out.println("Error: room not found.");
            return;
        }
        if (r.status.equals("Occupied")) {
            System.out.println("Error: an occupied room cannot be deleted.");
            return;
        }
        if (!InputHelper.readYesNo("Delete room " + number + "?")) {
            System.out.println("Delete cancelled.");
            return;
        }
        int used;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM guests WHERE room_id = ?")) {
            ps.setInt(1, r.id);
            try (ResultSet rs = ps.executeQuery()) {
                used = rs.getInt(1);
            }
        }
        if (used > 0) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE rooms SET active = 0 WHERE id = ?")) {
                ps.setInt(1, r.id);
                ps.executeUpdate();
            }
            System.out.println("Room has guest history, so it was marked inactive instead of deleted.");
        } else {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
                ps.setInt(1, r.id);
                ps.executeUpdate();
            }
            System.out.println("Room deleted.");
        }
    }
}
