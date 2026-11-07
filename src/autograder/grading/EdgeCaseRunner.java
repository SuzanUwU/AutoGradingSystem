package autograder.grading;

import autograder.compilation.JavaCompilerService;
import autograder.compilation.QuestionWorkspace;
import autograder.compilation.WorkspaceManager;
import autograder.execution.ProgramExecutor;
import autograder.model.CompilationResult;
import autograder.model.ExecutionResult;
import autograder.model.QuestionSpec;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Runs the optional edge-case testers (edgecases/QxxEdgeCase.java) against one student.
 *
 * <p>Workspace preparation, compilation and execution reuse the main grading pipeline
 * ({@link WorkspaceManager}, {@link JavaCompilerService}, {@link ProgramExecutor}); only the
 * edge-case specifics live here: the extra data files, and counting "PASSED" lines instead of
 * the official score format. Results never touch official grades.
 */
public class EdgeCaseRunner {
    private static final long COMPILE_TIMEOUT_SECONDS = 10;
    private static final long RUN_TIMEOUT_SECONDS = 8;
    private static final int MAX_OUTPUT_BYTES = 1_000_000;

    private final File edgeFolder;
    private final File resourcesFolder;
    private final JavaCompilerService compiler = new JavaCompilerService(COMPILE_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES);
    private final ProgramExecutor executor = new ProgramExecutor(RUN_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES, List.of());

    public EdgeCaseRunner(File edgeFolder, File resourcesFolder) {
        this.edgeFolder = edgeFolder;
        this.resourcesFolder = resourcesFolder;
    }

    public Result run(File studentRoot, String username) {
        LinkedHashMap<String, QuestionResult> results = new LinkedHashMap<>();
        Path temp = null;

        try {
            temp = Files.createTempDirectory("cs102-edge-" + safe(username) + "-");
            // Edge-case testers are the "testers" here, so the official WorkspaceManager places them.
            WorkspaceManager workspaces = new WorkspaceManager(temp.toFile(), edgeFolder, resourcesFolder);

            results.put("Q1a", runQuestion(workspaces, studentRoot, username,
                    new QuestionSpec("Q1a", "Q1", "Q1aEdgeCase", List.of("Q1a.java")), List.of(), 7, false));
            results.put("Q1b", runQuestion(workspaces, studentRoot, username,
                    new QuestionSpec("Q1b", "Q1", "Q1bEdgeCase", List.of("Q1b.java")), List.of(), 9, false));
            results.put("Q2a", runQuestion(workspaces, studentRoot, username,
                    new QuestionSpec("Q2a", "Q2", "Q2aEdgeCase", List.of("Q2a.java")), List.of("personsEdgeCase.txt"), 7, false));
            results.put("Q2b", runQuestion(workspaces, studentRoot, username,
                    new QuestionSpec("Q2b", "Q2", "Q2bEdgeCase", List.of("Q2b.java")), List.of("studentsEdgeCase.txt"), 6, false));
            // Q3 compiles every student file, as before, so helper classes in any file are found.
            results.put("Q3", runQuestion(workspaces, studentRoot, username,
                    new QuestionSpec("Q3", "Q3", "Q3EdgeCase", List.of()), List.of(), 16, true));

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

    private QuestionResult runQuestion(WorkspaceManager workspaces, File studentRoot, String username,
            QuestionSpec question, List<String> edgeData, int expectedTests, boolean compileAllSources) throws IOException {
        String qid = question.getQuestionId();

        if (findQuestionFolder(studentRoot, question.getFolderName()) == null) {
            return new QuestionResult(qid, 0, expectedTests, "Missing " + question.getFolderName() + " folder");
        }
        File edgeSource = new File(edgeFolder, question.getTesterSourceFile());
        if (!edgeSource.isFile()) return new QuestionResult(qid, 0, expectedTests, "Missing " + edgeSource);

        QuestionWorkspace workspace = workspaces.prepare(studentRoot, username, question);

        // Edge-case data files are not part of the official resources, so copy them alongside.
        for (String name : edgeData) {
            Path data = edgeFolder.toPath().resolve(name);
            if (!Files.isRegularFile(data)) return new QuestionResult(qid, 0, expectedTests, "Missing " + data);
            Files.copy(data, workspace.getDirectory().toPath().resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }

        CompilationResult compilation = compiler.compile(workspace, question, compileAllSources);
        switch (compilation.getStatus()) {
            case SUCCESS:
                break;
            case MISSING_SOURCES:
                return new QuestionResult(qid, 0, expectedTests, "Missing " + String.join(", ", compilation.getMissingSources()));
            case SYSTEM_ERROR:
                boolean timedOut = compilation.getOutput().contains("(" + ExecutionResult.Status.TIMED_OUT + ")");
                return new QuestionResult(qid, 0, expectedTests, timedOut ? "Compilation timed out" : "Compilation failed");
            default:
                return new QuestionResult(qid, 0, expectedTests, "Compilation failed");
        }

        ExecutionResult execution = executor.runTester(workspace, question);
        int passed = Math.min(countPassedLines(execution.getStdout()), expectedTests);
        return new QuestionResult(qid, passed, expectedTests, describe(execution));
    }

    private String describe(ExecutionResult execution) {
        switch (execution.getStatus()) {
            case COMPLETED: return "Completed";
            case TIMED_OUT: return "Timed out";
            case EXITED_WITH_ERROR: return "Exited with error";
            default: return "Could not run: " + execution.getMessage();
        }
    }

    /**
     * Counts tester verdict lines: "PASSED" on its own, or "PASSED, ..." (Q2b's exception checks).
     * Whole lines only, so "PASSED" inside other text (e.g. a student's print) is not counted.
     */
    private int countPassedLines(String output) {
        int count = 0;
        for (String line : output.split("\\R")) {
            String verdict = line.trim();
            if (verdict.equals("PASSED") || verdict.startsWith("PASSED,")) count++;
        }
        return count;
    }

    private File findQuestionFolder(File root, String name) {
        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null) return null;
        for (File folder : folders) if (folder.getName().equalsIgnoreCase(name)) return folder;
        return null;
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
}
