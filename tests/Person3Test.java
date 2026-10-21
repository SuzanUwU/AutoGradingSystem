import autograder.compilation.JavaCompilerService;
import autograder.compilation.QuestionWorkspace;
import autograder.compilation.WorkspaceManager;
import autograder.execution.ProcessRunner;
import autograder.execution.ProgramExecutor;
import autograder.model.CompilationResult;
import autograder.model.ExecutionResult;
import autograder.model.QuestionSpec;
import autograder.model.SubmissionResult;
import autograder.service.AutoGradingService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-check for the Person 3 pipeline: WorkspaceManager -> JavaCompilerService -> ProgramExecutor.
 *
 * <p>Part 1 grades the six sample submissions and compares each question with the known outcome.
 * Parts 2-4 build small fake submissions and programs on the fly to exercise edge cases.
 * Every check prints PASS or FAIL; the process exits with code 1 if anything failed.
 *
 * <p>Run from the project root with {@code bash tests/run-person3-tests.sh [submissionsFolder]}.
 */
public class Person3Test {
    private static final File TESTERS = new File("assessment/testers");
    private static final File RESOURCES = new File("assessment/resources");
    private static final long COMPILE_TIMEOUT_SECONDS = 30;
    private static final long SAMPLE_TIMEOUT_SECONDS = 5;
    private static final long EDGE_TIMEOUT_SECONDS = 2;
    private static final int MAX_OUTPUT_BYTES = 1_000_000;
    private static final int SMALL_OUTPUT_BYTES = 10_000;

    private static final List<QuestionSpec> QUESTIONS = List.of(
            new QuestionSpec("Q1a", "Q1", "Q1aTester", List.of("Q1a.java")),
            new QuestionSpec("Q1b", "Q1", "Q1bTester", List.of("Q1b.java")),
            new QuestionSpec("Q2a", "Q2", "Q2aTester", List.of("Q2a.java")),
            new QuestionSpec("Q2b", "Q2", "Q2bTester", List.of("Q2b.java")),
            new QuestionSpec("Q3", "Q3", "Q3Tester", List.of("Q3.java")));

