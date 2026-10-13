import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// Shared guest and billing.
public class Billing {

    public static class GuestInfo {
        public int id;
        public int roomId;
        public String name;
        public String roomNumber;
        public String discountType;
        public String idNumber;
        public String checkIn;
    }

    private static final String GUEST_SQL =
        "SELECT g.id, g.name, g.room_id, r.room_number, g.discount_type, g.id_number, g.check_in "
      + "FROM guests g JOIN rooms r ON r.id = g.room_id WHERE g.check_out IS NULL ";

    private static GuestInfo read(ResultSet rs) throws SQLException {
        GuestInfo g = new GuestInfo();
        g.id = rs.getInt("id");
        g.name = rs.getString("name");
        g.roomId = rs.getInt("room_id");
        g.roomNumber = rs.getString("room_number");
        g.discountType = rs.getString("discount_type");
        g.idNumber = rs.getString("id_number");
        g.checkIn = rs.getString("check_in");
        return g;
    }

    // The guest currently staying in a room (null if nobody)
    public static GuestInfo findCurrentGuest(Connection conn, String roomNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GUEST_SQL + "AND r.room_number = ?")) {
            ps.setString(1, roomNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        }
    }

    public static List<GuestInfo> currentGuests(Connection conn) throws SQLException {
        List<GuestInfo> list = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(GUEST_SQL + "ORDER BY r.room_number")) {
            while (rs.next()) {
                list.add(read(rs));
            }
        }
        return list;
    }

    private static double sumFor(Connection conn, String sql, int guestId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public static double totalCharges(Connection conn, int guestId) throws SQLException {
        return sumFor(conn, "SELECT COALESCE(SUM(amount), 0) FROM charges WHERE guest_id = ?", guestId);
    }

    public static double totalPaid(Connection conn, int guestId) throws SQLException {
        return sumFor(conn, "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE guest_id = ?", guestId);
    }

    public static double balance(Connection conn, int guestId) throws SQLException {
        return Discount.round2(totalCharges(conn, guestId) - totalPaid(conn, guestId));
    }

    // Asks for a payment amount. Returns 0 if the user cancels.
    public static double askPaymentAmount(double balance) {
        while (true) {
            double amount = InputHelper.readDouble("Payment amount (0 to cancel): ", 0);
            if (amount == 0) {
                return 0;
            }
            amount = Discount.round2(amount);
            if (amount > balance + 0.004) {
                System.out.println("Error: amount is more than the balance (" + Fmt.money(balance) + ").");
                continue;
            }
            return amount;
        }
    }

    public static void recordPayment(Connection conn, int guestId, double amount) throws SQLException {
        String sql = "INSERT INTO payments (guest_id, amount, payment_time) "
                   + "VALUES (?, ?, datetime('now', 'localtime'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            ps.setDouble(2, amount);
            ps.executeUpdate();
        }
    }

    public static void showAccount(Connection conn, GuestInfo g) throws SQLException {
        double rate = Discount.rate(g.discountType);
        System.out.println("\n" + Fmt.line('=', 50));
        System.out.println("GUEST ACCOUNT");
        System.out.println(Fmt.line('=', 50));
        System.out.println("Guest      : " + g.name);
        System.out.println("Room       : " + g.roomNumber);
        System.out.println("Checked in : " + g.checkIn);
        System.out.println("Discount   : " + g.discountType
            + (rate > 0 ? " (" + Math.round(rate * 100) + "%)" : ""));

        System.out.println("\nCHARGES");
        boolean any = false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT description, amount FROM charges WHERE guest_id = ? ORDER BY id")) {
            ps.setInt(1, g.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    any = true;
                    System.out.println("  " + Fmt.fit(rs.getString("description"), 34)
                        + String.format("%12s", Fmt.money(rs.getDouble("amount"))));
                }
            }
        }
        if (!any) {
            System.out.println("  (no charges yet)");
        }

        System.out.println("\nPAYMENTS");
        any = false;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT payment_time, amount FROM payments WHERE guest_id = ? ORDER BY id")) {
            ps.setInt(1, g.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    any = true;
                    System.out.println("  " + Fmt.fit(rs.getString("payment_time"), 34)
                        + String.format("%12s", Fmt.money(rs.getDouble("amount"))));
                }
            }
        }
        if (!any) {
            System.out.println("  (no payments yet)");
        }

        double charges = totalCharges(conn, g.id);
        double paid = totalPaid(conn, g.id);
        System.out.println(Fmt.line('-', 50));
        System.out.println(Fmt.fit("Total charges", 36) + String.format("%12s", Fmt.money(charges)));
        System.out.println(Fmt.fit("Total paid", 36) + String.format("%12s", Fmt.money(paid)));
        System.out.println(Fmt.fit("BALANCE", 36) + String.format("%12s", Fmt.money(Discount.round2(charges - paid))));
        System.out.println(Fmt.line('=', 50));
    }
}
