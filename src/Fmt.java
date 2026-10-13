// Small helpers for printing money and tidy columns.
public class Fmt {

    public static String money(double v) {
        return String.format("%,.2f", v);
    }

    // Cuts or pads text to an exact width (for table columns)
    public static String fit(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() > width) {
            return s.substring(0, width - 1) + ".";
        }
        return String.format("%-" + width + "s", s);
    }

    public static String line(char c, int n) {
        return String.valueOf(c).repeat(n);
    }
}