    // Expected outcome per question (Q1a Q1b Q2a Q2b Q3): the tester's printed score, or a failure status.
    // These come from running the original testers by hand, not from groundtruth.txt.
    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("chee.teo.2022", "3.0 3.0 5.0 1.0 0.0");
        EXPECTED.put("david.2024", "3.0 3.0 4.0 0.0 0.0");
        EXPECTED.put("fen.lai.2022", "1.0 0.0 5.0 4.0 COMPILATION_ERROR");
        EXPECTED.put("jing.lim.2021", "COMPILATION_ERROR COMPILATION_ERROR COMPILATION_ERROR 0.0 0.0");
        EXPECTED.put("ping.lee.2023", "3.0 3.0 5.0 5.0 4.0");
        EXPECTED.put("xing.yan.2023", "0.0 0.0 TIMED_OUT 0.0 0.0");
    }

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws IOException {
        File submissionsFolder = new File(args.length > 0 ? args[0] : "submissions");
        File temp = Files.createTempDirectory("person3-test").toFile();
        System.out.println("Test files are kept for inspection under: " + temp.getPath());

        testSampleSubmissions(submissionsFolder, new File(temp, "samples"));
        testWorkspaceManager(new File(temp, "workspace-cases"));
        testJavaCompilerService(new File(temp, "compiler-cases"));
        testProgramExecutor(new File(temp, "executor-cases"));

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------- Part 1: six sample submissions

    private static void testSampleSubmissions(File submissionsFolder, File temp) throws IOException {
        section("Part 1: sample submissions in " + submissionsFolder.getPath());
        File[] zips = submissionsFolder.listFiles(file -> file.getName().toLowerCase().endsWith(".zip"));
        if (zips == null || zips.length == 0) {
            System.out.println("SKIP  no ZIP files found; copy the 6 sample ZIPs into " + submissionsFolder.getPath());
            return;
        }

        File step1Folder = new File(temp, "step1");
        List<String> usernames = runStep1(submissionsFolder, step1Folder);

        WorkspaceManager workspaceManager = new WorkspaceManager(new File(temp, "grading"), TESTERS, RESOURCES);
        JavaCompilerService compiler = new JavaCompilerService(COMPILE_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES);
        ProgramExecutor executor = new ProgramExecutor(SAMPLE_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES, List.of("-Xmx256m"));

        for (String username : usernames) {
            File studentRoot = findStudentRoot(step1Folder, username);
            if (studentRoot == null) {
                check(username, false, "Step 1 produced no folder for this student");
                continue;
            }
            List<String> outcomes = new ArrayList<>();
            for (QuestionSpec question : QUESTIONS) {
                outcomes.add(gradeOnce(workspaceManager, compiler, executor, studentRoot, username, question));
            }
            String actual = String.join(" ", outcomes);
            String expected = EXPECTED.get(username);
            if (expected == null) System.out.println("INFO  " + username + " (no expected values): " + actual);
            else check(username + " -> " + actual, actual.equals(expected), "expected " + expected);
        }
    }

    /** Runs Person 2's Step 1 (extract, correct, validate) and returns the usernames it processed. */
    private static List<String> runStep1(File submissionsFolder, File step1Folder) {
        List<String> usernames = new ArrayList<>();
        new AutoGradingService(step1Folder).processSubmissions(submissionsFolder, new AutoGradingService.Listener() {
            @Override public void onLog(String message) { }
            @Override public void onSubmissionFinished(SubmissionResult result) { usernames.add(result.getUsername()); }
            @Override public void onProgress(int processed, int total) { }
        });
        Collections.sort(usernames);
        return usernames;
    }

    private static File findStudentRoot(File step1Folder, String username) {
        File[] submissionFolders = step1Folder.listFiles(File::isDirectory);
        if (submissionFolders == null) return null;
        for (File folder : submissionFolders) {
            File root = new File(folder, username);
            if (folder.getName().endsWith(username) && root.isDirectory()) return root;
        }
        return null;
    }

    /** Prepares, compiles and runs one question; returns the printed score or the failure status. */
    private static String gradeOnce(WorkspaceManager workspaceManager, JavaCompilerService compiler,
            ProgramExecutor executor, File studentRoot, String username, QuestionSpec question) throws IOException {
        QuestionWorkspace workspace = workspaceManager.prepare(studentRoot, username, question);
        CompilationResult compilation = compiler.compile(workspace, question);
        if (!compilation.isSuccess()) return compilation.getStatus().name();
        ExecutionResult execution = executor.runTester(workspace, question);
        if (!execution.isCompleted()) return execution.getStatus().name();
        return lastLine(execution.getStdout());
    }

    // ---------------------------------------------------------------- Part 2: WorkspaceManager

    private static void testWorkspaceManager(File temp) throws IOException {
        section("Part 2: WorkspaceManager");
        File student = new File(temp, "student");
        write(new File(student, "q1/Q1a.java"), VALID_Q1A);          // lower-case folder name on purpose
        write(new File(student, "q1/Q1a.class"), "stale bytes");     // stale student bytecode
        write(new File(student, "Q3/Q3.java"), "public class Q3 { }");
        write(new File(student, "Q3/Rectangle.class"), "tampered bytes");  // student copy of a trusted class
        WorkspaceManager manager = new WorkspaceManager(new File(temp, "grading"), TESTERS, RESOURCES);

        QuestionWorkspace q1a = manager.prepare(student, "alice", QUESTIONS.get(0));
        check("finds question folder regardless of case (q1)", q1a.isReady(), "missing " + q1a.getMissingSources());
        check("does not copy student .class files", !new File(q1a.getDirectory(), "Q1a.class").exists(), "Q1a.class was copied");
        check("copies the trusted tester", new File(q1a.getDirectory(), "Q1aTester.java").isFile(), "Q1aTester.java missing");

        QuestionWorkspace q3 = manager.prepare(student, "alice", QUESTIONS.get(4));
        File rectangle = new File(q3.getDirectory(), "Rectangle.class");
        File trustedRectangle = new File(RESOURCES, "Q3/Rectangle.class");
        check("uses the trusted Rectangle.class, not the student's copy",
                Files.mismatch(rectangle.toPath(), trustedRectangle.toPath()) == -1, "Rectangle.class differs from trusted copy");

        QuestionWorkspace q2a = manager.prepare(student, "alice", QUESTIONS.get(2));
        check("reports missing sources instead of throwing", !q2a.isReady()
                && q2a.getMissingSources().equals(List.of("Q2a.java")), "missing " + q2a.getMissingSources());
        check("copies trusted Q2 data files", new File(q2a.getDirectory(), "personstester.txt").isFile()
                && new File(q2a.getDirectory(), "DataException.class").isFile(), "Q2 resources missing");
        check("does not provide is442.txt (testers expect it to be absent)",
                !new File(q2a.getDirectory(), "is442.txt").exists(), "is442.txt exists");

        write(new File(q1a.getDirectory(), "leftover.class"), "old");
        q1a = manager.prepare(student, "alice", QUESTIONS.get(0));
        check("re-preparing clears files from a previous run", !new File(q1a.getDirectory(), "leftover.class").exists(),
                "leftover.class survived");

        expectIOException("rejects a username that escapes the grading folder",
                () -> manager.prepare(student, "../../escape", QUESTIONS.get(0)));
        expectIOException("throws when the trusted tester is missing",
                () -> manager.prepare(student, "alice", new QuestionSpec("Q9", "Q1", "Q9Tester", List.of("Q9.java"))));
    }

    // ---------------------------------------------------------------- Part 3: JavaCompilerService

    private static void testJavaCompilerService(File temp) throws IOException {
        section("Part 3: JavaCompilerService");
        File student = new File(temp, "student");
        write(new File(student, "Q1/Q1a.java"), "/*\n * Name: 测试 学生\n */\n" + VALID_Q1A);
        write(new File(student, "Q1/Q1b.java"), "public class Q1b { this does not compile }");
        WorkspaceManager manager = new WorkspaceManager(new File(temp, "grading"), TESTERS, RESOURCES);
        JavaCompilerService compiler = new JavaCompilerService(COMPILE_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES);

        CompilationResult q1a = compiler.compile(manager.prepare(student, "bob", QUESTIONS.get(0)), QUESTIONS.get(0));
        check("Q1a compiles although broken Q1b.java sits next to it (UTF-8 header)", q1a.isSuccess(), q1a.getOutput());

        CompilationResult q1b = compiler.compile(manager.prepare(student, "bob", QUESTIONS.get(1)), QUESTIONS.get(1));
        check("broken Q1b gives COMPILATION_ERROR with javac's message",
                q1b.getStatus() == CompilationResult.Status.COMPILATION_ERROR && q1b.getOutput().contains("error"),
                q1b.getStatus() + ": " + q1b.getOutput());

        CompilationResult q2a = compiler.compile(manager.prepare(student, "bob", QUESTIONS.get(2)), QUESTIONS.get(2));
        check("missing source gives MISSING_SOURCES without running javac",
                q2a.getStatus() == CompilationResult.Status.MISSING_SOURCES, q2a.getStatus().name());

        JavaCompilerService brokenJavac = new JavaCompilerService("/no/such/javac", COMPILE_TIMEOUT_SECONDS,
                new ProcessRunner(MAX_OUTPUT_BYTES));
        CompilationResult noJavac = brokenJavac.compile(manager.prepare(student, "bob", QUESTIONS.get(0)), QUESTIONS.get(0));
        check("javac not found gives SYSTEM_ERROR instead of an exception",
                noJavac.getStatus() == CompilationResult.Status.SYSTEM_ERROR, noJavac.getStatus().name());
    }

    // ---------------------------------------------------------------- Part 4: ProgramExecutor

    private static void testProgramExecutor(File temp) throws IOException {
        section("Part 4: ProgramExecutor / ProcessRunner (time limit " + EDGE_TIMEOUT_SECONDS + "s)");
        File programs = new File(temp, "programs");
        write(new File(programs, "Hang.java"),
                "public class Hang { public static void main(String[] a) { System.out.println(\"Passed\"); while (true) { } } }");
        write(new File(programs, "Spam.java"),
                "public class Spam { public static void main(String[] a) { while (true) System.out.println(\"spam spam\"); } }");
        write(new File(programs, "ReadInput.java"),
                "public class ReadInput { public static void main(String[] a) {"
                        + " System.out.println(new java.util.Scanner(System.in).hasNextLine()); } }");
        write(new File(programs, "ExitThree.java"),
                "public class ExitThree { public static void main(String[] a) { System.err.println(\"boom\"); System.exit(3); } }");
        write(new File(programs, "MemoryHog.java"),
                "public class MemoryHog { public static void main(String[] a) {"
                        + " java.util.List<long[]> l = new java.util.ArrayList<>(); while (true) l.add(new long[1_000_000]); } }");
        write(new File(programs, "DeepRecursion.java"),
                "public class DeepRecursion { static int f(int n) { return f(n + 1) + 1; }"
                        + " public static void main(String[] a) { f(0); } }");
        write(new File(programs, "PrintArgs.java"),
                "public class PrintArgs { public static void main(String[] a) { System.out.println(String.join(\"|\", a)); } }");

        File emptyStudent = new File(temp, "student");
        Files.createDirectories(emptyStudent.toPath());
        WorkspaceManager manager = new WorkspaceManager(new File(temp, "grading"), programs, RESOURCES);
        JavaCompilerService compiler = new JavaCompilerService(COMPILE_TIMEOUT_SECONDS, MAX_OUTPUT_BYTES);
        ProgramExecutor executor = new ProgramExecutor(EDGE_TIMEOUT_SECONDS, SMALL_OUTPUT_BYTES, List.of("-Xmx64m"));
        long limitMillis = (EDGE_TIMEOUT_SECONDS + 3) * 1000;

        ExecutionResult hang = runProgram(manager, compiler, executor, emptyStudent, "Hang");
        check("infinite loop is killed: TIMED_OUT within the limit", hang.isTimedOut()
                && hang.getDurationMillis() < limitMillis, hang.getStatus() + " after " + hang.getDurationMillis() + "ms");
        check("output printed before the kill is kept", hang.getStdout().contains("Passed"), "stdout: " + hang.getStdout());

        ExecutionResult spam = runProgram(manager, compiler, executor, emptyStudent, "Spam");
        check("infinite printing is killed and output is capped", spam.isTimedOut() && spam.isOutputTruncated()
                && spam.getStdout().length() <= SMALL_OUTPUT_BYTES,
                spam.getStatus() + ", kept " + spam.getStdout().length() + " bytes");

        ExecutionResult input = runProgram(manager, compiler, executor, emptyStudent, "ReadInput");
        check("reading System.in sees end-of-file instead of hanging",
                input.isCompleted() && input.getStdout().trim().equals("false"), input.getStatus() + ": " + input.getStdout());

        ExecutionResult exit = runProgram(manager, compiler, executor, emptyStudent, "ExitThree");
        check("System.exit(3) gives EXITED_WITH_ERROR, exit code 3, stderr kept",
                exit.getStatus() == ExecutionResult.Status.EXITED_WITH_ERROR && exit.getExitCode() == 3
                        && exit.getStderr().contains("boom"), exit.getStatus() + " exit=" + exit.getExitCode());

        ExecutionResult memory = runProgram(manager, compiler, executor, emptyStudent, "MemoryHog");
        check("memory limit stops a memory hog with OutOfMemoryError",
                memory.getStatus() == ExecutionResult.Status.EXITED_WITH_ERROR
                        && memory.getStderr().contains("OutOfMemoryError"), memory.getStatus() + ": " + firstLine(memory.getStderr()));

        ExecutionResult deep = runProgram(manager, compiler, executor, emptyStudent, "DeepRecursion");
        check("infinite recursion gives EXITED_WITH_ERROR with StackOverflowError",
                deep.getStatus() == ExecutionResult.Status.EXITED_WITH_ERROR
                        && deep.getStderr().contains("StackOverflowError"), deep.getStatus() + ": " + firstLine(deep.getStderr()));

        QuestionWorkspace argsWorkspace = prepareProgram(manager, compiler, emptyStudent, "PrintArgs");
        ExecutionResult args = executor.run(argsWorkspace.getDirectory(), "PrintArgs", List.of("3", "with space"));
        check("program arguments (including spaces) are passed intact",
                args.getStdout().trim().equals("3|with space"), "stdout: " + args.getStdout());

        ExecutionResult missing = executor.run(argsWorkspace.getDirectory(), "NoSuchClass", List.of());
        check("missing compiled class gives NOT_STARTED", missing.getStatus() == ExecutionResult.Status.NOT_STARTED,
                missing.getStatus().name());
    }

    /** Treats a stand-alone program as a "tester" so it goes through the real prepare/compile/run pipeline. */
    private static ExecutionResult runProgram(WorkspaceManager manager, JavaCompilerService compiler,
            ProgramExecutor executor, File student, String className) throws IOException {
        QuestionWorkspace workspace = prepareProgram(manager, compiler, student, className);
        return executor.run(workspace.getDirectory(), className, List.of());
    }

    private static QuestionWorkspace prepareProgram(WorkspaceManager manager, JavaCompilerService compiler,
            File student, String className) throws IOException {
        QuestionSpec spec = new QuestionSpec(className, "none", className, List.of());
        QuestionWorkspace workspace = manager.prepare(student, "programs", spec);
        CompilationResult compilation = compiler.compile(workspace, spec);
        if (!compilation.isSuccess()) throw new IOException("Test program did not compile: " + compilation.getOutput());
        return workspace;
    }

    // ---------------------------------------------------------------- helpers

    private static final String VALID_Q1A = "import java.util.*;\n"
            + "public class Q1a {\n"
            + "    public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {\n"
            + "        return new ArrayList<>();\n"
            + "    }\n"
            + "}\n";

    private interface IOAction {
        void run() throws IOException;
    }

    private static void expectIOException(String name, IOAction action) {
        try {
            action.run();
            check(name, false, "no IOException was thrown");
        } catch (IOException e) {
            check(name, true, "");
        }
    }

    private static void check(String name, boolean condition, String detailIfFailed) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
            System.out.println("      " + detailIfFailed);
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("=== " + title);
    }

    private static void write(File file, String content) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String lastLine(String text) {
        String[] lines = text.trim().split("\\R");
        return lines[lines.length - 1].trim();
    }

    private static String firstLine(String text) {
        return text.trim().split("\\R")[0];
    }
}
