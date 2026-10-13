// Discount rates. Change them here if your teacher gives different rates.
public class Discount {

    public static final double STUDENT = 0.10;  // ask your teacher for the student rate
    public static final double SENIOR  = 0.20;  // Senior citizen: 20% by law
    public static final double PWD     = 0.20;  // PWD: 20% by law

    public static double rate(String type) {
        if (type == null) {
            return 0;
        }
        switch (type) {
            case "Student": return STUDENT;
            case "Senior":  return SENIOR;
            case "PWD":     return PWD;
            default:        return 0;
        }
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
