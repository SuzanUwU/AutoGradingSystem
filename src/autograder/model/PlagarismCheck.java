package autograder.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public final class PlagarismCheck {
    static final double SUSPECTED_THRESHOLD = 80.0;
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_TOKENS = 5000;
    private static final Set<String> JAVA_ANSWERS =
            Set.of("q1a.java", "q1b.java", "q2a.java", "q2b.java", "q3.java");
    private static final List<String> OPERATORS = List.of(
            ">>>=", "<<=", ">>=", ">>>", "==", "!=", "<=", ">=", "++", "--",
            "+=", "-=", "*=", "/=", "%=", "&&", "||", "<<", ">>", "->", "::",
            "&=", "|=", "^=", "...");

    public static final class Result {
        private boolean compared;
        private boolean suspected;
        private boolean failed;
        private final List<String> details = new ArrayList<>();

        public String getStatus() {
            if (suspected) return "Suspected - review";
            if (failed) return "Could not check";
            return compared ? "No match found" : "Not compared";
        }

        public String getDetails() {
            String text = String.join("\n", details);
            if (!compared && !suspected && !failed) {
                text += (text.isEmpty() ? "" : "\n")
                        + "No comparable answer body from another student was available.";
            }
            return text;
        }
    }

    private static final class Answer {
        final String student;
        final String question;
        final List<String> tokens;

        Answer(String student, String question, List<String> tokens) {
            this.student = student;
            this.question = question;
            this.tokens = tokens;
        }
    }

    /** Compare matching Java questions using original ZIP contents. */
    public Map<String, Result> check(Path submissions) throws IOException {
        Map<String, Result> results = new LinkedHashMap<>();
        Map<String, List<Answer>> byQuestion = new LinkedHashMap<>();
        List<Path> archives;
        try (Stream<Path> paths = Files.list(submissions)) {
            archives = paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted().toList();
        }
        Set<String> duplicateUsers = new HashSet<>();
        for (Path archive : archives) {
            String user = username(archive);
            if (results.containsKey(user)) duplicateUsers.add(user);
            results.putIfAbsent(user, new Result());
        }
        for (Path archive : archives) {
            String user = username(archive);
            Result result = results.get(user);
            if (duplicateUsers.contains(user)) {
                fail(result, "Multiple ZIP files identify this student; comparison skipped.");
                continue;
            }
            try {
                List<Answer> answers = readAnswers(archive, user);
                if (answers.isEmpty()) fail(result, "No supported question answer files found.");
                for (Answer answer : answers) {
                    if (answer.tokens.isEmpty()) {
                        result.details.add(answer.question
                                + ": skipped (missing answer method or empty/starter implementation).");
                        continue;
                    }
                    byQuestion.computeIfAbsent(answer.question, key -> new ArrayList<>()).add(answer);
                }
            } catch (IOException ex) {
                fail(result, archive.getFileName() + ": " + ex.getMessage());
            }
        }
        for (List<Answer> answers : byQuestion.values()) {
            for (int i = 0; i < answers.size(); i++) {
                Answer first = answers.get(i);
                for (int j = i + 1; j < answers.size(); j++) {
                    Answer second = answers.get(j);
                    Result a = results.get(first.student), b = results.get(second.student);
                    if (first.tokens.isEmpty() || second.tokens.isEmpty()) continue;
                    a.compared = b.compared = true;
                    double percentage = similarity(first.tokens, second.tokens);
                    if (percentage < SUSPECTED_THRESHOLD) continue;
                    a.suspected = b.suspected = true;
                    String reason = String.format(Locale.ROOT, "%s: %.3f%% code similarity",
                            first.question, percentage);
                    a.details.add(reason + "; matches " + second.student);
                    b.details.add(reason + "; matches " + first.student);
                }
            }
        }
        return Collections.unmodifiableMap(results);
    }

    private static void fail(Result result, String message) {
        result.failed = true;
        if (!result.details.contains(message)) result.details.add(message);
    }

    private static String username(Path zip) {
        String name = zip.getFileName().toString().replaceFirst("(?i)\\.zip$", "");
        return name.replaceFirst("^\\d{4}-\\d{4}-", "");
    }

    private static List<Answer> readAnswers(Path archive, String user) throws IOException {
        List<Answer> answers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > 2000) throw new IOException("Too many ZIP entries.");
                String path = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || path.startsWith("__MACOSX/")
                        || path.contains("/__MACOSX/")) continue;
                String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
                // Only actual question files are considered: helpers and testers are excluded.
                if (!JAVA_ANSWERS.contains(name)) continue;
                if (!seen.add(name)) throw new IOException("Duplicate question file: " + name);
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) {
                    bytes = input.readNBytes(MAX_FILE_BYTES + 1);
                }
                if (bytes.length > MAX_FILE_BYTES) throw new IOException("Oversized answer: " + name);
                String source = new String(bytes, StandardCharsets.UTF_8);
                if (tokenize(source).size() > MAX_TOKENS)
                    throw new IOException("Answer too large to compare: " + name);
                answers.add(new Answer(user, name, answerTokens(source, name)));
            }
        }
        return answers;
    }

    private static final Map<String, String> JAVA_METHODS = Map.of(
            "q1a.java", "getIsogramWords", "q1b.java", "getSumOfEvenIntegers",
            "q2a.java", "getAverageAge", "q2b.java", "getTopStudent", "q3.java", "sortShapes");
    static List<String> answerTokens(String source, String question) throws IOException {
        String method = JAVA_METHODS.get(question);
        if (method == null) return List.of();
        List<String> body = javaBody(source, question, method);
        return isStarterBody(body) ? List.of() : body;
    }

    private static List<String> javaBody(String source, String filename, String method) throws IOException {
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("A JDK is required to inspect Java answer methods.");
        var input = new javax.tools.SimpleJavaFileObject(
                java.net.URI.create("string:///" + filename), javax.tools.JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        try (var manager = compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            var task = (com.sun.source.util.JavacTask) compiler.getTask(null, manager,
                    diagnostic -> { }, List.of("-proc:none"), null, List.of(input));
            var positions = com.sun.source.util.Trees.instance(task).getSourcePositions();
            List<String> result = new ArrayList<>();
            for (var unit : task.parse()) {
                new com.sun.source.util.TreeScanner<Void, Void>() {
                    @Override public Void visitMethod(com.sun.source.tree.MethodTree tree, Void ignored) {
                        if (!tree.getName().contentEquals(method) || tree.getBody() == null) return null;
                        long start = positions.getStartPosition(unit, tree.getBody());
                        long end = positions.getEndPosition(unit, tree.getBody());
                        if (start >= 0 && end > start && end <= source.length()) {
                            String block = source.substring((int) start, (int) end);
                            List<String> tokens = tokenize(block);
                            if (!tokens.isEmpty() && tokens.get(0).equals("{")) tokens.remove(0);
                            if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals("}"))
                                tokens.remove(tokens.size() - 1);
                            result.addAll(tokens);
                        }
                        return null;
                    }
                }.scan(unit, null);
            }
            return result;
        }
    }

    private static boolean isStarterBody(List<String> body) {
        if (body.isEmpty()) return true;
        String code = String.join("", body);
        return Set.of("return;", "returnnull;", "return0;", "return0.0;",
                "returnfalse;", "return\"\";", "thrownewUnsupportedOperationException();").contains(code)
                || code.matches("returnnew(?:ArrayList|LinkedList|HashSet|HashMap)(?:<[^;]*>)?\\(\\);");
    }

    static double similarity(List<String> a, List<String> b) {
        int longest = Math.max(a.size(), b.size());
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.equals(b)) return 100.0;
        if (b.size() > a.size()) {
            List<String> swap = a;
            a = b;
            b = swap;
        }
        int[] distance = new int[b.size() + 1];
        for (int j = 0; j <= b.size(); j++) distance[j] = j;
        for (int i = 1; i <= a.size(); i++) {
            int diagonal = distance[0];
            distance[0] = i;
            for (int j = 1; j <= b.size(); j++) {
                int previous = distance[j];
                int replacement = diagonal + (a.get(i - 1).equals(b.get(j - 1)) ? 0 : 1);
                distance[j] = Math.min(replacement,
                        Math.min(distance[j] + 1, distance[j - 1] + 1));
                diagonal = previous;
            }
        }
        return 100.0 * (longest - distance[b.size()]) / longest;
    }

    // Comments/whitespace are ignored outside strings; names and literal contents are preserved.
    static List<String> tokenize(String source) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (Character.isWhitespace(c) || c == '\uFEFF') { i++; continue; }
            if (source.startsWith("//", i)) {
                while (i < source.length() && source.charAt(i) != '\n'
                        && source.charAt(i) != '\r') i++;
                continue;
            }
            if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
                continue;
            }
            int start = i;
            if (c == '"' || c == '\'') {
                String triple = String.valueOf(c).repeat(3);
                boolean multiline = source.startsWith(triple, i) && c == '"';
                int width = multiline ? 3 : 1;
                i += width;
                while (i < source.length()) {
                    if (source.charAt(i) == '\\') { i = Math.min(source.length(), i + 2); continue; }
                    if (multiline ? source.startsWith(triple, i) : source.charAt(i) == c) {
                        i += width;
                        break;
                    }
                    i++;
                }
            } else if (Character.isJavaIdentifierPart(c)) {
                while (i < source.length() && Character.isJavaIdentifierPart(source.charAt(i))) i++;
            } else {
                String operator = null;
                for (String candidate : OPERATORS) {
                    if (source.startsWith(candidate, i)) { operator = candidate; break; }
                }
                i += operator == null ? 1 : operator.length();
            }
            tokens.add(source.substring(start, i));
        }
        return tokens;
    }
}
