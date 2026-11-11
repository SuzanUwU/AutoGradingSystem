package autograder.testgen;

// AI-assisted development using ChatGPT; reviewed and modified by the team.

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TesterGenerator {
    private final Path dataFolder;
    private final Path outputFolder;

    public TesterGenerator(Path dataFolder, Path outputFolder) {
        this.dataFolder = dataFolder;
        this.outputFolder = outputFolder;
    }

    public List<Path> generateAll() throws IOException {
        Files.createDirectories(outputFolder);
        ArrayList<Path> generated = new ArrayList<>();
        generated.add(write("Q1aTester.java", generateQ1a(readCases("Q1a.csv", 3))));
        generated.add(write("Q1bTester.java", generateQ1b(readCases("Q1b.csv", 3))));
        generated.add(write("Q2aTester.java", generateQ2a(readCases("Q2a.csv", 5))));
        generated.add(write("Q2bTester.java", generateQ2b(readCases("Q2b.csv", 5))));
        generated.add(write("Q3Tester.java", generateQ3(readCases("Q3.csv", 4))));
        return generated;
    }

    private List<String[]> readCases(String fileName, int expectedCount) throws IOException {
        Path file = dataFolder.resolve(fileName);
        if (!Files.isRegularFile(file)) throw new IOException("Missing parameter file: " + file);

        ArrayList<String[]> rows = new ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            rows.add(line.split(";", -1));
        }

        if (rows.size() != expectedCount) {
            throw new IOException(fileName + " must contain exactly " + expectedCount + " test cases, but found " + rows.size());
        }
        return rows;
    }

    private Path write(String fileName, String content) throws IOException {
        Path file = outputFolder.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private String generateQ1a(List<String[]> rows) throws IOException {
        StringBuilder code = header("Q1aTester", "Q1a", "Q1a");
        for (String[] row : rows) {
            requireColumns("Q1a.csv", row, 2);
            List<String> inputs = splitPipe(row[0]);
            List<String> expected = splitPipe(row[1]);

            code.append("            try {\n");
            code.append("                ArrayList<String> inputs = new ArrayList<>();\n");
            for (String value : inputs) code.append("                inputs.add(\"").append(escape(value)).append("\");\n");
            code.append("\n                System.out.printf(\"Test %d: getIsogramWords(%s)%n\", tcNum++, inputs);\n");
            if (expected.isEmpty()) {
                code.append("                ArrayList<String> expected = new ArrayList<>();\n");
            } else {
                code.append("                ArrayList<String> expected = new ArrayList<>(Arrays.asList(");
                for (int i = 0; i < expected.size(); i++) {
                    if (i > 0) code.append(", ");
                    code.append("\"").append(escape(expected.get(i))).append("\"");
                }
                code.append("));\n");
            }
            code.append("                ArrayList<String> result = getIsogramWords(inputs);\n");
            code.append(commonExpectedResult("expected.equals(result)"));
        }
        return footer(code);
    }

    private String generateQ1b(List<String[]> rows) throws IOException {
        StringBuilder code = header("Q1bTester", "Q1b", "Q1b");
        for (String[] row : rows) {
            requireColumns("Q1b.csv", row, 2);
            List<String> inputs = splitPipe(row[0]);
            int expected;
            try {
                expected = Integer.parseInt(row[1].trim());
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Q1b expected integer: " + row[1]);
            }

            code.append("            try {\n");
            code.append("                ArrayList<Object> inputs = new ArrayList<>();\n");
            for (String value : inputs) {
                code.append("                inputs.add(").append(toJavaObject(value)).append(");\n");
            }
            code.append("\n                System.out.printf(\"Test %d: getSumOfEvenIntegers(%s)%n\", tcNum++, inputs);\n");
            code.append("                int expected = ").append(expected).append(";\n");
            code.append("                int result = getSumOfEvenIntegers(inputs);\n");
            code.append(commonExpectedResult("expected == result"));
        }
        return footer(code);
    }

    private String generateQ2a(List<String[]> rows) throws IOException {
        StringBuilder code = header("Q2aTester", "Q2a", "Q2a");
        for (String[] row : rows) {
            requireColumns("Q2a.csv", row, 3);
            double expected;
            try {
                expected = Double.parseDouble(row[2].trim());
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Q2a expected number: " + row[2]);
            }

            code.append("            try {\n");
            code.append("                String inputs = \"").append(escape(row[0].trim())).append("\";\n");
            code.append("                String surname = \"").append(escape(row[1].trim())).append("\";\n");
            code.append("                System.out.printf(\"Test %d: getAverageAge(%s, %s)%n\", tcNum++, inputs, surname);\n");
            code.append("                double expected = ").append(expected).append(";\n");
            code.append("                double result = getAverageAge(inputs, surname);\n");
            code.append(commonExpectedResult("expected == result"));
        }
        return footer(code);
    }

    private String generateQ2b(List<String[]> rows) throws IOException {
        StringBuilder code = header("Q2bTester", "Q2b", "Q2b");
        for (String[] row : rows) {
            requireColumns("Q2b.csv", row, 3);
            String file = escape(row[0].trim());
            String course = escape(row[1].trim());
            String expected = row[2].trim();

            code.append("            try {\n");
            code.append("                String inputs = \"").append(file).append("\";\n");
            code.append("                String courseName = \"").append(course).append("\";\n");
            code.append("                System.out.printf(\"Test %d: getTopStudent(%s, %s)%n\", tcNum++, inputs, courseName);\n");

            if (expected.equals("DATA_EXCEPTION")) {
                code.append("                String result = getTopStudent(inputs, courseName);\n");
                code.append("                System.out.println(\"Failed -> Expecting a Data Exception\");\n");
                code.append("            } catch (DataException ex) {\n");
                code.append("                System.out.println(\"Passed\");\n");
                code.append("                score += 1;\n");
                code.append("            } catch (Exception e) {\n");
                code.append("                System.out.println(\"Failed -> Exception\");\n");
                code.append("                e.printStackTrace();\n");
                code.append("            }\n");
                code.append("            System.out.println(\"-------------------------------------------------------\");\n\n");
            } else {
                code.append("                String expected = \"").append(escape(expected)).append("\";\n");
                code.append("                String result = getTopStudent(inputs, courseName);\n");
                code.append(commonExpectedResult("expected.equals(result)"));
            }
        }
        return footer(code);
    }

    private String generateQ3(List<String[]> rows) throws IOException {
        StringBuilder code = header("Q3Tester", "Q3", "Q3");
        for (String[] row : rows) {
            requireColumns("Q3.csv", row, 2);
            List<String> shapes = splitPipe(row[0]);

            code.append("            try {\n");
            code.append("                ArrayList<Shape> inputs = new ArrayList<>();\n");
            for (String shape : shapes) code.append("                inputs.add(").append(toShape(shape)).append(");\n");
            code.append("\n                System.out.printf(\"Test %d: sortShapes(%s)%n\", tcNum++, inputs);\n");
            code.append("                String expected = \"").append(escape(row[1].trim())).append("\";\n");
            code.append("                List<Shape> result = sortShapes(inputs);\n");
            code.append(commonExpectedResult("expected.equals(result.toString())"));
        }
        return footer(code);
    }

    private StringBuilder header(String testerClass, String studentClass, String qn) {
        StringBuilder code = new StringBuilder();
        code.append("import java.util.*;\n\n");
        code.append("public class ").append(testerClass).append(" extends ").append(studentClass).append(" {\n\n");
        code.append("    private static double score;\n");
        code.append("    private static String qn = \"").append(qn).append("\";\n\n");
        code.append("    public static void main(String[] args) {\n");
        code.append("        grade();\n");
        code.append("        System.out.println(score);\n");
        code.append("    }\n\n");
        code.append("    public static void grade() {\n");
        code.append("        System.out.println(\"-------------------------------------------------------\");\n");
        code.append("        System.out.println(\"---------------------- \" + qn + \" ----------------------------\");\n");
        code.append("        System.out.println(\"-------------------------------------------------------\");\n\n");
        code.append("        int tcNum = 1;\n");
        code.append("        {\n");
        return code;
    }

    private String commonExpectedResult(String condition) {
        return "                System.out.printf(\"Expected  :|%s|%n\", expected);\n"
                + "                System.out.printf(\"Actual    :|%s|%n\", result);\n\n"
                + "                if (" + condition + ") {\n"
                + "                    score += 1;\n"
                + "                    System.out.println(\"Passed\");\n"
                + "                } else {\n"
                + "                    System.out.println(\"Failed\");\n"
                + "                }\n"
                + "            } catch (Exception e) {\n"
                + "                System.out.println(\"Failed -> Exception\");\n"
                + "                e.printStackTrace();\n"
                + "            }\n"
                + "            System.out.println(\"-------------------------------------------------------\");\n\n";
    }

    private String footer(StringBuilder code) {
        code.append("        }\n");
        code.append("    }\n");
        code.append("}\n");
        return code.toString();
    }

    private List<String> splitPipe(String value) {
        ArrayList<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) return result;
        for (String part : value.split("\\|", -1)) result.add(part.trim());
        return result;
    }

    private String toJavaObject(String token) throws IOException {
        String value = token.trim();
        int colon = value.indexOf(':');
        if (colon < 0) throw new IOException("Q1b typed value must look like int:2 or string:two: " + token);

        String type = value.substring(0, colon).trim().toLowerCase();
        String data = value.substring(colon + 1).trim();

        switch (type) {
            case "int":
                try { Integer.parseInt(data); } catch (NumberFormatException e) { throw new IOException("Invalid int: " + data); }
                return data;
            case "boolean":
                if (!data.equalsIgnoreCase("true") && !data.equalsIgnoreCase("false")) throw new IOException("Invalid boolean: " + data);
                return data.toLowerCase();
            case "double":
                try { Double.parseDouble(data); } catch (NumberFormatException e) { throw new IOException("Invalid double: " + data); }
                return data.contains(".") ? data : data + ".0";
            case "string":
                return "\"" + escape(data) + "\"";
            default:
                throw new IOException("Unsupported Q1b type: " + type);
        }
    }

    private String toShape(String token) throws IOException {
        String[] parts = token.trim().split(":");
        if (parts.length == 4 && parts[0].equalsIgnoreCase("R")) {
            return "new Rectangle(\"" + escape(parts[1]) + "\", " + number(parts[2]) + ", " + number(parts[3]) + ")";
        }
        if (parts.length == 3 && parts[0].equalsIgnoreCase("C")) {
            return "new Circle(\"" + escape(parts[1]) + "\", " + number(parts[2]) + ")";
        }
        throw new IOException("Invalid Q3 shape: " + token);
    }

    private String number(String value) throws IOException {
        try {
            Double.parseDouble(value.trim());
            return value.trim();
        } catch (NumberFormatException e) {
            throw new IOException("Invalid numeric shape value: " + value);
        }
    }

    private void requireColumns(String fileName, String[] row, int count) throws IOException {
        if (row.length != count) {
            throw new IOException("Invalid row in " + fileName + ": expected " + count + " columns separated by ';'");
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }
}
