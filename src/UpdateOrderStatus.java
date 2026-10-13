import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Room service: move an order from Pending to Preparing to Delivered.
public class UpdateOrderStatus {

    public static void run() {
        System.out.println("\n=== UPDATE ORDER STATUS ===");
        try (Connection conn = Database.getConnection()) {
            if (!showActive(conn)) {
                System.out.println("There are no active orders.");
                return;
            }
            int id = InputHelper.readInt("Order ID (0 to cancel): ", 0, 1000000);
            if (id == 0) {
                return;
            }
            String current = statusOf(conn, id);
            if (current == null || !(current.equals("Pending") || current.equals("Preparing"))) {
                System.out.println("Error: order not found or not active.");
                return;
            }

            System.out.println("Current status: " + current);
            System.out.println("New status: 1. Preparing   2. Delivered");
            int choice = InputHelper.readInt("Choose: ", 1, 2);
            String next = (choice == 1) ? "Preparing" : "Delivered";
            if (next.equals(current)) {
                System.out.println("Error: the order is already " + current + ".");
                return;
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
                ps.setString(1, next);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            System.out.println("Order #" + id + " is now " + next + ".");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static boolean showActive(Connection conn) throws SQLException {
        boolean any = false;
        String sql = "SELECT o.id, r.room_number, o.status, o.total, o.order_time FROM orders o "
                   + "JOIN guests g ON g.id = o.guest_id JOIN rooms r ON r.id = g.room_id "
                   + "WHERE o.status IN ('Pending', 'Preparing') ORDER BY o.id";
        System.out.println(Fmt.fit("ID", 6) + Fmt.fit("Room", 8) + Fmt.fit("Status", 12)
            + Fmt.fit("Time", 21) + String.format("%10s", "Total"));
        System.out.println(Fmt.line('-', 57));
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                any = true;
                System.out.println(Fmt.fit(String.valueOf(rs.getInt(1)), 6) + Fmt.fit(rs.getString(2), 8)
                    + Fmt.fit(rs.getString(3), 12) + Fmt.fit(rs.getString(5), 21)
                    + String.format("%10s", Fmt.money(rs.getDouble(4))));
            }
        }
        return any;
    }

    private static String statusOf(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM orders WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
