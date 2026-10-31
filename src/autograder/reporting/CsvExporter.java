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

    public CsvExporter(Path template) throws IOException {
        rows = parse(Files.readString(template, StandardCharsets.UTF_8));
        if (rows.isEmpty()) throw new IOException("The scoresheet is empty.");
        List<String> header = rows.get(0);
        orgDefinedIdColumn = column(header, "OrgDefinedId");
        emailColumn = column(header, "Email");
        endOfLineColumn = column(header, "End-of-Line Indicator");
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
    
    // generate new id for new students
    private String nextOrgDefinedId() throws IOException {
        long maxId = -1;
        for (int i = 1; i < rows.size(); i++) {
            String value = rows.get(i).get(orgDefinedIdColumn).trim().replaceFirst("^#", "");
            try {
                maxId = Math.max(maxId, Long.parseLong(value));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid OrgDefinedId in scoresheet row " + (i + 1));
            }
        }
        if (maxId < 0) {
            throw new IOException("No existing OrgDefinedId found in scoresheet.");
        }
        return "#" + String.format(Locale.ROOT, "%08d", maxId + 1);
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

    // split existing fullname in template to 2 parts
    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", ""};
        }
        String [] parts = fullName.trim().split("\\s+", 2);
        String firstName = parts[0].toUpperCase();
        String lastName = "-";
        if (parts.length > 1) {
            lastName = parts[1].toUpperCase();
        }

        return new String[] {firstName, lastName};
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
            String name = username(result.getSubmission().getUsername());

            if (byUsername.put(name, result) != null) {
                throw new IOException("Duplicate submission username: " + name);
            }
        }

        List<List<String>> output = new ArrayList<>();
        output.add(new ArrayList<>(rows.get(0)));

        Set<String> matched = new HashSet<>();

        // keep existing template rows and update their scores
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = new ArrayList<>(rows.get(i));
            String name = username(row.get(usernameColumn));

            StudentResult result = byUsername.get(name);

            if (result != null) {
                matched.add(name);
                if (result.getQuestions().isEmpty()) {
                    row.set(scoreColumn, "");
                } else {
                    double maximum;

                    try {
                        maximum = Double.parseDouble(row.get(maximumColumn));
                    } catch (NumberFormatException ex) {
                        throw new IOException("Invalid denominator for " + row.get(usernameColumn)
                        );
                    }

                    if (maximum != result.getMaximum()) {
                        throw new IOException("Scoresheet denominator differs from grading maximum for " + row.get(usernameColumn));
                    }
                    row.set(scoreColumn, Double.toString(result.getTotal()));

                    // Split the existing full name if the last-name column is blank.
                    String existingLast = row.get(lastNameColumn).trim();
                    String existingFirst = row.get(firstNameColumn).trim();
                    boolean hasNoLastName = existingLast.isBlank() || existingLast.equals("_");
                    boolean hasFirstName = !existingFirst.isBlank() && !existingFirst.equals("_");

                    if (hasNoLastName && hasFirstName) {
                        String[] names = splitFullName(existingFirst);
                        row.set(firstNameColumn, names[0]);
                        row.set(lastNameColumn, names[1]);
                    }
                }
            }
            output.add(row);
        }

        // add students who are not already in the template
        String nextId = nextOrgDefinedId();
        for (Map.Entry<String, StudentResult> entry : byUsername.entrySet()) {
            String name = entry.getKey();
            StudentResult result = entry.getValue();

            if (matched.contains(name)) {
                continue;
            }

            List<String> row = new ArrayList<>(java.util.Collections.nCopies(rows.get(0).size(), ""));

            String[] names = fullname(name);

            row.set(orgDefinedIdColumn, nextId);
            long idNumber = Long.parseLong(nextId.substring(1));
            nextId = "#" + String.format(Locale.ROOT, "%08d", idNumber + 1);

            String emailUsername = name.trim().replaceFirst("^#", "");

            row.set(usernameColumn, formatUsername(name));
            row.set(lastNameColumn, names[1].isBlank() ? "-" : names[1]);
            row.set(firstNameColumn, names[0]);
            row.set(emailColumn, emailUsername + "@computing.smu.edu.sg");
            row.set(maximumColumn, "20");
            row.set(endOfLineColumn, "#");

            if (result.getQuestions().isEmpty()) {
                row.set(scoreColumn, "");
            } else {
                row.set(scoreColumn, Double.toString(result.getTotal()));
            }
            output.add(row);
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

    // handle quoted commas, escaped quotes, multiline fields and UTF-8 BOM. */
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
