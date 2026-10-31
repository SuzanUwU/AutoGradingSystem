package autograder.reporting;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;
import autograder.model.SubmissionResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class PdfReportExporter {
    private static final float margin = 50;
    private static final float fontSize = 10;
    private static final float lineHeight = 14;
    private static final float bottomMargin = 50;
    private static final PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);

    public void export(Path reportFile, List<StudentResult> students) throws IOException {
        if (reportFile == null) {
            throw new IOException("The PDF report path cannot be empty.");
        }
        if (students == null) {
            throw new IOException("Student results cannot be null.");
        }

        Path absolutePath = reportFile.toAbsolutePath();

        if (absolutePath.getParent() != null) {
            Files.createDirectories(absolutePath.getParent());
        }

        // create pdf
        try (PDDocument document = new PDDocument()) {
            ReportWriter writer = new ReportWriter(document);
            for (int i = 0; i < students.size(); i++) {
                if (i > 0) {
                    writer.newPage();
                }
                addStudentReport(writer, students.get(i));
            }
            writer.close();
            document.save(absolutePath.toFile());
        }
    }

    private void addStudentReport(ReportWriter writer,StudentResult student) throws IOException {
        SubmissionResult submission = student.getSubmission();

        writer.addLine("============================================================");
        writer.addLine("STUDENT: " + formatUsername(submission.getUsername()));
        writer.addLine("============================================================");
        writer.addBlankLine();

        writer.addLine("QUESTION SCORES:");
        writer.addBlankLine();

        addQuestion(writer, student, "Q1a");
        addQuestion(writer, student, "Q1b");
        addQuestion(writer, student, "Q2a");
        addQuestion(writer, student, "Q2b");
        addQuestion(writer, student, "Q3");

        writer.addLine("------------------------------------------------------------");

        if (student.getQuestions().isEmpty()) {
            writer.addLine("TOTAL SCORE: Not available");
        } else {
            writer.addLine(String.format(Locale.ROOT, "TOTAL SCORE: %.1f / %.1f", student.getTotal(), student.getMaximum()));
        }

        writer.addLine("Grading Status: " + student.getGradingStatus());
        writer.addLine("Validation Status: " + submission.getOverallStatus());
        writer.addBlankLine();

        writer.addLine("SUBMISSION ISSUES:");
        addIssues(writer, submission.getSubmissionIssues());

        writer.addBlankLine();
        writer.addLine("Q1 ISSUES:");
        addIssues(writer, submission.getQ1Issues());

        writer.addBlankLine();
        writer.addLine("Q2 ISSUES:");
        addIssues(writer, submission.getQ2Issues());

        writer.addBlankLine();
        writer.addLine("Q3 ISSUES:");
        addIssues(writer, submission.getQ3Issues());
    }

    private void addQuestion(ReportWriter writer, StudentResult student, String questionId) throws IOException {
        QuestionResult question = findQuestion(student, questionId);
        writer.addLine(questionId);

        if (question == null) {
            writer.addLine("  Score:          Not available");
            writer.addLine("  Manual Mark:    -");
            writer.addLine("  Grading Status: No result");
            writer.addBlankLine();
            return;
        }

        double finalScore = student.getFinalScore(question);
        writer.addLine(String.format(Locale.ROOT, "  Score:          %.1f / %.1f", finalScore, question.getMaxScore()));

        if (student.hasManualScore(questionId)) {
            writer.addLine(String.format(Locale.ROOT, "  Manual Mark:    %.1f", finalScore));
        } else {
            writer.addLine("  Manual Mark:    -");
        }

        writer.addWrappedLine("  Grading Status: " + question.getStatusMessage());

        String comment = student.getComment(questionId);
        if (comment != null && !comment.isBlank()) {
            writer.addWrappedLine("  Comments:       " + comment.trim());
        }

        writer.addBlankLine();
    }

    private QuestionResult findQuestion(StudentResult student, String questionId) {
        for (QuestionResult question : student.getQuestions()) {
            if (question.getQuestionId().equals(questionId)) {
                return question;
            }
        }
        return null;
    }

    public void addIssues(ReportWriter writer, List<String> issues) throws IOException {
        if (issues == null || issues.isEmpty()) {
            writer.addLine("  - None");
            return;
        }
        for (String issue : issues) {
            writer.addWrappedLine("  - " + issue);
        }
    }

    // format username to have # infront
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

    // rites lines and create pages
    private static class ReportWriter {
        private final PDDocument document;
        private PDPageContentStream content;
        private float y;
        private float maxTextWidth;

        ReportWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        private void ensureSpace(float requiredHeight) throws IOException {
            if (y - requiredHeight < bottomMargin) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            content = new PDPageContentStream(document, page);

            y = page.getMediaBox().getHeight() - margin;
            maxTextWidth = page.getMediaBox().getWidth() - (2 * margin);
        }

        private void addLine(String text) throws IOException {
            ensureSpace(lineHeight);

            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(margin, y);
            content.showText(toPdfText(text));
            content.endText();

            y -= lineHeight;
        }

        private void addBlankLine() throws IOException {
            y -= lineHeight / 2;

            if (y < bottomMargin) {
                newPage();
            }
        }

        private void addWrappedLine(String text) throws IOException {
            if (text == null || text.isEmpty()) {
                addLine("");
                return;
            }
            int maxChars = (int)(maxTextWidth / (fontSize * 0.6f));
            String remaining = text;
            while (!remaining.isEmpty()) {
                int breakAt = Math.min(remaining.length(), maxChars);
                if (breakAt < remaining.length()) {
                    int space = remaining.lastIndexOf(' ', breakAt);
                    if (space > 0) {
                        breakAt = space;
                    }
                }
                
                addLine(remaining.substring(0, breakAt));
                remaining = remaining.substring(breakAt).stripLeading();
            }
        }

        private String toPdfText(String text) {
            StringBuilder safe = new StringBuilder();
            for (char c : text.toCharArray()) {
                // replace unsupported characters to prevent ? in pdf
                if (c >= 32 && c <= 255) {
                    safe.append(c);
                } else if (c == '\t') {
                    safe.append("    ");
                } else {
                    safe.append('?');
                }
            }
            return safe.toString();
        }

        void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }
    }
}
