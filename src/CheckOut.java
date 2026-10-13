import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Front desk: check out a guest (balance must be zero).
public class CheckOut {

    public static void run() {
        System.out.println("\n=== CHECK OUT GUEST ===");
        String room = InputHelper.readString("Room number: ");
        try (Connection conn = Database.getConnection()) {
            Billing.GuestInfo g = Billing.findCurrentGuest(conn, room);
            if (g == null) {
                System.out.println("No checked-in guest in that room.");
                return;
            }

            int open = openOrders(conn, g.id);
            if (open > 0) {
                System.out.println("Cannot check out: " + open
                    + " order(s) are not delivered yet. Deliver or cancel them first.");
                return;
            }

            while (true) {
                Billing.showAccount(conn, g);
                double balance = Billing.balance(conn, g.id);
                if (balance <= 0.004) {
                    break;
                }
                System.out.println("Outstanding balance: " + Fmt.money(balance));
                if (!InputHelper.readYesNo("Record a payment now?")) {
                    System.out.println("Check-out cancelled. The balance must be zero first.");
                    return;
                }
                double amount = Billing.askPaymentAmount(balance);
                if (amount == 0) {
                    System.out.println("Check-out cancelled.");
                    return;
                }
                Billing.recordPayment(conn, g.id, amount);
                System.out.println("Payment recorded.");
            }

            if (!InputHelper.readYesNo("Balance is zero. Check out " + g.name + " now?")) {
                System.out.println("Check-out cancelled.");
                return;
            }

            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE guests SET check_out = datetime('now', 'localtime') WHERE id = ?")) {
                    ps.setInt(1, g.id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE rooms SET status = 'Available' WHERE id = ?")) {
                    ps.setInt(1, g.roomId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

            Receipt.printFinal(conn, g.id);
            System.out.println("\nCheck-out successful. Room " + g.roomNumber + " is now Available.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    private static int openOrders(Connection conn, int guestId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM orders WHERE guest_id = ? AND status IN ('Pending', 'Preparing')")) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getInt(1);
            }
        }
    }
}
