import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Room service: cancel a Pending order and remove its charge.
public class CancelOrder {

    public static void run() {
        System.out.println("\n=== CANCEL ORDER ===");
        try (Connection conn = Database.getConnection()) {
            if (!showPending(conn)) {
                System.out.println("There are no Pending orders to cancel.");
                return;
            }
            int id = InputHelper.readInt("Order ID to cancel (0 to go back): ", 0, 1000000);
            if (id == 0) {
                return;
            }

            int guestId = -1;
            double total = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT guest_id, total FROM orders WHERE id = ? AND status = 'Pending'")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        guestId = rs.getInt(1);
                        total = rs.getDouble(2);
                    }
                }
            }
            if (guestId < 0) {
                System.out.println("Error: that order was not found or can no longer be cancelled.");
                return;
            }

            // If the guest already paid more than the remaining bill, cancelling would leave a negative balance.
            double balanceAfter = Billing.balance(conn, guestId) - total;
            if (balanceAfter < -0.004) {
                System.out.println("Error: the guest has already paid this amount, so the order cannot be cancelled.");
                return;
            }

            if (!InputHelper.readYesNo("Cancel order #" + id + "?")) {
                System.out.println("Order was not cancelled.");
                return;
            }

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE orders SET status = 'Cancelled' WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM charges WHERE order_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            System.out.println("Order #" + id + " cancelled and its charge was removed.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static boolean showPending(Connection conn) throws SQLException {
        boolean any = false;
        String sql = "SELECT o.id, r.room_number, o.order_time, o.total FROM orders o "
                   + "JOIN guests g ON g.id = o.guest_id JOIN rooms r ON r.id = g.room_id "
                   + "WHERE o.status = 'Pending' ORDER BY o.id";
        System.out.println(Fmt.fit("ID", 6) + Fmt.fit("Room", 8) + Fmt.fit("Time", 21) + String.format("%10s", "Total"));
        System.out.println(Fmt.line('-', 45));
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                any = true;
                System.out.println(Fmt.fit(String.valueOf(rs.getInt(1)), 6) + Fmt.fit(rs.getString(2), 8)
                    + Fmt.fit(rs.getString(3), 21) + String.format("%10s", Fmt.money(rs.getDouble(4))));
            }
        }
        return any;
    }
}
