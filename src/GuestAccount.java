import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// View a guest's account (charges, payments, balance).
public class GuestAccount {

    public static void run() {
        boolean back = false;
        while (!back) {
            System.out.println("\n=== GUEST ACCOUNTS ===");
            System.out.println("1. View a guest's account (by room number)");
            System.out.println("2. List all checked-in guests and balances");
            System.out.println("0. Back");
            int choice = InputHelper.readInt("Choose: ", 0, 2);
            switch (choice) {
                case 1: viewOne(); break;
                case 2: listAll(); break;
                case 0: back = true; break;
            }
        }
    }

    private static void viewOne() {
        String room = InputHelper.readString("Room number: ");
        try (Connection conn = Database.getConnection()) {
            Billing.GuestInfo g = Billing.findCurrentGuest(conn, room);
            if (g == null) {
                System.out.println("No checked-in guest in that room.");
                return;
            }
            Billing.showAccount(conn, g);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }

    private static void listAll() {
        try (Connection conn = Database.getConnection()) {
            List<Billing.GuestInfo> guests = Billing.currentGuests(conn);
            if (guests.isEmpty()) {
                System.out.println("No guests are checked in.");
                return;
            }
            System.out.println("\n" + Fmt.fit("Room", 7) + Fmt.fit("Guest", 22) + Fmt.fit("Discount", 10)
                + String.format("%12s%12s%12s", "Charges", "Paid", "Balance"));
            System.out.println(Fmt.line('-', 75));
            for (Billing.GuestInfo g : guests) {
                double charges = Billing.totalCharges(conn, g.id);
                double paid = Billing.totalPaid(conn, g.id);
                System.out.println(Fmt.fit(g.roomNumber, 7) + Fmt.fit(g.name, 22) + Fmt.fit(g.discountType, 10)
                    + String.format("%12s%12s%12s", Fmt.money(charges), Fmt.money(paid),
                                    Fmt.money(Discount.round2(charges - paid))));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return;
        }
        InputHelper.pause();
    }
}
