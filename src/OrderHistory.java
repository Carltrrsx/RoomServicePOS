import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Shows past orders and lets you open any order's receipt.
public class OrderHistory {

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ORDER HISTORY ===");
            System.out.println("1. All orders");
            System.out.println("2. Orders of one room");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 2);
            switch (choice) {
                case 1: show(null); break;
                case 2: show(InputHelper.readString("Room number: ")); break;
                case 0: back = true; break;
            }
        }
    }

    private static void show(String room) {
        String sql = "SELECT o.id, o.order_time, r.room_number, g.name, o.status, o.total FROM orders o "
                   + "JOIN guests g ON g.id = o.guest_id JOIN rooms r ON r.id = g.room_id "
                   + (room != null ? "WHERE r.room_number = ? " : "") + "ORDER BY o.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (room != null) {
                ps.setString(1, room);
            }
            int count = 0;
            System.out.println("\n" + Fmt.fit("ID", 5) + Fmt.fit("Time", 21) + Fmt.fit("Room", 7)
                + Fmt.fit("Guest", 18) + Fmt.fit("Status", 11) + String.format("%10s", "Total"));
            System.out.println(Fmt.line('-', 72));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    System.out.println(Fmt.fit(String.valueOf(rs.getInt(1)), 5) + Fmt.fit(rs.getString(2), 21)
                        + Fmt.fit(rs.getString(3), 7) + Fmt.fit(rs.getString(4), 18)
                        + Fmt.fit(rs.getString(5), 11) + String.format("%10s", Fmt.money(rs.getDouble(6))));
                }
            }
            if (count == 0) {
                System.out.println("No orders found.");
                return;
            }
            int id = InputHelper.readInt("\nEnter an order ID to see its receipt (0 to go back): ", 0, 1000000);
            if (id != 0 && !Receipt.printOrder(conn, id)) {
                System.out.println("Order not found.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }
}
