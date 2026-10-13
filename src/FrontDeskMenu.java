public class FrontDeskMenu {
    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== FRONT DESK MENU ===");
            System.out.println("1. Check in guest");
            System.out.println("2. Check out guest");
            System.out.println("3. View rooms and status");
            System.out.println("4. View guest account");
            System.out.println("5. Record payment");
            System.out.println("0. Log out");

            int choice = InputHelper.readInt("Choose: ", 0, 5);
            switch (choice) {
                case 1: CheckIn.run(); break;
                case 2: CheckOut.run(); break;
                case 3: RoomViewer.run(); break;
                case 4: GuestAccount.run(); break;
                case 5: PaymentManager.run(); break;
                case 0: back = true; break;
            }
        }
    }
}
