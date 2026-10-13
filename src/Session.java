// Remembers who is logged in right now.
public class Session {
    public static int userId;
    public static String username;
    public static String fullName;
    public static String role;   // "Admin", "Front Desk" or "Room Service"

    public static void login(int id, String user, String name, String userRole) {
        userId = id;
        username = user;
        fullName = name;
        role = userRole;
    }

    public static void logout() {
        userId = 0;
        username = null;
        fullName = null;
        role = null;
    }
}
