package models;

import java.util.Optional;

/**
 * The BDI credit score scale and the color each score is shown in.
 *
 * <p>BDI (Coface's partner in Israel) scores companies from 0 to 10: 10 is the lowest risk (dark green),
 * 0 the highest (dark red). The colors run from dark green through yellow to red, like BDI's own reports.
 * If the scale or the colors change, this is the only place to update.
 */
public enum BdiGrade {
    GRADE_0(0, "A50026"),
    GRADE_1(1, "D73027"),
    GRADE_2(2, "F46D43"),
    GRADE_3(3, "FDAE61"),
    GRADE_4(4, "FEE08B"),
    GRADE_5(5, "FFFFBF"),
    GRADE_6(6, "D9EF8B"),
    GRADE_7(7, "A6D96A"),
    GRADE_8(8, "66BD63"),
    GRADE_9(9, "1A9850"),
    GRADE_10(10, "006837");

    public static final int BEST = 10;
    public static final int WORST = 0;

    private final int score;
    private final String color; // hex RGB, no '#'

    BdiGrade(int score, String color) {
        this.score = score;
        this.color = color;
    }

    public int score() {
        return score;
    }

    public String color() {
        return color;
    }

    /**
     * The grade written in a sheet cell ("7", " 7 ", "7.0"), or empty if the cell is blank or not a score
     * from 0 to 10.
     */
    public static Optional<BdiGrade> parse(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            double value = Double.parseDouble(text.trim());
            if (value != Math.rint(value)) {
                return Optional.empty();
            }
            int score = (int) value;
            return score >= WORST && score <= BEST ? Optional.of(values()[score]) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
