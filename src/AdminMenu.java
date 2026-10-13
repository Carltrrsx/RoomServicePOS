public class AdminMenu {
    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. Manage staff accounts");
            System.out.println("2. Manage rooms");
            System.out.println("3. Manage menu and prices");
            System.out.println("4. View guests and bills");
            System.out.println("5. Orders and reports");
            System.out.println("0. Log out");

            int choice = InputHelper.readInt("Choose: ", 0, 5);
            switch (choice) {
                case 1: StaffManager.run(); break;
                case 2: RoomManager.run(); break;
                case 3: MenuManager.run(); break;
                case 4: GuestAccount.run(); break;
                case 5: Reports.run(); break;
                case 0: back = true; break;
            }
        }
    }
}
