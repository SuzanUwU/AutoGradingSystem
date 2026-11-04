package autograder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class EdgeCaseRunner {
    private final File edgeFolder;
    private final File resourcesFolder;

    public EdgeCaseRunner(File edgeFolder, File resourcesFolder) {
        this.edgeFolder = edgeFolder;
        this.resourcesFolder = resourcesFolder;
    }

    public Result run(File studentRoot, String username) {
        LinkedHashMap<String, QuestionResult> results = new LinkedHashMap<>();
        Path temp = null;

        try {
            temp = Files.createTempDirectory("cs102-edge-" + safe(username) + "-");

            results.put("Q1a", runQuestion(studentRoot, temp, "Q1a", "Q1", "Q1aEdgeCase",
                    List.of("Q1a.java"), List.of(), 7, false));
            results.put("Q1b", runQuestion(studentRoot, temp, "Q1b", "Q1", "Q1bEdgeCase",
                    List.of("Q1b.java"), List.of(), 9, false));
            results.put("Q2a", runQuestion(studentRoot, temp, "Q2a", "Q2", "Q2aEdgeCase",
                    List.of("Q2a.java"), List.of("personsEdgeCase.txt"), 7, false));
            results.put("Q2b", runQuestion(studentRoot, temp, "Q2b", "Q2", "Q2bEdgeCase",
                    List.of("Q2b.java"), List.of("studentsEdgeCase.txt"), 6, false));
            results.put("Q3", runQuestion(studentRoot, temp, "Q3", "Q3", "Q3EdgeCase",
                    List.of(), List.of(), 16, true));

            return new Result(username, results, "");
        } catch (Exception e) {
            return skipped(username, e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            if (temp != null) {
                try { deleteTree(temp); } catch (IOException ignored) {}
            }
        }
    }

    public static Result skipped(String username, String reason) {
        return new Result(username, new LinkedHashMap<>(), reason == null ? "Skipped" : reason);
    }

    private QuestionResult runQuestion(File studentRoot, Path tempRoot, String qid, String folderName,
            String edgeClass, List<String> requiredSources, List<String> edgeData,
            int expectedTests, boolean copyAllJava) throws Exception {

        Path work = tempRoot.resolve(qid);
        Files.createDirectories(work);

        File questionFolder = findQuestionFolder(studentRoot, folderName);
        if (questionFolder == null) return new QuestionResult(qid, 0, expectedTests, "Missing " + folderName + " folder");

        if (copyAllJava) {
            copyJavaFiles(questionFolder.toPath(), work);
        } else {
            for (String name : requiredSources) {
                Path source = questionFolder.toPath().resolve(name);
                if (!Files.isRegularFile(source)) return new QuestionResult(qid, 0, expectedTests, "Missing " + name);
                Files.copy(source, work.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        copyResources(new File(resourcesFolder, folderName).toPath(), work);

        Path edgeSource = edgeFolder.toPath().resolve(edgeClass + ".java");
        if (!Files.isRegularFile(edgeSource)) return new QuestionResult(qid, 0, expectedTests, "Missing " + edgeSource);
        Files.copy(edgeSource, work.resolve(edgeSource.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        for (String name : edgeData) {
            Path data = edgeFolder.toPath().resolve(name);
            if (!Files.isRegularFile(data)) return new QuestionResult(qid, 0, expectedTests, "Missing " + data);
            Files.copy(data, work.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }

        ArrayList<String> javaFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(work)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java"))
                    .forEach(p -> javaFiles.add(p.getFileName().toString()));
        }

        ArrayList<String> compileCommand = new ArrayList<>(List.of("javac", "-cp", ".", "-d", "."));
        compileCommand.addAll(javaFiles);

        ProcessResult compile = execute(compileCommand, work, 10);
        if (compile.timedOut) return new QuestionResult(qid, 0, expectedTests, "Compilation timed out");
        if (compile.exitCode != 0) return new QuestionResult(qid, 0, expectedTests, "Compilation failed");

        ProcessResult execution = execute(List.of("java", "-cp", ".", edgeClass), work, 8);
        int passed = Math.min(count(execution.output, "PASSED"), expectedTests);

        String status = execution.timedOut ? "Timed out"
                : execution.exitCode == 0 ? "Completed"
                : "Exited with error";

        return new QuestionResult(qid, passed, expectedTests, status);
    }

    private File findQuestionFolder(File root, String name) {
        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null) return null;
        for (File folder : folders) if (folder.getName().equalsIgnoreCase(name)) return folder;
        return null;
    }

    private void copyJavaFiles(Path source, Path destination) throws IOException {
        try (Stream<Path> stream = Files.list(source)) {
            for (Path path : (Iterable<Path>) stream
                    .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".java"))::iterator) {
                Files.copy(path, destination.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void copyResources(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source)) return;

        try (Stream<Path> stream = Files.list(source)) {
            for (Path path : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                Files.copy(path, destination.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private ProcessResult execute(List<String> command, Path work, int timeoutSeconds) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(work.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Thread reader = new Thread(() -> {
            try (InputStream input = process.getInputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                int total = 0;

                while ((read = input.read(buffer)) != -1 && total < 1_000_000) {
                    int keep = Math.min(read, 1_000_000 - total);
                    output.write(buffer, 0, keep);
                    total += keep;
                }
            } catch (IOException ignored) {}
        });

        reader.start();

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();

        reader.join(1000);

        return new ProcessResult(
                finished ? process.exitValue() : -1,
                output.toString(StandardCharsets.UTF_8),
                !finished);
    }

    private int count(String text, String word) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(word, index)) >= 0) {
            count++;
            index += word.length();
        }

        return count;
    }

    private String safe(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;

        try (Stream<Path> stream = Files.walk(path)) {
            for (Path entry : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        }
    }

    public static class Result {
        private final String username;
        private final LinkedHashMap<String, QuestionResult> questions;
        private final String error;

        Result(String username, LinkedHashMap<String, QuestionResult> questions, String error) {
            this.username = username;
            this.questions = questions;
            this.error = error == null ? "" : error;
        }

        public String getUsername() { return username; }
        public String getError() { return error; }
        public boolean isSkipped() { return !error.isEmpty(); }
        public QuestionResult get(String qid) { return questions.get(qid); }
        public int getPassed() { return questions.values().stream().mapToInt(QuestionResult::getPassed).sum(); }
        public int getTotal() { return questions.values().stream().mapToInt(QuestionResult::getTotal).sum(); }
        public int getFailed() { return getTotal() - getPassed(); }
        public String getSummary() { return isSkipped() ? "Skipped: " + error : getPassed() + "/" + getTotal() + " passed"; }
    }

    public static class QuestionResult {
        private final String questionId;
        private final int passed;
        private final int total;
        private final String status;

        QuestionResult(String questionId, int passed, int total, String status) {
            this.questionId = questionId;
            this.passed = passed;
            this.total = total;
            this.status = status;
        }

        public String getQuestionId() { return questionId; }
        public int getPassed() { return passed; }
        public int getTotal() { return total; }
        public int getFailed() { return total - passed; }
        public String getStatus() { return status; }
        public String getScoreText() { return passed + "/" + total; }
    }

    private static class ProcessResult {
        final int exitCode;
        final String output;
        final boolean timedOut;

        ProcessResult(int exitCode, String output, boolean timedOut) {
            this.exitCode = exitCode;
            this.output = output;
            this.timedOut = timedOut;
        }
    }
}
