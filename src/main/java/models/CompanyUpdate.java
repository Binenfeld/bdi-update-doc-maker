package models;

import java.util.Optional;

/**
 * One company's row of the BDI sheet: its BDI score before and after the latest check, and what to do about it.
 * Every field is the cell's text as typed (trimmed, never null); the scores are read with {@link BdiGrade#parse}.
 *
 * @param name           company name (שם חברה)
 * @param oldBdi         BDI score before the check (BDI ציון)
 * @param newBdi         BDI score after the check (BDI ציון חדש)
 * @param reason         why the score changed (סיבה לשינוי BDI)
 * @param redLights      warning signs (נורות אדומות)
 * @param intelligence   internal intelligence: what we heard about the company (מודיעין פנימי)
 * @param recommendation the CEO's recommendation (המלצת מנכ"ל)
 */
public record CompanyUpdate(String name, String oldBdi, String newBdi, String reason, String redLights,
                            String intelligence, String recommendation) {

    public Optional<BdiGrade> oldGrade() {
        return BdiGrade.parse(oldBdi);
    }

    public Optional<BdiGrade> newGrade() {
        return BdiGrade.parse(newBdi);
    }

    /**
     * True if a new score is filled in and differs from the old one; a first score (no old one) counts as a change.
     * Scores that aren't 0-10 are compared as text.
     */
    public boolean changed() {
        if (newBdi.isEmpty()) {
            return false;
        }
        if (oldGrade().isPresent() && newGrade().isPresent()) {
            return oldGrade().get() != newGrade().get();
        }
        return !newBdi.equals(oldBdi);
    }

    /** True if both scores are known and the new one is higher (lower risk). */
    public boolean improved() {
        return oldGrade().isPresent() && newGrade().isPresent() && newGrade().get().score() > oldGrade().get().score();
    }

    /** True if both scores are known and the new one is lower (higher risk). */
    public boolean worsened() {
        return oldGrade().isPresent() && newGrade().isPresent() && newGrade().get().score() < oldGrade().get().score();
    }

    @Override
    public String toString() {
        return String.format(
                "Company[%s | BDI %s -> %s | Reason: %s | Red lights: %s | Intelligence: %s | Recommendation: %s]",
                ltr(name), ltr(oldBdi), ltr(newBdi), ltr(reason), ltr(redLights), ltr(intelligence),
                ltr(recommendation));
    }

    // Left-to-right mark: keeps Hebrew values from reordering the separators around them in the console.
    private static final String LRM = "‎";

    private static String ltr(String value) {
        return LRM + value.replace("\n", " / ") + LRM;
    }
}
