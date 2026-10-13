import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Front desk: check in a guest.
public class CheckIn {

    public static void run() {
        try (Connection conn = Database.getConnection()) {
            while (true) {
                System.out.println("\n=== CHECK IN GUEST ===");
                if (!showAvailableRooms(conn)) {
                    System.out.println("No rooms are available right now.");
                    return;
                }

                String name = InputHelper.readString("Guest name: ");
                String contact = InputHelper.readString("Contact number: ");
                String room = InputHelper.readString("Room number: ");

                int roomId = availableRoomId(conn, room);
                if (roomId < 0) {
                    System.out.println("Error: that room is not available.");
                    if (!InputHelper.readYesNo("Try again?")) {
                        return;
                    }
                    continue;
                }

                System.out.println("Discount: 1. None   2. Student   3. Senior   4. PWD");
                int d = InputHelper.readInt("Choose discount type: ", 1, 4);
                String[] types = {"None", "Student", "Senior", "PWD"};
                String type = types[d - 1];
                String idNumber = null;
                if (!type.equals("None")) {
                    idNumber = InputHelper.readString("ID number: ");
                }

                save(conn, name, contact, roomId, type, idNumber);
                System.out.println("\nCheck-in successful! " + name + " is now in room " + room + ".");
                return;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static boolean showAvailableRooms(Connection conn) throws SQLException {
        boolean any = false;
        System.out.println("Available rooms:");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT room_number FROM rooms WHERE status = 'Available' AND active = 1 ORDER BY room_number")) {
            while (rs.next()) {
                any = true;
                System.out.println("  Room " + rs.getString(1));
            }
        }
        return any;
    }

    private static int availableRoomId(Connection conn, String room) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM rooms WHERE room_number = ? AND status = 'Available' AND active = 1")) {
            ps.setString(1, room);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static void save(Connection conn, String name, String contact, int roomId,
                             String type, String idNumber) throws SQLException {
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO guests (name, contact, room_id, discount_type, id_number, check_in) "
                  + "VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime'))")) {
                ps.setString(1, name);
                ps.setString(2, contact);
                ps.setInt(3, roomId);
                ps.setString(4, type);
                ps.setString(5, idNumber);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE rooms SET status = 'Occupied' WHERE id = ? AND status = 'Available'")) {
                ps.setInt(1, roomId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Room is no longer available.");
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
