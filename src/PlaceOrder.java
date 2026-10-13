import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Room service: place an order for a checked-in guest.
public class PlaceOrder {

    private static class Line {
        int itemId;
        String name;
        double price;
        int qty;
    }

    public static void run() {
        System.out.println("\n=== PLACE ORDER ===");
        String room = InputHelper.readString("Room number: ");

        boolean exitProgram = false;
        try (Connection conn = Database.getConnection()) {
            Billing.GuestInfo guest = Billing.findCurrentGuest(conn, room);
            if (guest == null) {
                System.out.println("No checked-in guest in that room.");
                return;
            }
            double rate = Discount.rate(guest.discountType);
            System.out.println("Guest: " + guest.name + "   Discount: " + guest.discountType
                + (rate > 0 ? " (" + Math.round(rate * 100) + "%)" : ""));

            List<Line> lines = new ArrayList<>();
            while (true) {
                if (MenuViewer.printMenu(conn, true) == 0) {
                    System.out.println("No items are available right now.");
                    return;
                }
                int productId = InputHelper.readInt("\nProduct number (0 to cancel order): ", 0, 100000);
                if (productId == 0) {
                    if (lines.isEmpty() || InputHelper.readYesNo("Cancel this whole order?")) {
                        System.out.println("Order cancelled.");
                        return;
                    }
                    continue;
                }
                Line item = findItem(conn, productId);
                if (item == null) {
                    System.out.println("Error: invalid or unavailable product.");
                    continue;
                }
                int qty = InputHelper.readInt("Quantity for " + item.name + " (1-50): ", 1, 50);
                addToList(lines, item, qty);
                showList(lines);
                if (InputHelper.readYesNo("Add another item?")) {
                    continue;
                }
                break;
            }

            double subtotal = 0;
            for (Line l : lines) {
                subtotal += l.price * l.qty;
            }
            subtotal = Discount.round2(subtotal);
            double discount = Discount.round2(subtotal * rate);
            double total = Discount.round2(subtotal - discount);

            int orderId = save(conn, guest, lines, subtotal, discount, total);
            Receipt.printOrder(conn, orderId);

            if (!InputHelper.readYesNo("\nContinue? (Y = back to menu, N = exit program)")) {
                exitProgram = true;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        if (exitProgram) {
            System.out.println("Goodbye!");
            System.exit(0);
        }
    }

    private static Line findItem(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, price FROM menu_items WHERE id = ? AND active = 1 AND available = 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Line l = new Line();
                l.itemId = rs.getInt("id");
                l.name = rs.getString("name");
                l.price = rs.getDouble("price");
                return l;
            }
        }
    }

    private static void addToList(List<Line> lines, Line item, int qty) {
        for (Line l : lines) {
            if (l.itemId == item.itemId) {
                l.qty += qty;
                return;
            }
        }
        item.qty = qty;
        lines.add(item);
    }

    private static void showList(List<Line> lines) {
        System.out.println("\nYour order so far:");
        double sub = 0;
        for (Line l : lines) {
            System.out.println("  " + Fmt.fit(l.name, 24) + String.format("%3d x %9s", l.qty, Fmt.money(l.price)));
            sub += l.price * l.qty;
        }
        System.out.println("  Subtotal: " + Fmt.money(sub));
    }

    // Saves the order, its items and the charge in one transaction. Returns the order id.
    private static int save(Connection conn, Billing.GuestInfo guest, List<Line> lines,
                            double subtotal, double discount, double total) throws SQLException {
        conn.setAutoCommit(false);
        try {
            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO orders (guest_id, order_time, status, subtotal, discount_amount, total) "
                  + "VALUES (?, datetime('now', 'localtime'), 'Pending', ?, ?, ?)")) {
                ps.setInt(1, guest.id);
                ps.setDouble(2, subtotal);
                ps.setDouble(3, discount);
                ps.setDouble(4, total);
                ps.executeUpdate();
            }
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                orderId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO order_items (order_id, menu_item_id, quantity, price) VALUES (?, ?, ?, ?)")) {
                for (Line l : lines) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, l.itemId);
                    ps.setInt(3, l.qty);
                    ps.setDouble(4, l.price);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO charges (guest_id, order_id, amount, description) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, guest.id);
                ps.setInt(2, orderId);
                ps.setDouble(3, total);
                ps.setString(4, "Room service order #" + orderId);
                ps.executeUpdate();
            }
            conn.commit();
            return orderId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
