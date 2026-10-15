package utils;

import models.CompanyUpdate;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Reads the companies from the BDI sheet, exported from Excel as "CSV UTF-8".
 *
 * <p>The columns are found by their header text in the first row, so the sheet's other columns (ח.פ., address,
 * phone, ...) can be added, removed or moved freely. The columns used:
 * <pre>
 *   שם חברה                      company name
 *   BDI ציון                     BDI score before the check
 *   BDI ציון חדש                 BDI score after the check (any header with both "BDI" and "חדש")
 *   סיבה לשינוי BDI              why the score changed
 *   נורות אדומות                 warning signs
 *   מודיעין פנימי                internal intelligence
 *   המלצת מנכ"ל                  the CEO's recommendation
 * </pre>
 * Rows without a company name are skipped (empty rows, and the list of recommendations kept beside the table).
 */
public final class CompanyParser {
    // Excel starts a UTF-8 CSV with this invisible character.
    private static final String BYTE_ORDER_MARK = String.valueOf((char) 0xFEFF);

    /** A column the report needs, and how to recognize its (normalized) header. */
    private record Column(String description, Predicate<String> matches) {
    }

    private static final Column NAME = new Column("שם חברה", h -> h.equals("שם חברה"));
    private static final Column OLD_BDI = new Column("BDI ציון",
            h -> h.contains("bdi") && h.contains("ציון") && !h.contains("חדש"));
    private static final Column NEW_BDI = new Column("BDI ציון חדש", h -> h.contains("bdi") && h.contains("חדש"));
    private static final Column REASON = new Column("סיבה לשינוי BDI", h -> h.contains("סיבה"));
    private static final Column RED_LIGHTS = new Column("נורות אדומות",
            h -> h.contains("נורות אדומות") && !h.contains("מודיעין"));
    private static final Column INTELLIGENCE = new Column("מודיעין פנימי",
            h -> h.contains("מודיעין") && !h.contains("נורות אדומות"));
    private static final Column RECOMMENDATION = new Column("המלצת מנכ\"ל", h -> h.contains("המלצת"));
    // In this order, the CompanyUpdate fields.
    private static final List<Column> COLUMNS =
            List.of(NAME, OLD_BDI, NEW_BDI, REASON, RED_LIGHTS, INTELLIGENCE, RECOMMENDATION);

    // The sheet used to have a single column for both; the report now shows them separately.
    private static final Column COMBINED_RED_LIGHTS_AND_INTELLIGENCE = new Column("נורות אדומות ומודיעין פנימי",
            h -> h.contains("נורות אדומות") && h.contains("מודיעין"));

    private CompanyParser() {
    }

    /** Returns the companies in sheet order. */
    public static List<CompanyUpdate> parseCsv(Path csvFile) throws IOException, UserFacingException {
        List<List<String>> rows = parseCsvText(readText(csvFile));
        if (rows.isEmpty()) {
            throw new UserFacingException(csvFile + " is empty. Export the sheet again.");
        }

        List<String> header = rows.getFirst();
        int combined = findColumn(header, COMBINED_RED_LIGHTS_AND_INTELLIGENCE);
        if (combined >= 0) {
            throw new UserFacingException(csvFile + " has one column for both red lights and internal intelligence (\""
                    + header.get(combined).trim() + "\"), but the report shows them separately. In the Excel sheet,"
                    + " insert a column next to it, name one \"נורות אדומות\" and the other \"מודיעין פנימי\","
                    + " move each text to its column, and export the sheet again.");
        }
        int[] index = new int[COLUMNS.size()];
        List<String> missing = new ArrayList<>();
        for (int c = 0; c < COLUMNS.size(); c++) {
            index[c] = findColumn(header, COLUMNS.get(c));
            if (index[c] < 0) {
                missing.add("\"" + COLUMNS.get(c).description() + "\"");
            }
        }
        if (!missing.isEmpty()) {
            throw new UserFacingException(csvFile + " does not look like the BDI sheet: the first row has no "
                    + String.join(", ", missing) + " column. Check that the right sheet was exported, with the"
                    + " headers in the first row. If a header was renamed, update the matching rule in CompanyParser.");
        }

        List<CompanyUpdate> companies = new ArrayList<>();
        for (List<String> row : rows.subList(1, rows.size())) {
            String name = cell(row, index[0]);
            if (name.isEmpty()) continue;
            companies.add(new CompanyUpdate(name, cell(row, index[1]), cell(row, index[2]), cell(row, index[3]),
                    cell(row, index[4]), cell(row, index[5]), cell(row, index[6])));
        }
        if (companies.isEmpty()) {
            throw new UserFacingException(csvFile + " has the sheet's headers but no companies (no row has a"
                    + " company name). Check that the right sheet was exported.");
        }
        return companies;
    }

    private static String readText(Path csvFile) throws IOException, UserFacingException {
        try {
            return new FileIOHandler().readString(csvFile).replace(BYTE_ORDER_MARK, "");
        } catch (MalformedInputException e) {
            throw new UserFacingException(csvFile + " is not saved as UTF-8, so its Hebrew text cannot be read."
                    + " In Excel, save the sheet again with File > Save As > \"CSV UTF-8 (Comma delimited)\""
                    + " (not the plain \"CSV (Comma delimited)\").", e);
        } catch (FileSystemException e) {
            throw new UserFacingException("Cannot read " + csvFile + ": " + Objects.requireNonNullElse(e.getReason(), e.toString())
                    + ". If it is open in another program, close it and run again.", e);
        }
    }

    // The index of the first header the column's rule matches, or -1.
    private static int findColumn(List<String> header, Column column) {
        for (int i = 0; i < header.size(); i++) {
            if (column.matches().test(normalize(header.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    // Lower case, single spaces, no surrounding spaces, and no direction marks Excel may keep in Hebrew text.
    private static String normalize(String header) {
        return header.replaceAll("[‎‏‪-‮]", "").replaceAll("\\s+", " ").trim()
                .toLowerCase(Locale.ROOT);
    }

    // The trimmed field at the given index, or "" if the row is shorter than that.
    private static String cell(List<String> row, int index) {
        return index < row.size() ? row.get(index).trim() : "";
    }

    /**
     * Splits CSV text into rows of fields. Commas and line breaks inside double quotes belong to the field
     * (Excel quotes a cell typed with Alt+Enter), and a doubled quote inside quotes ("מנכ""ל") is one quote.
     * A quote in the middle of an unquoted field (בע"מ) is kept as it is.
     * Line breaks inside a field become "\n".
     */
    static List<List<String>> parseCsvText(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else if (c != '\r') {
                    current.append(c);
                }
            } else if (c == '"' && current.isEmpty()) {
                // Only a quote at the start of a field opens a quoted field; one in the middle (בע"מ) is text.
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(current.toString());
                current.setLength(0);
                rows.add(fields);
                fields = new ArrayList<>();
            } else {
                current.append(c);
            }
        }
        // The last row, unless the text ended with a line break.
        if (!current.isEmpty() || !fields.isEmpty()) {
            fields.add(current.toString());
            rows.add(fields);
        }
        return rows;
    }
}
