public class RoomServiceMenu {
    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== ROOM SERVICE MENU ===");
            System.out.println("1. Place order");
            System.out.println("2. Update order status");
            System.out.println("3. Cancel order");
            System.out.println("4. View menu and prices");
            System.out.println("5. Order history");
            System.out.println("0. Log out");

            int choice = InputHelper.readInt("Choose: ", 0, 5);
            switch (choice) {
                case 1: PlaceOrder.run(); break;
                case 2: UpdateOrderStatus.run(); break;
                case 3: CancelOrder.run(); break;
                case 4: MenuViewer.run(); break;
                case 5: OrderHistory.run(); break;
                case 0: back = true; break;
            }
        }
    }
}
