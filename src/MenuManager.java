import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Admin: add, view, edit and delete menu items, plus categories and availability.
public class MenuManager {

    private static class Item {
        int id;
        String name;
        double price;
        int categoryId;
        boolean available;
        boolean active;
    }

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== MANAGE MENU AND PRICES ===");
            System.out.println("1. Add item");
            System.out.println("2. View all items");
            System.out.println("3. Edit item");
            System.out.println("4. Delete item");
            System.out.println("5. Set item available / sold out");
            System.out.println("6. Add category");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 6);
            try (Connection conn = Database.getConnection()) {
                switch (choice) {
                    case 1: add(conn); break;
                    case 2: view(conn); break;
                    case 3: edit(conn); break;
                    case 4: delete(conn); break;
                    case 5: toggle(conn); break;
                    case 6: addCategory(conn); break;
                    case 0: back = true; break;
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private static Item find(Connection conn, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, price, category_id, available, active FROM menu_items WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Item it = new Item();
                it.id = rs.getInt("id");
                it.name = rs.getString("name");
                it.price = rs.getDouble("price");
                it.categoryId = rs.getInt("category_id");
                it.available = rs.getInt("available") == 1;
                it.active = rs.getInt("active") == 1;
                return it;
            }
        }
    }

    // Shows the categories and returns the chosen category id (0 = cancel)
    private static int chooseCategory(Connection conn) throws SQLException {
        System.out.println("Categories:");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, name FROM categories ORDER BY id")) {
            while (rs.next()) {
                System.out.println("  " + rs.getInt(1) + ". " + rs.getString(2));
            }
        }
        while (true) {
            int id = InputHelper.readInt("Category number (0 to cancel): ", 0, 100000);
            if (id == 0) {
                return 0;
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM categories WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return id;
                    }
                }
            }
            System.out.println("That category does not exist.");
        }
    }

    private static void add(Connection conn) throws SQLException {
        String name = InputHelper.readString("Item name: ");
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM menu_items WHERE LOWER(name) = LOWER(?) AND active = 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Error: an item with that name already exists.");
                    return;
                }
            }
        }
        int cat = chooseCategory(conn);
        if (cat == 0) {
            System.out.println("Cancelled.");
            return;
        }
        double price = InputHelper.readDouble("Price (more than 0): ", 0.01);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO menu_items (name, price, category_id) VALUES (?, ?, ?)")) {
            ps.setString(1, name);
            ps.setDouble(2, Discount.round2(price));
            ps.setInt(3, cat);
            ps.executeUpdate();
        }
        System.out.println("Item added.");
    }

    private static void view(Connection conn) throws SQLException {
        System.out.println("\n" + Fmt.fit("ID", 5) + Fmt.fit("Item", 26) + Fmt.fit("Category", 14)
            + String.format("%10s", "Price") + "  Status");
        System.out.println(Fmt.line('-', 68));
        String sql = "SELECT m.id, m.name, c.name AS category, m.price, m.available, m.active "
                   + "FROM menu_items m JOIN categories c ON c.id = m.category_id ORDER BY c.id, m.id";
        int count = 0;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String status = rs.getInt("active") == 0 ? "Inactive"
                              : rs.getInt("available") == 1 ? "Available" : "Sold out";
                System.out.println(Fmt.fit(String.valueOf(rs.getInt("id")), 5) + Fmt.fit(rs.getString("name"), 26)
                    + Fmt.fit(rs.getString("category"), 14) + String.format("%10s", Fmt.money(rs.getDouble("price")))
                    + "  " + status);
                count++;
            }
        }
        System.out.println(count + " item(s)");
        InputHelper.pause();
    }

    private static void edit(Connection conn) throws SQLException {
        int id = InputHelper.readInt("Item ID to edit (0 to cancel): ", 0, 100000);
        if (id == 0) {
            return;
        }
        Item it = find(conn, id);
        if (it == null || !it.active) {
            System.out.println("Error: item not found.");
            return;
        }
        System.out.println("Editing: " + it.name + " (" + Fmt.money(it.price) + ")");

        String name = InputHelper.readOptional("New name (Enter to keep): ");
        if (name.isEmpty()) {
            name = it.name;
        }

        double price = it.price;
        String p = InputHelper.readOptional("New price (Enter to keep): ");
        if (!p.isEmpty()) {
            try {
                price = Double.parseDouble(p);
            } catch (NumberFormatException e) {
                System.out.println("Error: invalid price.");
                return;
            }
            if (price <= 0) {
                System.out.println("Error: price must be more than 0.");
                return;
            }
        }

        int cat = it.categoryId;
        if (InputHelper.readYesNo("Change category?")) {
            int c = chooseCategory(conn);
            if (c != 0) {
                cat = c;
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE menu_items SET name = ?, price = ?, category_id = ? WHERE id = ?")) {
            ps.setString(1, name);
            ps.setDouble(2, Discount.round2(price));
            ps.setInt(3, cat);
            ps.setInt(4, it.id);
            ps.executeUpdate();
        }
        System.out.println("Item updated.");
    }

    private static void delete(Connection conn) throws SQLException {
        int id = InputHelper.readInt("Item ID to delete (0 to cancel): ", 0, 100000);
        if (id == 0) {
            return;
        }
        Item it = find(conn, id);
        if (it == null || !it.active) {
            System.out.println("Error: item not found.");
            return;
        }
        if (!InputHelper.readYesNo("Delete " + it.name + "?")) {
            System.out.println("Delete cancelled.");
            return;
        }
        int used;
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM order_items WHERE menu_item_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                used = rs.getInt(1);
            }
        }
        if (used > 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE menu_items SET active = 0, available = 0 WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            System.out.println("This item is in past orders, so it was marked inactive instead of deleted.");
        } else {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM menu_items WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            System.out.println("Item deleted.");
        }
    }

    private static void toggle(Connection conn) throws SQLException {
        int id = InputHelper.readInt("Item ID (0 to cancel): ", 0, 100000);
        if (id == 0) {
            return;
        }
        Item it = find(conn, id);
        if (it == null || !it.active) {
            System.out.println("Error: item not found.");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE menu_items SET available = ? WHERE id = ?")) {
            ps.setInt(1, it.available ? 0 : 1);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
        System.out.println(it.name + " is now " + (it.available ? "SOLD OUT." : "AVAILABLE."));
    }

    private static void addCategory(Connection conn) throws SQLException {
        String name = InputHelper.readString("New category name: ");
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM categories WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Error: that category already exists.");
                    return;
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO categories (name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
        System.out.println("Category added.");
    }
}
