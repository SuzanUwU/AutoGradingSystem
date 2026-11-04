package autograder.reporting;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DetailedCsvExporter {

    public void export(Path target, List<StudentResult> students)
            throws IOException {

        // Collect question IDs and maximum marks dynamically.
        Map<String, Double> questions = new LinkedHashMap<>();

        for (StudentResult student : students) {
            for (QuestionResult question : student.getQuestions()) {
                questions.putIfAbsent(
                    question.getQuestionId(),
                    question.getMaxScore()
                );
            }
        }

        List<String> lines = new ArrayList<>();

        List<String> header = new ArrayList<>();
        header.add("Student");
        header.add("Validation Status");
        header.add("Grading Status");

        for (Map.Entry<String, Double> entry : questions.entrySet()) {
            header.add(entry.getKey() + " (/" + entry.getValue() + ")");
        }

        header.add("Total");
        header.add("Maximum");

        lines.add(toCsvLine(header));

        for (StudentResult student : students) {

            List<String> row = new ArrayList<>();

            row.add(student.getSubmission().getUsername());
            row.add(student.getSubmission().getOverallStatus());
            row.add(student.getGradingStatus());

            Map<String, QuestionResult> byId = new HashMap<>();

            for (QuestionResult question : student.getQuestions()) {
                byId.put(question.getQuestionId(), question);
            }

            for (String questionId : questions.keySet()) {
                QuestionResult question = byId.get(questionId);

                row.add(question == null
                    ? ""
                    : Double.toString(student.getFinalScore(question)));
            }

            row.add(student.getQuestions().isEmpty()
                ? ""
                : Double.toString(student.getTotal()));

            row.add(student.getQuestions().isEmpty()
                ? ""
                : Double.toString(student.getMaximum()));

            lines.add(toCsvLine(row));
        }

        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(target, lines, StandardCharsets.UTF_8);
    }

    // Escape commas, quotation marks, and newlines.
    private String toCsvLine(List<String> values) {
        List<String> escaped = new ArrayList<>();

        for (String value : values) {
            if (value == null) value = "";

            escaped.add(
                "\"" + value.replace("\"", "\"\"") + "\""
            );
        }

        return String.join(",", escaped);
    }
}