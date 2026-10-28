package autograder.reporting;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Writes two CSV files:
 * - the scoresheet: a copy of the instructor's scoresheet template with the grade numerator filled in
 * - the details report: per-question scores, manual marks, comments and submission issues
 */
public class CsvExporter {
    // header for detailed report
    private static final List<String> detailHeaders = Arrays.asList(
        "Username", 
        "Q1a Score", "Q1a Manual Score", "Q1a Grading Status",
        "Q1b Score", "Q1b Manual Score", "Q1b Grading Status", "Q1 Comments",
        "Q2a Score", "Q2a Manual Score", "Q2a Grading Status",
        "Q2b Score", "Q2b Manual Score", "Q2b Grading Status", "Q2 Comments",
        "Q3 Score", "Q3 Manual Score", "Q3 Grading Status", "Q3 Comments",
        "Total Score", "Grading Status", "Validation Status", 
        "Submission Issues", "Q1 Issues", "Q2 Issues", "Q3 Issues");

    // scoresheet template rows and the columns we read or fill in
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
    // scoresheet username "#ping.lee.2023" -> "ping.lee.2023" for matching
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

    // username
    private String formatUsername(String username) {
        if (username == null) {
            return "";
        }
        username = username.trim();
        while (username.startsWith("#")) {
            username = username.substring(1);
        }

        return "#" + username;
    }

    // find question by ID
    private QuestionResult findQuestion(StudentResult student, String questionId) {
        for (QuestionResult question : student.getQuestions()) {
            if (question.getQuestionId().equals(questionId)) {
                return question;
            }
        }
        return null;
    }

    // add score, manual score, grading status for qns in detailed report
    private void addQuestionDetails(List<String> row, StudentResult student, String questionId) {
        QuestionResult question = findQuestion(student, questionId);
        if (question == null) {
            row.add("");
            row.add("");
            row.add("No result");
        } else {
            // final score includes manual marks after
            double finalScore = student.getFinalScore(question);
            row.add(Double.toString(finalScore) + " / " + Double.toString(question.getMaxScore()));
            
            // manual marks
            if (student.hasManualScore(questionId)) {
                row.add(Double.toString(finalScore));
            } else {
                row.add("");
            }

            // grading status
            row.add(question.getStatusMessage());
        }
    }

    // comments (combined so qn1a and qn1b comments same box)
    // varargs let method accept 0+ string arguments as individual strings, array or nothing
    private String comments(StudentResult student, String...questionIds) {
        String comments = "";

        for (String questionId : questionIds) {
            String comment = student.getComment(questionId);

            if (comment != null && !comment.isBlank()) {
                if (!comments.isEmpty()) {
                    comments = comments + "; ";
                }
                comments = comments + questionId + ": " + comment;
            }
        }
        return comments;
    }

    // prevent csv formatting issues
    private static String encode(List<List<String>> records) {
        StringBuilder text = new StringBuilder();

        for (List<String> record : records) {
            for (int i = 0; i < record.size(); i++) {
                if (i > 0) {
                    text.append(',');
                }
                String value = record.get(i);
                if (value == null) {
                    value = "";
                }

                text.append('"');
                text.append(value.replace("\"", "\"\""));
                text.append('"');
            }
            text.append("\r\n");
        }
        return text.toString();
    }

    // final grade csv: fill the numerator column of the scoresheet template
    private String buildScoresCsv(List<StudentResult> results) throws IOException {
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
        return encode(output);
    }

    // detailed report csv
    private String buildDetailsCsv(List<StudentResult> results) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(detailHeaders));

        for (StudentResult student : results) {
            List<String> row = new ArrayList<>();
            
            String username = formatUsername(student.getSubmission().getUsername());
            row.add(username);
            
            // q1 scores and comments
            addQuestionDetails(row, student, "Q1a");
            addQuestionDetails(row, student, "Q1b");
            row.add(comments(student, "Q1a", "Q1b"));

            // q2 scores and comments
            addQuestionDetails(row, student, "Q2a");
            addQuestionDetails(row, student, "Q2b");
            row.add(comments(student, "Q2a", "Q2b"));

            // q3 score and comments
            addQuestionDetails(row, student, "Q3");
            row.add(comments(student, "Q3"));

            // total
            if (student.getQuestions().isEmpty()) {
                row.add("");
            } else {
                row.add(Double.toString(student.getTotal()) + " / 20.0");
            }

            // new submission issues
            String submissionIssues = String.join("; ", student.getSubmission().getSubmissionIssues());
            String q1Issues = String.join("; ", student.getSubmission().getQ1Issues());
            String q2Issues = String.join("; ", student.getSubmission().getQ2Issues());
            String q3Issues = String.join("; ", student.getSubmission().getQ3Issues());

            row.add(student.getGradingStatus());
            row.add(student.getSubmission().getOverallStatus());
            row.add(submissionIssues);
            row.add(q1Issues);
            row.add(q2Issues);
            row.add(q3Issues);

            rows.add(row);
        }
        return encode(rows);
    }

    public void export(Path scoresFile, Path detailsFile, List<StudentResult> results) throws IOException {
        if (scoresFile == null || detailsFile == null) {
            throw new IOException("The output file paths cannot be empty.");
        }

        if (results == null) {
            throw new IOException("Student results cannot be null");
        }

        // Build and validate both outputs before writing either file.
        String scoresCsv = buildScoresCsv(results);
        String detailsCsv = buildDetailsCsv(results);

        if (scoresFile.toAbsolutePath().getParent() != null) {
            Files.createDirectories(scoresFile.toAbsolutePath().getParent());
        }
        if (detailsFile.toAbsolutePath().getParent() != null) {
            Files.createDirectories(detailsFile.toAbsolutePath().getParent());
        }

        Files.writeString(scoresFile, scoresCsv, StandardCharsets.UTF_8);
        Files.writeString(detailsFile, detailsCsv, StandardCharsets.UTF_8);
    }

    /** Handles quoted commas, escaped quotes, multiline fields and UTF-8 BOM. */
    private static List<List<String>> parse(String text) throws IOException {
        if (text.startsWith("﻿")) text = text.substring(1);
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
