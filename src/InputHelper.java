import java.util.Scanner;

public class InputHelper {

    private static final Scanner sc = new Scanner(System.in);

    // Whole number between min and max (keeps asking until valid)
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // Decimal number that must be at least min
    public static double readDouble(String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                double value = Double.parseDouble(line);
                if (value >= min) {
                    return value;
                }
                System.out.println("Value must be at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    // Text that cannot be empty
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("This cannot be empty.");
        }
    }

    // Text that may be left empty (just press Enter to skip)
    public static String readOptional(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    // Y or N
    public static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (Y/N): ");
            String line = sc.nextLine().trim().toUpperCase();
            if (line.equals("Y") || line.equals("YES")) {
                return true;
            }
            if (line.equals("N") || line.equals("NO")) {
                return false;
            }
            System.out.println("Please type Y or N.");
        }
    }

    public static void pause() {
        System.out.print("\nPress Enter to continue...");
        sc.nextLine();
    }
}
