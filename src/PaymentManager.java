import java.sql.Connection;
import java.sql.SQLException;

// Front desk: record a payment for a guest.
public class PaymentManager {
    public static void run() {
        System.out.println("\n=== RECORD PAYMENT ===");
        String room = InputHelper.readString("Room number: ");
        try (Connection conn = Database.getConnection()) {
            Billing.GuestInfo g = Billing.findCurrentGuest(conn, room);
            if (g == null) {
                System.out.println("No checked-in guest in that room.");
                return;
            }
            double balance = Billing.balance(conn, g.id);
            System.out.println("Guest: " + g.name + "   Balance due: " + Fmt.money(balance));
            if (balance <= 0.004) {
                System.out.println("Nothing to pay. The balance is already zero.");
                return;
            }
            double amount = Billing.askPaymentAmount(balance);
            if (amount == 0) {
                System.out.println("Payment cancelled.");
                return;
            }
            Billing.recordPayment(conn, g.id, amount);
            System.out.println("Payment recorded. New balance: " + Fmt.money(Billing.balance(conn, g.id)));
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
