package utils;

import models.BdiGrade;
import models.CompanyUpdate;
import utils.DocxWriter.TableColors;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the Hebrew BDI update report (.docx), on landscape pages so the long text columns have room.
 *
 * <p>Document layout:
 * <pre>
 *   Title:      עדכון דירוגי BDI
 *   Line:       date | companies checked | how many changed score, went up and went down
 *   Table:      company | BDI מקור | BDI חדש | reason for the change | red lights | internal intelligence |
 *               CEO's recommendation
 *               one row per company whose score changed, in sheet order; the score cells are colored by score
 *               (if no score changed: a line saying so instead of the table and legend)
 *   Heading 2:  מקרא ציוני BDI  -> one cell per score from 10 (dark green) to 0 (dark red)
 * </pre>
 */
public final class ReportFactory {
    private static final String NO_VALUE = "—";

    private static final int OLD_BDI_COLUMN = 1;
    private static final int NEW_BDI_COLUMN = 2;

    private ReportFactory() {
    }

    /**
     * Writes the report to {@code path}, overwriting any existing file. Only the companies whose score changed
     * ({@link CompanyUpdate#changed()}) are listed; if none did, the report says so.
     */
    public static void writeReport(List<CompanyUpdate> companies, Path path) throws IOException {
        List<CompanyUpdate> changed = companies.stream().filter(CompanyUpdate::changed).toList();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        DocxWriter doc = new DocxWriter(true, true)
                .addTitle("עדכון דירוגי BDI")
                .addParagraph("נכון לתאריך " + today + "  |  " + summary(companies.size(), changed));

        if (changed.isEmpty()) {
            doc.addParagraph("לא היו שינויים בדירוגי BDI.");
        } else {
            addCompaniesTable(doc, changed);
            addLegend(doc);
        }

        doc.save(path);
    }

    // "11 חברות נבדקו  |  8 שינו דירוג (3 עלו, 4 ירדו, 1 דירוג ראשון)"; parts that are 0 are left out.
    private static String summary(int checked, List<CompanyUpdate> changed) {
        long up = changed.stream().filter(CompanyUpdate::improved).count();
        long down = changed.stream().filter(CompanyUpdate::worsened).count();
        long first = changed.stream().filter(c -> c.oldBdi().isEmpty()).count();

        List<String> parts = new ArrayList<>();
        if (up > 0) parts.add(up + " עלו");
        if (down > 0) parts.add(down + " ירדו");
        if (first > 0) parts.add(first + " דירוג ראשון");
        return checked + " חברות נבדקו  |  " + changed.size() + " שינו דירוג"
                + (parts.isEmpty() ? "" : " (" + String.join(", ", parts) + ")");
    }

    private static void addCompaniesTable(DocxWriter doc, List<CompanyUpdate> companies) {
        List<String> headers = List.of("שם החברה", "BDI מקור", "BDI חדש", "סיבה לשינוי BDI",
                "נורות אדומות", "מודיעין פנימי", "המלצת מנכ\"ל");
        List<List<String>> rows = new ArrayList<>();
        for (CompanyUpdate c : companies) {
            rows.add(List.of(c.name(), orNoValue(c.oldBdi()), orNoValue(c.newBdi()), orNoValue(c.reason()),
                    orNoValue(c.redLights()), orNoValue(c.intelligence()), orNoValue(c.recommendation())));
        }
        doc.addStyledTable(TableColors.BLUE, headers, rows, Set.of(OLD_BDI_COLUMN, NEW_BDI_COLUMN),
                (row, column) -> switch (column) {
                    case OLD_BDI_COLUMN -> colorOf(companies.get(row).oldGrade());
                    case NEW_BDI_COLUMN -> colorOf(companies.get(row).newGrade());
                    default -> null;
                },
                5, 3, 3, 6, 6, 6, 5);
    }

    // One cell per score, best on the right (the reading start) to worst on the left.
    private static void addLegend(DocxWriter doc) {
        doc.addHeading(2, "מקרא ציוני BDI");
        List<String> headers = new ArrayList<>();
        List<String> scores = new ArrayList<>();
        for (int score = BdiGrade.BEST; score >= BdiGrade.WORST; score--) {
            scores.add(String.valueOf(score));
        }
        headers.add("טוב ביותר");
        for (int i = 1; i < scores.size() - 1; i++) {
            headers.add("");
        }
        headers.add("גרוע ביותר");
        doc.addStyledTable(TableColors.BLUE, headers, List.of(scores), allColumns(scores.size()),
                (row, column) -> BdiGrade.parse(scores.get(column)).map(BdiGrade::color).orElse(null));
    }

    private static Set<Integer> allColumns(int columns) {
        Set<Integer> result = new HashSet<>();
        for (int c = 0; c < columns; c++) {
            result.add(c);
        }
        return result;
    }

    private static String colorOf(Optional<BdiGrade> grade) {
        return grade.map(BdiGrade::color).orElse(null);
    }

    private static String orNoValue(String value) {
        return value.isEmpty() ? NO_VALUE : value;
    }
}
