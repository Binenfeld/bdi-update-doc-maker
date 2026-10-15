# BDIUpdateDocMaker

Turns the BDI sheet (Excel) into a Hebrew Word report of the companies whose BDI score changed, and a PDF copy
of it. A sister project of [WorkerDocMaker](https://github.com/Binenfeld/worker-doc-maker), built the same way.

```
sheet.csv (exported from Excel) ──► CompanyParser ──► List<CompanyUpdate> ──► ReportFactory ──► עדכון BDI <date>.docx ──► PdfConverter ──► .pdf
```

Step-by-step instructions in Hebrew for non-technical users, including how to fill in the sheet:
[`הוראות הפעלה.txt`](הוראות%20הפעלה.txt).

## What the report contains

One landscape page (more if needed), right to left:

| Part | Content |
|---|---|
| Title | `עדכון דירוגי BDI` |
| Summary line | Today's date, how many companies were checked, how many changed score (up / down / first score) |
| Table | `שם החברה`, `BDI מקור`, `BDI חדש`, `סיבה לשינוי BDI`, `נורות אדומות`, `מודיעין פנימי`, `המלצת מנכ"ל`; one row per company whose score changed, in sheet order. The two score cells are colored by score |
| Legend | One cell per score, 10 (dark green) to 0 (dark red) |

A company is listed when its new score is filled in and differs from the old one; a company's first score (no
old score) counts as a change. If no score changed, the report says so instead of showing the table.
Empty cells show `—`, and line breaks inside a cell (Alt+Enter) are kept.

**The BDI scale** is 0–10, 10 being the lowest risk (Coface's scale; BDI is Coface's partner in Israel). The
scale and its colors are in `models/BdiGrade`. A score that isn't a whole number from 0 to 10 is shown as typed,
without a color, and the run prints a warning.

## Requirements

- JDK 21 or newer (the project is developed with JDK 26)
- Gradle, through the included wrapper (`gradlew`); no external libraries are needed
- Excel, to export the sheet to CSV
- Microsoft Word (desktop), to convert the report to PDF. `PdfConverter` drives it through PowerShell, so this
  needs Windows. Without Word the run fails after the .docx is written.

## Running it

1. **Export the sheet.** Open the BDI workbook, select the table's sheet and use File → Save As →
   **CSV UTF-8 (Comma delimited)**. Save it in `C:\bdiupdatedocmaker` under any name ending in `.csv`. It must be
   the UTF-8 variant, or the Hebrew text will be garbled.
2. **Build** (once, and again after code changes): double-click **`scripts\build.cmd`**, or run
   ```
   powershell -ExecutionPolicy Bypass -File scripts\build.ps1
   ```
   This produces `build\libs\BDIUpdateDocMaker.jar`. The script uses `JAVA_HOME` if it is set, otherwise the
   newest JDK in `~\.jdks` (where IntelliJ downloads JDKs).
3. **Run:** double-click **`scripts\run.cmd`**, or run
   ```
   powershell -ExecutionPolicy Bypass -File scripts\run.ps1
   ```
   Both `.cmd` files keep the window open at the end to show the result. You can also run `Main` from IntelliJ.
   Options of `run.ps1`:
   - `-InputFolder D:\some\folder`: use a different folder
   - `-Debug`: also print every company read, and the full stack trace of an error
4. The program reads the **newest** `.csv` in the folder and writes the report next to it as
   `עדכון BDI <yyyy-mm-dd>.docx`, plus a PDF copy made with Word.

The jar takes the same options directly: `java -jar BDIUpdateDocMaker.jar [folder] [--debug]`.

### When something goes wrong

The run stops with exit code 1 and a line starting with `ERROR:` that says what is wrong and how to fix it: a
missing folder or CSV, a CSV saved as plain CSV instead of CSV UTF-8, a missing column, a report left open in
Word, and a missing Word install. An `ERROR: Unexpected error`
comes with a stack trace; that one is a bug or something unusual on the computer.

## Input format

`CompanyParser` finds the columns by their header text in the first row, so other columns can be added, removed or
moved. Matching ignores case, extra spaces and direction marks.

| Column | Header must… | Meaning |
|---|---|---|
| Company | be `שם חברה` | Company name; rows without one are skipped |
| Old score | contain `BDI` and `ציון`, not `חדש` | BDI score before the check |
| New score | contain `BDI` and `חדש` | BDI score after the check |
| Reason | contain `סיבה` | Why the score changed |
| Red lights | contain `נורות אדומות`, not `מודיעין` | Warning signs |
| Intelligence | contain `מודיעין`, not `נורות אדומות` | Internal intelligence |
| Recommendation | contain `המלצת` | The CEO's recommendation |

The original sheet had a single `נורות אדומות ומודיעין פנימי` column. The report shows the two separately, so such
a sheet is rejected with instructions to split the column.

The CSV reader handles quoted cells with commas, doubled quotes (`מנכ""ל`) and line breaks, and a quote in the
middle of an unquoted cell (`בע"מ`).

## Project structure

```
scripts/
├── build.ps1 / build.cmd     builds build\libs\BDIUpdateDocMaker.jar (the .cmd is for double-clicking)
└── run.ps1 / run.cmd         runs the jar on the newest CSV in C:\bdiupdatedocmaker
src/main/java/
├── Main.java                 entry point: newest CSV → parse → write report → PDF
├── models/
│   ├── CompanyUpdate.java    one company's row: scores, reason, red lights, intelligence, recommendation
│   └── BdiGrade.java         the 0-10 BDI scale and its colors
└── utils/
    ├── CompanyParser.java    reads the CSV, finding the columns by header
    ├── ReportFactory.java    lays out the report
    ├── DocxWriter.java       minimal .docx writer (RTL, landscape, colored tables and cells) without libraries
    ├── PdfConverter.java     .docx → PDF through Word (PowerShell + COM); always closes the Word it starts
    ├── UserFacingException.java  a problem the user can fix; Main prints its message without a stack trace
    └── FileIOHandler.java    small UTF-8 file helpers
```

## License

Copyright (C) 2026 Binenfeld

This program is free software: you can redistribute it and/or modify it under the terms of the
[GNU General Public License](LICENSE) as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. It is distributed WITHOUT ANY WARRANTY; see the license for details.
