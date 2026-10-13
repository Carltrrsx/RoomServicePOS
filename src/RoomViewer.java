import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Front desk: list rooms, their status and who is staying.
public class RoomViewer {
    public static void run() {
        String sql = "SELECT r.room_number, r.status, g.name FROM rooms r "
                   + "LEFT JOIN guests g ON g.room_id = r.id AND g.check_out IS NULL "
                   + "WHERE r.active = 1 ORDER BY r.room_number";
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("\n=== ROOMS AND STATUS ===");
            System.out.println(Fmt.fit("Room", 8) + Fmt.fit("Status", 14) + "Guest");
            System.out.println(Fmt.line('-', 44));
            while (rs.next()) {
                String guest = rs.getString("name");
                System.out.println(Fmt.fit(rs.getString("room_number"), 8)
                    + Fmt.fit(rs.getString("status"), 14) + (guest == null ? "-" : guest));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        InputHelper.pause();
    }
}
