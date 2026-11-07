package autograder.reporting;

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

// write a csv file with the total marks for each student
public class CsvExporter {
    // scoresheet template rows and the columns we read or fill in
    private final List<List<String>> rows;
    private final int orgDefinedIdColumn;
    private final int emailColumn;
    private final int endOfLineColumn;
    private final int usernameColumn;
    private final int scoreColumn;
    private final int maximumColumn;
    private final int firstNameColumn;
    private final int lastNameColumn;

    /** Creates the scoresheet without requiring a template file. */
    public CsvExporter() {
        rows = new ArrayList<>();
        rows.add(List.of("OrgDefinedId", "Username", "First Name", "Last Name", "Email",
                "Calculated Final Grade Numerator", "Calculated Final Grade Denominator",
                "End-of-Line Indicator"));
        orgDefinedIdColumn = 0;
        usernameColumn = 1;
        firstNameColumn = 2;
        lastNameColumn = 3;
        emailColumn = 4;
        scoreColumn = 5;
        maximumColumn = 6;
        endOfLineColumn = 7;
    }
    
    private String nextOrgDefinedId(int studentNumber) {
        return "#" + String.format(Locale.ROOT, "%08d", studentNumber);
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

        return new String[]{firstName.toUpperCase(), lastName.toUpperCase()};
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
            if (result == null || result.getSubmission() == null) {
                throw new IOException("Invalid student result.");
            }

            String name = result.getSubmission().getUsername();

            if (name == null || name.isBlank()) {
                throw new IOException("Student username cannot be empty.");
            }

            if (byUsername.put(name, result) != null) {
                throw new IOException("Duplicate submission username: " + name);
            }
        }

        List<List<String>> output = new ArrayList<>();
        output.add(new ArrayList<>(rows.get(0)));

        int studentNumber = 1;
        for (Map.Entry<String, StudentResult> entry : byUsername.entrySet()) {
            String username = entry.getKey();
            StudentResult result = entry.getValue();

            List<String> row = new ArrayList<>(java.util.Collections.nCopies(rows.get(0).size(), ""));

            // generate student ID and format details
            String[] names = fullname(username);

            row.set(orgDefinedIdColumn, nextOrgDefinedId(studentNumber));
            row.set(usernameColumn, formatUsername(username));
            row.set(lastNameColumn, names[1].isBlank() ? "-" : names[1]);
            row.set(firstNameColumn, names[0]);
            row.set(emailColumn, username + "@computing.smu.edu.sg");
            row.set(maximumColumn, result.getQuestions().isEmpty()
                    ? "" : Double.toString(result.getMaximum()));
            row.set(endOfLineColumn, "#");

            if (result.getQuestions().isEmpty()) {
                row.set(scoreColumn, "");
            } else {
                row.set(scoreColumn, Double.toString(result.getTotal()));
            }
            output.add(row);
            studentNumber++;
        }
        return encode(output);
    }

    public void export(Path scoresFile, List<StudentResult> results) throws IOException {
        if (scoresFile == null) {
            throw new IOException("The output file paths cannot be empty.");
        }

        if (results == null) {
            throw new IOException("Student results cannot be null");
        }

        String scoresCsv = buildScoresCsv(results);

        if (scoresFile.toAbsolutePath().getParent() != null) {
            Files.createDirectories(scoresFile.toAbsolutePath().getParent());
        }

        Files.writeString(scoresFile, scoresCsv, StandardCharsets.UTF_8);
    }
}
