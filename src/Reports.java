import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Admin: order and sales reports (cancelled orders are not counted as sales).
public class Reports {

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ORDERS AND REPORTS ===");
            System.out.println("1. Sales per day");
            System.out.println("2. Popular items");
            System.out.println("3. Orders by status");
            System.out.println("4. Order history and receipts");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 4);
            switch (choice) {
                case 1: salesPerDay(); break;
                case 2: popularItems(); break;
                case 3: ordersByStatus(); break;
                case 4: OrderHistory.run(); break;
                case 0: back = true; break;
            }
        }
    }

    private static void salesPerDay() {
        String sql = "SELECT date(order_time) AS day, COUNT(*) AS orders, SUM(subtotal) AS sub, "
                   + "SUM(discount_amount) AS disc, SUM(total) AS tot FROM orders "
                   + "WHERE status <> 'Cancelled' GROUP BY day ORDER BY day DESC";
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n=== SALES PER DAY ===");
            System.out.println(Fmt.fit("Date", 12) + String.format("%8s%12s%12s%12s", "Orders", "Subtotal", "Discounts", "Total"));
            System.out.println(Fmt.line('-', 56));
            double grand = 0;
            int count = 0;
            while (rs.next()) {
                count++;
                grand += rs.getDouble("tot");
                System.out.println(Fmt.fit(rs.getString("day"), 12)
                    + String.format("%8d%12s%12s%12s", rs.getInt("orders"), Fmt.money(rs.getDouble("sub")),
                        Fmt.money(rs.getDouble("disc")), Fmt.money(rs.getDouble("tot"))));
            }
            if (count == 0) {
                System.out.println("No sales yet.");
            } else {
                System.out.println(Fmt.line('-', 56));
                System.out.println(Fmt.fit("ALL DAYS TOTAL", 44) + String.format("%12s", Fmt.money(grand)));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }

    private static void popularItems() {
        String sql = "SELECT m.name, SUM(oi.quantity) AS qty, SUM(oi.quantity * oi.price) AS revenue "
                   + "FROM order_items oi JOIN orders o ON o.id = oi.order_id "
                   + "JOIN menu_items m ON m.id = oi.menu_item_id "
                   + "WHERE o.status <> 'Cancelled' GROUP BY m.id ORDER BY qty DESC, revenue DESC LIMIT 10";
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n=== TOP 10 POPULAR ITEMS ===");
            System.out.println(Fmt.fit("Item", 28) + String.format("%10s%14s", "Quantity", "Sales"));
            System.out.println(Fmt.line('-', 52));
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println(Fmt.fit(rs.getString("name"), 28)
                    + String.format("%10d%14s", rs.getInt("qty"), Fmt.money(rs.getDouble("revenue"))));
            }
            if (count == 0) {
                System.out.println("No orders yet.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }

    private static void ordersByStatus() {
        String sql = "SELECT status, COUNT(*) AS n, SUM(total) AS tot FROM orders GROUP BY status ORDER BY status";
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n=== ORDERS BY STATUS ===");
            System.out.println(Fmt.fit("Status", 14) + String.format("%8s%14s", "Orders", "Amount"));
            System.out.println(Fmt.line('-', 36));
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.println(Fmt.fit(rs.getString("status"), 14)
                    + String.format("%8d%14s", rs.getInt("n"), Fmt.money(rs.getDouble("tot"))));
            }
            if (count == 0) {
                System.out.println("No orders yet.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }
}
