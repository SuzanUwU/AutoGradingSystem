package autograder.reporting;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsvExporter {
    private final List<List<String>> rows;
    private final int usernameColumn;
    private final int scoreColumn;
    private final int maximumColumn;
    private final int firstNameColumn;
    private final int lastNameColumn;

    public CsvExporter(Path template) throws IOException {
        rows = parse(Files.readString(template, StandardCharsets.UTF_8));
        if (rows.isEmpty()) throw new IOException("The scoresheet is empty.");
        List<String> header = rows.get(0);
        usernameColumn = column(header, "Username");
        scoreColumn = column(header, "Calculated Final Grade Numerator");
        maximumColumn = column(header, "Calculated Final Grade Denominator");
        firstNameColumn = column(header, "First Name");
        lastNameColumn = column(header, "Last Name");
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < rows.size(); i++) {
            if (rows.get(i).size() != header.size()) throw new IOException("Invalid scoresheet row " + (i + 1));
            if (!seen.add(username(rows.get(i).get(usernameColumn)))) throw new IOException("Duplicate username in scoresheet.");
        }
    }
    private static int column(List<String> header, String name) throws IOException {
        int index = header.indexOf(name);
        if (index < 0) throw new IOException("Scoresheet is missing column: " + name);
        return index;
    }
    private static String username(String value) {
        return value.replaceFirst("^#", "").trim().toLowerCase(Locale.ROOT);
    }
    public Map<String, String> getRosterNames() {
        Map<String, String> names = new LinkedHashMap<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            names.put(username(row.get(usernameColumn)),
                    (row.get(firstNameColumn) + " " + row.get(lastNameColumn).replace("_", "")).trim());
        }
        return names;
    }
    public void export(Path scoresFile, Path detailsFile, List<StudentResult> results) throws IOException {
        Map<String, StudentResult> byUsername = new LinkedHashMap<>();
        for (StudentResult result : results) {
            String name = username(result.getSubmission().getUsername());
            if (byUsername.put(name, result) != null) throw new IOException("Duplicate submission username: " + name);
            if (!getRosterNames().containsKey(name)) throw new IOException("Student missing from scoresheet: " + name);
        }
        List<List<String>> output = new ArrayList<>();
        output.add(new ArrayList<>(rows.get(0)));
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = new ArrayList<>(rows.get(i));
            StudentResult result = byUsername.get(username(row.get(usernameColumn)));
            if (result != null) {
                if (result.getQuestions().isEmpty()) row.set(scoreColumn, "");
                else {
                    double maximum;
                    try { maximum = Double.parseDouble(row.get(maximumColumn)); }
                    catch (NumberFormatException ex) { throw new IOException("Invalid denominator for " + row.get(usernameColumn)); }
                    if (maximum != result.getMaximum()) throw new IOException("Scoresheet denominator differs from grading maximum for " + row.get(usernameColumn));
                    row.set(scoreColumn, Double.toString(result.getTotal()));
                }
            }
            output.add(row);
        }
        List<List<String>> details = new ArrayList<>();
        details.add(List.of("Username", "Question", "Autograded Score", "Manual Marked?", "Manual Score", "Manual Comment", "Final Score", "Maximum Score", "Grading Status", "Validation Status"));
        for (StudentResult result : results) {
            String name = result.getSubmission().getUsername();
            if (result.getQuestions().isEmpty()) {
                details.add(List.of(name, "", "", "No", "", "", "", "", result.getGradingError(),
                        result.getSubmission().getOverallStatus()));
            }
            for (QuestionResult q : result.getQuestions()) {
                boolean manuallyMarked = result.hasManualScore(q.getQuestionId());
                details.add(List.of(name, q.getQuestionId(), Double.toString(q.getScore()),
                        manuallyMarked ? "Yes" : "No",
                        manuallyMarked ? Double.toString(result.getFinalScore(q)) : "",
                        result.getComment(q.getQuestionId()), Double.toString(result.getFinalScore(q)),
                        Double.toString(q.getMaxScore()), q.getStatusMessage(),
                        result.getSubmission().getOverallStatus()));
            }
        }
        // Build and validate both outputs before writing either file.
        Files.writeString(detailsFile, encode(details), StandardCharsets.UTF_8);
        Files.writeString(scoresFile, encode(output), StandardCharsets.UTF_8);
    }
    private static String encode(List<List<String>> records) {
        StringBuilder text = new StringBuilder();
        for (List<String> record : records) {
            for (int i = 0; i < record.size(); i++) {
                if (i > 0) text.append(',');
                text.append('"').append(record.get(i).replace("\"", "\"\"")).append('"');
            }
            text.append("\r\n");
        }
        return text.toString();
    }
    /** Handles quoted commas, escaped quotes, multiline fields and UTF-8 BOM. */
    private static List<List<String>> parse(String text) throws IOException {
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') { field.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { row.add(field.toString()); field.setLength(0); }
            else if ((c == '\r' || c == '\n') && !quoted) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); records.add(row); row = new ArrayList<>(); field.setLength(0);
            } else field.append(c);
        }
        if (quoted) throw new IOException("Unclosed quoted field in scoresheet.");
        if (field.length() > 0 || !row.isEmpty()) { row.add(field.toString()); records.add(row); }
        return records;
    }
}
