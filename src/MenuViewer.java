import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Shows the menu with categories and prices.
public class MenuViewer {

    public static void run() {
        try (Connection conn = Database.getConnection()) {
            printMenu(conn, false);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }

    // Prints the menu grouped by category. If onlyAvailable is true, sold-out items are hidden.
    // Returns how many items were printed.
    public static int printMenu(Connection conn, boolean onlyAvailable) throws SQLException {
        String sql = "SELECT m.id, m.name, m.price, m.available, c.name AS category "
                   + "FROM menu_items m JOIN categories c ON c.id = m.category_id "
                   + "WHERE m.active = 1 " + (onlyAvailable ? "AND m.available = 1 " : "")
                   + "ORDER BY c.id, m.id";
        int count = 0;
        String lastCategory = "";
        System.out.println("\n=== MENU ===");
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String cat = rs.getString("category");
                if (!cat.equals(lastCategory)) {
                    System.out.println("\n-- " + cat.toUpperCase() + " --");
                    lastCategory = cat;
                }
                String note = rs.getInt("available") == 1 ? "" : "  (sold out)";
                System.out.println(String.format("%3d. ", rs.getInt("id")) + Fmt.fit(rs.getString("name"), 26)
                    + String.format("%10s", Fmt.money(rs.getDouble("price"))) + note);
                count++;
            }
        }
        if (count == 0) {
            System.out.println("(no items)");
        }
        return count;
    }
}
