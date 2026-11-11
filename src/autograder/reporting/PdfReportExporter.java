package autograder.reporting;

// AI-assisted development using ChatGPT; reviewed and modified by the team.

import autograder.grading.EdgeCaseRunner;
import autograder.model.QuestionResult;
import autograder.model.StudentResult;
import autograder.model.SubmissionResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PdfReportExporter {
    private static final Color DARK = new Color(45, 49, 53);
    private static final Color GREEN = new Color(40, 120, 85);
    private static final Color LIGHT_GREEN = new Color(232, 243, 237);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER = new Color(210, 213, 216);
    private static final Color AMBER = new Color(255, 244, 214);
    private static final Color LIGHT_RED = new Color(251, 231, 229);
    private static final Color WHITE = Color.WHITE;

    private static final Color VALID_BG = new Color(220, 245, 220);
    private static final Color CORRECTED_BG = new Color(220, 235, 255);
    private static final Color REVIEW_BG = new Color(255, 242, 194);
    private static final Color FAILED_BG = new Color(255, 220, 220);

    private static final Color VALID_TEXT = new Color(20, 110, 40);
    private static final Color CORRECTED_TEXT = new Color(35, 85, 200);
    private static final Color REVIEW_TEXT = new Color(180, 90, 0);
    private static final Color FAILED_TEXT = new Color(190, 30, 30);

    private static final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    // Keep the old export method so existing code can still generate a PDF without edge-case results.
    public void export(Path reportFile, List<StudentResult> students) throws IOException {
        export(reportFile, students, Map.of(), "Auto Grading Assessment");
    }

    // Updated export method to include edge-case results and assessment type.
    public void export(Path reportFile, List<StudentResult> students, Map<String, EdgeCaseRunner.Result> edgeResults, String assessment) throws IOException {
        if (reportFile == null) throw new IOException("The PDF report path cannot be empty.");
        if (students == null) throw new IOException("Student results cannot be null.");
        if (edgeResults == null) edgeResults = Map.of();

        Path absolutePath = reportFile.toAbsolutePath();
        if (absolutePath.getParent() != null) Files.createDirectories(absolutePath.getParent());

        // create pdf with batch summary, student reports and review queue
        try (PDDocument document = new PDDocument()) {
            ReportWriter writer = new ReportWriter(document);
            addBatchSummary(writer, students, edgeResults, assessment);

            for (StudentResult student : students) {
                writer.newPage();
                addStudentReport(writer, student, edgeResults.get(student.getSubmission().getUsername()));
            }

            writer.newPage();
            addReviewQueue(writer, students, edgeResults);
            writer.close();
            document.save(absolutePath.toFile());
        }
    }

    // Display overall grading, validation, correction and edge-case information.
    private void addBatchSummary(ReportWriter writer, List<StudentResult> students, Map<String, EdgeCaseRunner.Result> edgeResults, String assessment) throws IOException {
        writer.addTitle("AUTO GRADING REPORT", assessment + " | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        int total = students.size(), graded = 0, review = 0, failed = 0;

        for (StudentResult student : students) {
            if (student.isGraded()) graded++;
            if (student.needsAttention()) review++;
            if ("Failed".equalsIgnoreCase(student.getSubmission().getOverallStatus())) failed++;
        }

        writer.addCards(new String[][]{
                {"STUDENTS", String.valueOf(total)},
                {"GRADED", String.valueOf(graded)},
                {"REVIEW", String.valueOf(review)},
                {"FAILED", String.valueOf(failed)}
        });

        writer.addSection("Validation Overview");

        List<String[]> validationRows = new ArrayList<>();

        for (String status : List.of("Valid", "Corrected", "Review", "Failed")) {
            int count = 0;

            for (StudentResult student : students)
                if (status.equalsIgnoreCase(student.getSubmission().getOverallStatus())) count++;

            String percentage = total == 0 ? "0%" : String.format(Locale.ROOT, "%.1f%%", count * 100.0 / total);
            validationRows.add(new String[]{status, String.valueOf(count), percentage});
        }

        writer.addTable(new String[]{"Status", "Students", "Percentage"}, validationRows, .45f, .25f, .30f);

        writer.addSection("Grade Statistics");

        List<StudentResult> gradedStudents = new ArrayList<>();
        for (StudentResult student : students) if (!student.getQuestions().isEmpty()) gradedStudents.add(student);

        List<String[]> gradeRows = new ArrayList<>();

        if (gradedStudents.isEmpty()) {
            gradeRows.add(new String[]{"Average", "Not available"});
            gradeRows.add(new String[]{"Highest", "Not available"});
            gradeRows.add(new String[]{"Lowest", "Not available"});
        } else {
            double average = gradedStudents.stream().mapToDouble(StudentResult::getTotal).average().orElse(0);
            StudentResult highest = Collections.max(gradedStudents, Comparator.comparingDouble(StudentResult::getTotal));
            StudentResult lowest = Collections.min(gradedStudents, Comparator.comparingDouble(StudentResult::getTotal));

            gradeRows.add(new String[]{"Average", String.format(Locale.ROOT, "%.2f", average)});
            gradeRows.add(new String[]{"Highest", formatScore(highest) + " (" + highest.getSubmission().getUsername() + ")"});
            gradeRows.add(new String[]{"Lowest", formatScore(lowest) + " (" + lowest.getSubmission().getUsername() + ")"});
        }

        writer.addTable(new String[]{"Metric", "Result"}, gradeRows, .35f, .65f);

        writer.addSection("Correction Overview");

        int folder = 0, name = 0, email = 0, open = 0, anomaly = 0;

        for (StudentResult student : students) {
            SubmissionResult submission = student.getSubmission();
            folder += submission.getFolderFixCount();
            name += submission.getNameFixCount();
            email += submission.getEmailFixCount();
            open += submission.getOpenIssueCount();
            anomaly += submission.getAnomalyCount();
        }

        writer.addTable(new String[]{"Category", "Total"}, List.of(
                new String[]{"Folder corrections", String.valueOf(folder)},
                new String[]{"Name corrections", String.valueOf(name)},
                new String[]{"Email corrections", String.valueOf(email)},
                new String[]{"Open issues", String.valueOf(open)},
                new String[]{"Anomalies", String.valueOf(anomaly)}
        ), .70f, .30f);

        writer.addSection("Edge-Case Overview");

        if (edgeResults.isEmpty()) {
            writer.addNote("Edge-case testing has not been run.");
        } else {
            int passed = 0, possible = 0, skipped = 0;

            for (EdgeCaseRunner.Result result : edgeResults.values()) {
                if (result.isSkipped()) skipped++;
                else {
                    passed += result.getPassed();
                    possible += result.getTotal();
                }
            }

            writer.addTable(new String[]{"Metric", "Result"}, List.of(
                    new String[]{"Cases passed", passed + " / " + possible},
                    new String[]{"Cases failed", String.valueOf(Math.max(0, possible - passed))},
                    new String[]{"Students skipped", String.valueOf(skipped)}
            ), .70f, .30f);
        }
    }

    // Display individual student grading, correction history and edge-case results.
    private void addStudentReport(ReportWriter writer, StudentResult student, EdgeCaseRunner.Result edgeResult) throws IOException {
        SubmissionResult submission = student.getSubmission();

        writer.addTitle(formatUsername(submission.getUsername()), "Individual Student Report");

        writer.addCards(new String[][]{
                {"VALIDATION", submission.getOverallStatus()},
                {"GRADING", student.getGradingStatus()},
                {"SCORE", student.getQuestions().isEmpty() ? "N/A" : formatScore(student)},
                {"EDGE CASES", edgeResult == null ? "Not run" : edgeResult.isSkipped() ? "Skipped" : edgeResult.getPassed() + "/" + edgeResult.getTotal()}
        });

        writer.addSection("Official Grades");

        List<String[]> questionRows = new ArrayList<>();

        // Display every question returned by the grading engine.
        // Supports Java, Python, and future assessments.
        for (QuestionResult question : student.getQuestions()) {
            String manualMark = student.hasManualScore(question.getQuestionId()) ? String.format(Locale.ROOT, "%.1f", student.getFinalScore(question)) : "-";

            questionRows.add(new String[]{
                    question.getQuestionId(),
                    String.format(Locale.ROOT, "%.1f", question.getScore()),
                    manualMark,
                    String.format(Locale.ROOT, "%.1f", student.getFinalScore(question)),
                    String.format(Locale.ROOT, "%.1f", question.getMaxScore()),
                    question.getStatusMessage()
            });
        }

        if (questionRows.isEmpty()) questionRows.add(new String[]{"-", "-", "-", "-", "-", "No grading result"});

        writer.addTable(new String[]{"Question", "Auto", "Manual", "Final", "Max", "Status"},
                questionRows, .14f, .11f, .12f, .11f, .10f, .42f);

        // Correction history is included in the PDF report
        writer.addSection("Validation & Correction History");

        writer.addTable(new String[]{"Category", "Count", "Details"}, List.of(
                new String[]{"Folder", String.valueOf(submission.getFolderFixCount()), details(submission.getFolderFixDetails())},
                new String[]{"Name", String.valueOf(submission.getNameFixCount()), details(submission.getNameFixDetails())},
                new String[]{"Email", String.valueOf(submission.getEmailFixCount()), details(submission.getEmailFixDetails())},
                new String[]{"Open Issues", String.valueOf(submission.getOpenIssueCount()), details(submission.getOpenIssueDetails())},
                new String[]{"Anomalies", String.valueOf(submission.getAnomalyCount()), details(submission.getAnomalyDetails())}
        ), .20f, .12f, .68f);

        writer.addSection("Remaining Issues / Review Items");

        List<String[]> issues = new ArrayList<>();
        addIssues(issues, "Submission", submission.getSubmissionIssues());
        addIssues(issues, "Q1", submission.getQ1Issues());
        addIssues(issues, "Q2", submission.getQ2Issues());
        addIssues(issues, "Q3", submission.getQ3Issues());

        if (issues.isEmpty()) writer.addNote("No remaining issues requiring review.");
        else writer.addTable(new String[]{"Scope", "Finding"}, issues, .20f, .80f);

        // Edge-case results are included in the PDF report
        writer.addSection("Edge-Case Diagnostics");

        if (edgeResult == null) {
            writer.addNote("Edge-case testing has not been run for this student.");
        } else if (edgeResult.isSkipped()) {
            writer.addNote("Skipped: " + edgeResult.getError());
        } else {
            List<String[]> edgeRows = new ArrayList<>();

            for (String questionId : List.of("Q1a", "Q1b", "Q2a", "Q2b", "Q3")) {
                EdgeCaseRunner.QuestionResult question = edgeResult.get(questionId);
                if (question == null) continue;

                String result = question.getPassed() == question.getTotal() ? "Pass"
                        : question.getPassed() == 0 ? "Failed" : "Partial";

                edgeRows.add(new String[]{
                        questionId,
                        String.valueOf(question.getPassed()),
                        String.valueOf(question.getTotal()),
                        result,
                        question.getStatus()
                });
            }

            writer.addTable(new String[]{"Question", "Passed", "Total", "Result", "Status"},
                    edgeRows, .16f, .13f, .12f, .18f, .41f);
        }

        List<String[]> manualRows = new ArrayList<>();

        for (QuestionResult question : student.getQuestions()) {
            if (!student.hasManualScore(question.getQuestionId())) continue;

            manualRows.add(new String[]{
                    question.getQuestionId(),
                    String.format(Locale.ROOT, "%.1f", question.getScore()),
                    String.format(Locale.ROOT, "%.1f", student.getFinalScore(question)),
                    student.getComment(question.getQuestionId())
            });
        }

        // Only show this section when a manual score exists.
        if (!manualRows.isEmpty()) {
            writer.addSection("Manual Score Adjustments");
            writer.addTable(new String[]{"Question", "Original", "Final", "Reason"}, manualRows, .15f, .15f, .15f, .55f);
        }
    }

    // Display students that may need the professor's attention.
    private void addReviewQueue(ReportWriter writer, List<StudentResult> students, Map<String, EdgeCaseRunner.Result> edgeResults) throws IOException {
        writer.addTitle("REVIEW QUEUE", "Students requiring instructor attention");

        List<String[]> rows = new ArrayList<>();

        for (StudentResult student : students) {
            EdgeCaseRunner.Result edgeResult = edgeResults.get(student.getSubmission().getUsername());
            boolean edgeIssue = edgeResult != null && (edgeResult.isSkipped() || edgeResult.getFailed() > 0);

            if (!student.needsAttention() && !edgeIssue) continue;

            rows.add(new String[]{
                    formatUsername(student.getSubmission().getUsername()),
                    student.getSubmission().getOverallStatus(),
                    student.getGradingStatus(),
                    edgeResult == null ? "Not run" : edgeResult.isSkipped() ? "Skipped" : edgeResult.getPassed() + "/" + edgeResult.getTotal()
            });
        }

        if (rows.isEmpty()) writer.addNote("No students currently require review.");
        else writer.addTable(new String[]{"Student", "Validation", "Grading", "Edge Cases"}, rows, .35f, .22f, .22f, .21f);
    }

    private static String formatScore(StudentResult student) {
        return String.format(Locale.ROOT, "%.1f / %.1f", student.getTotal(), student.getMaximum());
    }

    private static String details(List<String> list) {
        return list == null || list.isEmpty() ? "None" : String.join("; ", list);
    }

    private static void addIssues(List<String[]> rows, String scope, List<String> issues) {
        if (issues != null) for (String issue : issues) if (issue != null && !issue.isBlank()) rows.add(new String[]{scope, issue});
    }

    // format username to have # infront
    private static String formatUsername(String username) {
        if (username == null) return "";
        username = username.trim();
        while (username.startsWith("#")) username = username.substring(1);
        return "#" + username;
    }

    // writes tables, sections and creates new pages when required
    private static class ReportWriter {
        private static final float margin = 40;
        private static final float bottomMargin = 40;

        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private int pageNumber;

        ReportWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < bottomMargin) newPage();
        }

        private void newPage() throws IOException {
            if (content != null) {
                addFooter();
                content.close();
            }

            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - margin;
            pageNumber++;
        }

        private void addTitle(String title, String subtitle) throws IOException {
            ensureSpace(55);
            drawBox(margin, y - 40, getWidth(), 40, DARK);
            addText(title, margin + 10, y - 17, bold, 16, WHITE);
            addText(subtitle, margin + 10, y - 31, normal, 8, WHITE);
            y -= 55;
        }

        private void addCards(String[][] cards) throws IOException {
            ensureSpace(52);
            float gap = 7;
            float cardWidth = (getWidth() - gap * (cards.length - 1)) / cards.length;

            for (int i = 0; i < cards.length; i++) {
                float x = margin + i * (cardWidth + gap);

                // Keep summary boxes neutral without status colours.
                drawBox(x, y - 38, cardWidth, 38, LIGHT_GRAY);
                drawBorder(x, y - 38, cardWidth, 38);
                addText(cards[i][0], x + 6, y - 11, bold, 7, DARK);

                List<String> lines = wrapText(cards[i][1], bold, 10, cardWidth - 12);

                for (int j = 0; j < Math.min(2, lines.size()); j++)
                    addText(lines.get(j), x + 6, y - 25 - j * 10, bold, 10, DARK);
            }

            y -= 50;
        }

        private void addSection(String title) throws IOException {
            ensureSpace(24);
            addText(title, margin, y - 13, bold, 11, DARK);
            y -= 24;
        }

        private void addNote(String text) throws IOException {
            List<String> lines = wrapText(text, normal, 8, getWidth() - 16);
            float height = Math.max(26, lines.size() * 10 + 12);

            ensureSpace(height + 6);
            drawBox(margin, y - height, getWidth(), height, LIGHT_GRAY);
            drawBorder(margin, y - height, getWidth(), height);

            float lineY = y - 15;

            for (String line : lines) {
                addText(line, margin + 8, lineY, normal, 8, DARK);
                lineY -= 10;
            }

            y -= height + 7;
        }

        private void addTable(String[] headers, List<String[]> rows, float... ratios) throws IOException {
            float[] widths = getWidths(ratios);
            addTableHeader(headers, widths);

            boolean alternate = false;

            for (String[] row : rows) {
                float rowHeight = getRowHeight(row, widths);

                if (y - rowHeight < bottomMargin) {
                    newPage();
                    addTableHeader(headers, widths);
                }

                float x = margin;

                for (int i = 0; i < widths.length; i++) {
                    String value = i < row.length && row[i] != null ? row[i] : "";

                    // Keep the existing table design, but colour status cells same as the GUI.
                    Color background = alternate ? LIGHT_GRAY : WHITE;
                    Color textColor = DARK;

                    if (value.equalsIgnoreCase("Valid")) {
                        background = VALID_BG;
                        textColor = VALID_TEXT;
                    } else if (value.equalsIgnoreCase("Corrected")) {
                        background = CORRECTED_BG;
                        textColor = CORRECTED_TEXT;
                    } else if (value.equalsIgnoreCase("Review")) {
                        background = REVIEW_BG;
                        textColor = REVIEW_TEXT;
                    } else if (value.equalsIgnoreCase("Failed")) {
                        background = FAILED_BG;
                        textColor = FAILED_TEXT;
                    }

                    drawBox(x, y - rowHeight, widths[i], rowHeight, background);
                    drawBorder(x, y - rowHeight, widths[i], rowHeight);

                    List<String> lines = wrapText(value, normal, 8, widths[i] - 8);
                    float lineY = y - 13;

                    for (String line : lines) {
                        addText(line, x + 4, lineY, normal, 8, textColor);
                        lineY -= 10;
                    }

                    x += widths[i];
                }

                y -= rowHeight;
                alternate = !alternate;
            }

            y -= 8;
        }

        private void addTableHeader(String[] headers, float[] widths) throws IOException {
            ensureSpace(22);
            float x = margin;

            for (int i = 0; i < headers.length; i++) {
                drawBox(x, y - 21, widths[i], 21, DARK);
                addText(headers[i], x + 4, y - 14, bold, 7.5f, WHITE);
                x += widths[i];
            }

            y -= 21;
        }

        private float getRowHeight(String[] row, float[] widths) throws IOException {
            int maxLines = 1;

            for (int i = 0; i < widths.length; i++) {
                String value = i < row.length && row[i] != null ? row[i] : "";
                maxLines = Math.max(maxLines, wrapText(value, normal, 8, widths[i] - 8).size());
            }

            return Math.max(22, maxLines * 10 + 8);
        }

        private float[] getWidths(float[] ratios) {
            float total = 0;
            for (float ratio : ratios) total += ratio;

            float[] widths = new float[ratios.length];
            for (int i = 0; i < ratios.length; i++) widths[i] = getWidth() * ratios[i] / total;

            return widths;
        }

        private List<String> wrapText(String text, PDType1Font font, float size, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();

            if (text == null || text.isBlank()) {
                lines.add("");
                return lines;
            }

            StringBuilder line = new StringBuilder();

            for (String word : toPdfText(text).split("\\s+")) {
                String test = line.length() == 0 ? word : line + " " + word;

                if (font.getStringWidth(test) / 1000f * size <= maxWidth) line = new StringBuilder(test);
                else {
                    if (line.length() > 0) lines.add(line.toString());
                    line = new StringBuilder(word);
                }
            }

            if (line.length() > 0) lines.add(line.toString());
            return lines;
        }

        private float getWidth() {
            return page.getMediaBox().getWidth() - margin * 2;
        }

        private void drawBox(float x, float y, float width, float height, Color color) throws IOException {
            content.setNonStrokingColor(color);
            content.addRect(x, y, width, height);
            content.fill();
        }

        private void drawBorder(float x, float y, float width, float height) throws IOException {
            content.setStrokingColor(BORDER);
            content.addRect(x, y, width, height);
            content.stroke();
        }

        private void addText(String text, float x, float y, PDType1Font font, float size, Color color) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.setNonStrokingColor(color);
            content.newLineAtOffset(x, y);
            content.showText(toPdfText(text));
            content.endText();
        }

        private void addFooter() throws IOException {
            content.setStrokingColor(BORDER);
            content.moveTo(margin, 28);
            content.lineTo(margin + getWidth(), 28);
            content.stroke();

            addText("Auto Grading System", margin, 15, normal, 7, Color.GRAY);
            addText("Page " + pageNumber, margin + getWidth() - 35, 15, normal, 7, Color.GRAY);
        }

        private Color getStatusColor(String value) {
            String status = value == null ? "" : value.toLowerCase(Locale.ROOT);

            if (status.contains("fail") || status.contains("error")) return LIGHT_RED;
            if (status.contains("review") || status.contains("partial") || status.contains("skip") || status.contains("manual")) return AMBER;
            if (status.contains("valid") || status.contains("graded") || status.contains("pass") || status.contains("corrected")) return LIGHT_GREEN;

            return LIGHT_GRAY;
        }

        // replace unsupported characters to prevent ? in pdf
        private String toPdfText(String text) {
            if (text == null) return "";

            StringBuilder safe = new StringBuilder();

            for (char c : text.replace('\n', ' ').replace('\r', ' ').toCharArray()) {
                if (c >= 32 && c <= 255) safe.append(c);
                else if (c == '\t') safe.append("    ");
                else safe.append('?');
            }

            return safe.toString();
        }

        void close() throws IOException {
            if (content != null) {
                addFooter();
                content.close();
                content = null;
            }
        }
    }
}
