
package autograder.reporting;

import autograder.model.QuestionResult;
import autograder.model.StudentResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvExporter {
    // header for final grades
    private static final List<String> finalHeaders = Arrays.asList(
        "ID", "Username", "Last Name", "First Name", "Email", 
        "Calculated Final Grade Numerator", 
        "Calculated Final Grade Denominator");
    
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

    // split username to first and last name
    private String[] fullname(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new String[]{"", ""};
        }
        
        username = username.trim();

        while (username.startsWith("#")) {
            username = username.substring(1);
        }

        // remove year
        String[] parts = username.split("\\.");
        if (parts.length > 0 && parts[parts.length-1].matches("\\d{4}")) {
            parts = Arrays.copyOf(parts, parts.length - 1);
        }

        String firstName = "";
        String lastName = "";
        if (parts.length > 0) {
            firstName = parts[0];
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
        }
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                if (!lastName.isEmpty()) {
                    lastName = lastName + " ";
                }
                lastName = lastName + parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1).toLowerCase();
            }
        }

        return new String[]{firstName, lastName};
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

    // final grade csv
    private String buildScoresCsv(List<StudentResult> results) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(finalHeaders));
        int id = 0;
        
        for (StudentResult student : results) {
            id++;
            // unformatted username
            String username = student.getSubmission().getUsername();
            String[] names = fullname(username);
            
            String email = username.trim();
            while (email.startsWith("#")) {
                email = email.substring(1);
            }
            email = email + "@computing.smu.edu.sg";
            username = formatUsername(username);

            List<String> row = new ArrayList<>();
            row.add(Integer.toString(id));
            row.add(username);
            row.add(names[1]);
            row.add(names[0]);
            row.add(email);

            if (student.getQuestions().isEmpty()) {
                row.add("");
                row.add("");
            } else {
                row.add(Double.toString(student.getTotal()));
                row.add(Double.toString(student.getMaximum()));
            }
            rows.add(row);
        }
        return encode(rows);
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

}