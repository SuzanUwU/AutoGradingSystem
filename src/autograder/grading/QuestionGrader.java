package autograder.grading;

import autograder.compilation.JavaCompilerService;
import autograder.compilation.QuestionWorkspace;
import autograder.compilation.WorkspaceManager;
import autograder.model.CompilationResult;
import autograder.model.ExecutionResult;
import autograder.model.QuestionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestionGrader {
    private static final Pattern COMPILER_ERROR_PATTERN =
            Pattern.compile("(?m)^(.+?):(\\d+):\\s*error:\\s*(.+)$");
    private final JavaCompilerService compiler;
    private final TestCaseRunner testRunner;
    private final WorkspaceManager workspaceManager;

    public QuestionGrader(JavaCompilerService compiler, TestCaseRunner testRunner, WorkspaceManager workspaceManager) {
        this.compiler = compiler;
        this.testRunner = testRunner;
        this.workspaceManager = workspaceManager;
    }

    /**
     * Grades a single question part for a given student.
     *
     * @param studentRoot Corrected submission directory containing question folders (Q1, Q2, Q3)
     * @param username    Student username / ID
     * @param spec        Question specification (Q1a, Q1b, Q2a, Q2b, Q3)
     */
    
    public QuestionResult gradeQuestion(File studentRoot, String username, QuestionSpec spec) {
        String questionId = spec.getQuestionId();
        double maxScore = getMaxScoreForQuestion(questionId);

        try {
            // 1. Prepare Workspace via Person 3's WorkspaceManager
            QuestionWorkspace workspace;
            try {
                workspace = workspaceManager.prepare(studentRoot, username, spec);
            } catch (IOException e) {
                return new QuestionResult(questionId, 0.0, maxScore, describeWorkspaceError(e));
            }

            // 2. Compile via Person 3's JavaCompilerService
            CompilationResult compResult = compiler.compile(workspace, spec);

            if (!compResult.isSuccess()) {
                switch (compResult.getStatus()) {
                    case MISSING_SOURCES:
                        return new QuestionResult(questionId, 0.0, maxScore,
                                "Missing source files: " + String.join(", ", compResult.getMissingSources()));

                    case COMPILATION_ERROR:
                        return new QuestionResult(questionId, 0.0, maxScore,
                                summarizeCompilationError(compResult));

                    case SYSTEM_ERROR:
                        return new QuestionResult(questionId, 0.0, maxScore,
                                "INCOMPLETE (System Error): " + compResult.getOutput());

                    default:
                        return new QuestionResult(questionId, 0.0, maxScore,
                                "Compilation Failed: " + compResult.getOutput());
                }
            }

            // 3. Execute Tester Program via TestCaseRunner
            ExecutionResult execResult = testRunner.runTestSafely(workspace.getDirectory(), spec.getTesterClassName());

            if (execResult.getStatus() == ExecutionResult.Status.NOT_STARTED) {
                return new QuestionResult(questionId, 0.0, maxScore,
                        "INCOMPLETE (Execution Failed): " + execResult.getMessage());
            }

            // 4. Parse Score from Output (even if process timed out or threw an exception mid-way)
            String stdout = execResult.getStdout();
            double earnedScore = ScoreParser.parseScore(stdout, maxScore);

            if (execResult.isTimedOut()) {
                String msg = String.format("Timed out after %d ms. Partial Score: %.1f/%.1f",
                        execResult.getDurationMillis(), earnedScore, maxScore);
                return new QuestionResult(questionId, earnedScore, maxScore, msg);
            }

            if (execResult.getStatus() == ExecutionResult.Status.EXITED_WITH_ERROR) {
                String diagnostics = combineOutput(execResult.getStdout(), execResult.getStderr());
                String msg = String.format("Runtime Error (Exit Code %d). Partial Score: %.1f/%.1f%s",
                        execResult.getExitCode(), earnedScore, maxScore,
                        diagnostics.isEmpty() ? "" : "\n" + diagnostics);
                return new QuestionResult(questionId, earnedScore, maxScore, msg);
            }
            if (execResult.getStatus() == ExecutionResult.Status.INTERRUPTED) {
                String diagnostics = combineOutput(execResult.getStdout(), execResult.getStderr());
                String msg = String.format("Execution interrupted. Partial Score: %.1f/%.1f%s",
                        earnedScore, maxScore, diagnostics.isEmpty() ? "" : "\n" + diagnostics);
                return new QuestionResult(questionId, earnedScore, maxScore, msg);
            }
            if (execResult.getStatus() == ExecutionResult.Status.COMPLETED) {
                return new QuestionResult(questionId, earnedScore, maxScore, "Graded successfully");
            }
            return new QuestionResult(questionId, earnedScore, maxScore,
                    "INCOMPLETE (Unexpected execution status): " + execResult.getStatus());
        } catch (Exception e) {
            // Safety net: catch unexpected runtime crashes during grading
            String message = e.getMessage();
            return new QuestionResult(questionId, 0.0, maxScore,
                    "Grading Error: " + (message == null || message.isEmpty() ? e.getClass().getSimpleName() : message));
        } 
    }

    private static String combineOutput(String stdout, String stderr) {
        String standardOutput = stdout == null ? "" : stdout.trim();
        String standardError = stderr == null ? "" : stderr.trim();
        if (standardOutput.isEmpty()) return standardError;
        if (standardError.isEmpty()) return standardOutput;
        return standardOutput + System.lineSeparator() + standardError;
    }

    private static String describeWorkspaceError(IOException exception) {
        if (exception instanceof FileSystemException) {
            FileSystemException fileException = (FileSystemException) exception;
            String reason = fileException.getReason();
            if (reason != null && reason.toLowerCase().contains("being used by another process")) {
                return "Workspace Preparation Error: file is in use"
                        + (fileException.getFile() == null ? "" : " (" + fileException.getFile() + ")")
                        + "; retry after the other process releases it.";
            }
        }
        String message = exception.getMessage();
        return "Workspace Preparation Error: "
                + (message == null || message.isEmpty() ? exception.getClass().getSimpleName() : message);
    }

    private static String summarizeCompilationError(CompilationResult result) {
        Matcher matcher = COMPILER_ERROR_PATTERN.matcher(result.getOutput());
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> firstLocations = new LinkedHashMap<>();
        int totalErrors = 0;

        while (matcher.find()) {
            String category = categorizeCompilerError(matcher.group(3));
            counts.put(category, counts.getOrDefault(category, 0) + 1);
            firstLocations.putIfAbsent(category, matcher.group(1) + ":" + matcher.group(2));
            totalErrors++;
        }

        if (totalErrors == 0) {
            return "Compilation Error (javac exit code " + result.getExitCode() + "): "
                    + firstNonEmptyLine(result.getOutput());
        }

        StringBuilder summary = new StringBuilder("Compilation Error (javac exit code ")
                .append(result.getExitCode())
                .append(", ")
                .append(totalErrors)
                .append(totalErrors == 1 ? " error): " : " errors): ");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!first) {
                summary.append("; ");
            }
            first = false;
            summary.append(entry.getKey())
                    .append(" (")
                    .append(entry.getValue())
                    .append(", e.g. ")
                    .append(firstLocations.get(entry.getKey()))
                    .append(')');
        }
        return summary.toString();
    }

    private static String categorizeCompilerError(String diagnostic) {
        String lowerCase = diagnostic.toLowerCase();
        if (lowerCase.contains("cannot find symbol")) return "cannot find symbol";
        if (lowerCase.contains("incompatible types")) return "incompatible types";
        if (lowerCase.contains("preview feature")) return "preview feature disabled";
        if (lowerCase.contains("bad operand types")) return "invalid operand types";
        return diagnostic;
    }

    private static String firstNonEmptyLine(String output) {
        if (output != null) {
            for (String line : output.split("\\R")) {
                if (!line.trim().isEmpty()) {
                    String trimmed = line.trim();
                    return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 197) + "...";
                }
            }
        }
        return "No compiler diagnostic was provided";
    }

    public static double getMaxScoreForQuestion(String questionId) {
        switch (questionId.toUpperCase()) {
            case "Q1A": return 3.0;
            case "Q1B": return 3.0;
            case "Q2A": return 5.0;
            case "Q2B": return 5.0;
            case "Q3":  return 4.0;
            default:    return 4.0;
        }
    }
}