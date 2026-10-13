public class Main {

    public static void main(String[] args) {
        Database.init();

        boolean running = true;
        while (running) {
            System.out.println("\n=== SOGO HOTEL ROOM SERVICE POS ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");

            int choice = InputHelper.readInt("Choose: ", 0, 2);

            switch (choice) {
                case 1:
                    AuthService.login();
                    break;
                case 2:
                    AuthService.register();
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    running = false;
                    break;
            }
        }
    }
}
