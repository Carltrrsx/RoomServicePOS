import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:roomservice.db";

    // Every part of the program calls this to talk to the database.
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    // Call this once at the start of Main.
    public static void init() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            seedIfEmpty(conn);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS rooms ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "room_number TEXT NOT NULL UNIQUE, "
                + "status TEXT NOT NULL DEFAULT 'Available', "
                + "active INTEGER NOT NULL DEFAULT 1)");

            stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password TEXT NOT NULL, "
                + "full_name TEXT NOT NULL, "
                + "role TEXT NOT NULL, "
                + "active INTEGER NOT NULL DEFAULT 1)");

            stmt.execute("CREATE TABLE IF NOT EXISTS categories ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL UNIQUE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS menu_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "price REAL NOT NULL, "
                + "category_id INTEGER NOT NULL, "
                + "available INTEGER NOT NULL DEFAULT 1, "
                + "active INTEGER NOT NULL DEFAULT 1, "
                + "FOREIGN KEY (category_id) REFERENCES categories(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS guests ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "contact TEXT, "
                + "room_id INTEGER NOT NULL, "
                + "discount_type TEXT NOT NULL DEFAULT 'None', "
                + "id_number TEXT, "
                + "check_in TEXT NOT NULL, "
                + "check_out TEXT, "
                + "FOREIGN KEY (room_id) REFERENCES rooms(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "guest_id INTEGER NOT NULL, "
                + "order_time TEXT NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'Pending', "
                + "subtotal REAL NOT NULL DEFAULT 0, "
                + "discount_amount REAL NOT NULL DEFAULT 0, "
                + "total REAL NOT NULL DEFAULT 0, "
                + "FOREIGN KEY (guest_id) REFERENCES guests(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS order_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "order_id INTEGER NOT NULL, "
                + "menu_item_id INTEGER NOT NULL, "
                + "quantity INTEGER NOT NULL, "
                + "price REAL NOT NULL, "
                + "FOREIGN KEY (order_id) REFERENCES orders(id), "
                + "FOREIGN KEY (menu_item_id) REFERENCES menu_items(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS charges ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "guest_id INTEGER NOT NULL, "
                + "order_id INTEGER, "
                + "amount REAL NOT NULL, "
                + "description TEXT, "
                + "FOREIGN KEY (guest_id) REFERENCES guests(id), "
                + "FOREIGN KEY (order_id) REFERENCES orders(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS payments ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "guest_id INTEGER NOT NULL, "
                + "amount REAL NOT NULL, "
                + "payment_time TEXT NOT NULL, "
                + "FOREIGN KEY (guest_id) REFERENCES guests(id))");
        }
    }

    // Fills the database with sample data, but only the very first time.
    private static void seedIfEmpty(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.getInt(1) > 0) {
                return; // already seeded
            }
        }

        try (Statement stmt = conn.createStatement()) {

            // Test accounts (one per role)
            stmt.execute("INSERT INTO users (username, password, full_name, role) VALUES "
                + "('admin', 'admin123', 'System Admin', 'Admin'), "
                + "('frontdesk', 'front123', 'Front Desk Staff', 'Front Desk'), "
                + "('roomservice', 'room123', 'Room Service Staff', 'Room Service')");

            // Rooms (101 is occupied by the sample guest below)
            stmt.execute("INSERT INTO rooms (room_number, status) VALUES "
                + "('101', 'Occupied'), ('102', 'Available'), ('103', 'Available'), "
                + "('201', 'Available'), ('202', 'Available'), ('203', 'Maintenance')");

            stmt.execute("INSERT INTO categories (name) VALUES "
                + "('Breakfast'), ('Main Course'), ('Snacks'), ('Drinks'), ('Desserts'), ('Services')");
        }

        // 30 menu items: {name, category, price}  (prices are placeholders, edit them)
        String[][] items = {
            {"Tapsilog", "Breakfast", "220"},
            {"Longsilog", "Breakfast", "200"},
            {"Tosilog", "Breakfast", "210"},
            {"Pancakes", "Breakfast", "180"},
            {"Continental Breakfast", "Breakfast", "350"},
            {"Chicken Adobo", "Main Course", "280"},
            {"Beef Sinigang", "Main Course", "350"},
            {"Pork Sisig", "Main Course", "320"},
            {"Fried Chicken", "Main Course", "290"},
            {"Club Sandwich", "Main Course", "310"},
            {"Spaghetti Bolognese", "Main Course", "260"},
            {"Beef Burger", "Main Course", "330"},
            {"French Fries", "Snacks", "150"},
            {"Chicken Nuggets", "Snacks", "200"},
            {"Nachos", "Snacks", "210"},
            {"Garden Salad", "Snacks", "180"},
            {"Mixed Fruit Platter", "Snacks", "220"},
            {"Bottled Water", "Drinks", "60"},
            {"Iced Tea", "Drinks", "90"},
            {"Coffee", "Drinks", "100"},
            {"Orange Juice", "Drinks", "110"},
            {"Soft Drink", "Drinks", "80"},
            {"Mango Shake", "Drinks", "140"},
            {"Halo-Halo", "Desserts", "160"},
            {"Leche Flan", "Desserts", "120"},
            {"Chocolate Cake", "Desserts", "170"},
            {"Ice Cream", "Desserts", "130"},
            {"Extra Towels", "Services", "50"},
            {"Extra Pillow", "Services", "50"},
            {"Laundry Service", "Services", "250"}
        };

        String sql = "INSERT INTO menu_items (name, price, category_id) "
                   + "VALUES (?, ?, (SELECT id FROM categories WHERE name = ?))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] item : items) {
                ps.setString(1, item[0]);
                ps.setDouble(2, Double.parseDouble(item[2]));
                ps.setString(3, item[1]);
                ps.executeUpdate();
            }
        }

        try (Statement stmt = conn.createStatement()) {

            // One checked-in guest in room 101
            stmt.execute("INSERT INTO guests (name, contact, room_id, discount_type, check_in) "
                + "VALUES ('Juan Dela Cruz', '09171234567', "
                + "(SELECT id FROM rooms WHERE room_number = '101'), 'None', "
                + "datetime('now', 'localtime'))");

            // One sample delivered order so the guest account is not empty
            stmt.execute("INSERT INTO orders (guest_id, order_time, status, subtotal, discount_amount, total) "
                + "VALUES (1, datetime('now', 'localtime'), 'Delivered', 510, 0, 510)");
            stmt.execute("INSERT INTO order_items (order_id, menu_item_id, quantity, price) VALUES "
                + "(1, (SELECT id FROM menu_items WHERE name = 'Beef Burger'), 1, 330), "
                + "(1, (SELECT id FROM menu_items WHERE name = 'Iced Tea'), 2, 90)");
            stmt.execute("INSERT INTO charges (guest_id, order_id, amount, description) "
                + "VALUES (1, 1, 510, 'Room service order #1')");
        }
    }
}
