import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Prints receipts to the console.
public class Receipt {

    private static final int W = 50;

    private static void header(String title) {
        System.out.println("\n" + Fmt.line('=', W));
        System.out.println(center("SOGO HOTEL"));
        System.out.println(center(title));
        System.out.println(Fmt.line('=', W));
    }

    private static String center(String s) {
        int pad = Math.max(0, (W - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    private static void itemHeader() {
        System.out.println(Fmt.fit("Item", 22) + " " + String.format("%3s %9s %9s", "Qty", "Price", "Total"));
        System.out.println(Fmt.line('-', W));
    }

    private static void printItems(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT m.name, oi.quantity, oi.price FROM order_items oi "
                   + "JOIN menu_items m ON m.id = oi.menu_item_id WHERE oi.order_id = ? ORDER BY oi.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("quantity");
                    double price = rs.getDouble("price");
                    System.out.println(Fmt.fit(rs.getString("name"), 22) + " "
                        + String.format("%3d %9s %9s", qty, Fmt.money(price), Fmt.money(qty * price)));
                }
            }
        }
    }

    private static void money(String label, double v) {
        System.out.println(Fmt.fit(label, 36) + String.format("%14s", Fmt.money(v)));
    }

    // Receipt for one order. Returns false if the order does not exist.
    public static boolean printOrder(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT o.order_time, o.status, o.subtotal, o.discount_amount, o.total, "
                   + "g.name, g.discount_type, r.room_number "
                   + "FROM orders o JOIN guests g ON g.id = o.guest_id "
                   + "JOIN rooms r ON r.id = g.room_id WHERE o.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                header("ROOM SERVICE ORDER RECEIPT");
                System.out.println("Order #  : " + orderId);
                System.out.println("Date     : " + rs.getString("order_time"));
                System.out.println("Status   : " + rs.getString("status"));
                System.out.println("Room     : " + rs.getString("room_number"));
                System.out.println("Guest    : " + rs.getString("name"));
                if (Session.fullName != null) {
                    System.out.println("Served by: " + Session.fullName);
                }
                System.out.println(Fmt.line('-', W));
                itemHeader();
                printItems(conn, orderId);
                System.out.println(Fmt.line('-', W));

                String type = rs.getString("discount_type");
                double rate = Discount.rate(type);
                money("Subtotal", rs.getDouble("subtotal"));
                money("Discount (" + type + (rate > 0 ? " " + Math.round(rate * 100) + "%" : "") + ")",
                      -rs.getDouble("discount_amount"));
                money("TOTAL", rs.getDouble("total"));
                System.out.println(Fmt.line('=', W));
                System.out.println(center("Thank you!"));
                return true;
            }
        }
    }

    // Final receipt printed at check-out.
    public static void printFinal(Connection conn, int guestId) throws SQLException {
        String sql = "SELECT g.name, g.discount_type, g.id_number, g.check_in, g.check_out, r.room_number "
                   + "FROM guests g JOIN rooms r ON r.id = g.room_id WHERE g.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                header("FINAL RECEIPT (CHECK-OUT)");
                System.out.println("Guest     : " + rs.getString("name"));
                System.out.println("Room      : " + rs.getString("room_number"));
                System.out.println("Check-in  : " + rs.getString("check_in"));
                System.out.println("Check-out : " + rs.getString("check_out"));
                String type = rs.getString("discount_type");
                double rate = Discount.rate(type);
                System.out.println("Discount  : " + type + (rate > 0 ? " (" + Math.round(rate * 100) + "%)" : ""));
                if (rs.getString("id_number") != null) {
                    System.out.println("ID number : " + rs.getString("id_number"));
                }
            }
        }

        List<Integer> orderIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM orders WHERE guest_id = ? AND status <> 'Cancelled' ORDER BY id")) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderIds.add(rs.getInt(1));
                }
            }
        }

        double subtotal = 0, discount = 0, total = 0;
        System.out.println(Fmt.line('-', W));
        if (orderIds.isEmpty()) {
            System.out.println("No room service orders.");
        } else {
            itemHeader();
        }
        for (int id : orderIds) {
            System.out.println("Order #" + id);
            printItems(conn, id);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT subtotal, discount_amount, total FROM orders WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    subtotal += rs.getDouble(1);
                    discount += rs.getDouble(2);
                    total += rs.getDouble(3);
                }
            }
        }

        System.out.println(Fmt.line('-', W));
        money("Subtotal", subtotal);
        money("Discount", -discount);
        money("TOTAL CHARGES", total);

        double paid = Billing.totalPaid(conn, guestId);
        System.out.println(Fmt.line('-', W));
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT payment_time, amount FROM payments WHERE guest_id = ? ORDER BY id")) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    money("Payment " + rs.getString("payment_time"), rs.getDouble("amount"));
                }
            }
        }
        money("TOTAL PAID", paid);
        money("BALANCE", Discount.round2(Billing.totalCharges(conn, guestId) - paid));
        System.out.println(Fmt.line('=', W));
        System.out.println(center("Thank you for staying with us!"));
    }
}
