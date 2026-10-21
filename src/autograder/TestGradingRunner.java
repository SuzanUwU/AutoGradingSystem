package autograder;

import autograder.compilation.JavaCompilerService;
import autograder.compilation.WorkspaceManager;
import autograder.execution.ProgramExecutor;
import autograder.grading.GradingEngine;
import autograder.grading.QuestionGrader;
import autograder.grading.TestCaseRunner;
import autograder.model.QuestionResult;
import autograder.model.QuestionSpec;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestGradingRunner {

    public static void main(String[] args) {
        System.out.println("=== RUNNING BATCH GRADING TEST WITH AUTO-FLATTENING ===");

        File reformattedSubmissionsDir = new File("workspace");
        File gradingBuildRoot = new File("temp_workspaces"); 
        File testersFolder = new File("assessment/testers");
        File resourcesFolder = new File("assessment/resources");

        if (!reformattedSubmissionsDir.exists() || !reformattedSubmissionsDir.isDirectory()) {
            System.err.println("ERROR: Cannot find submissions directory at " + reformattedSubmissionsDir.getAbsolutePath());
            return;
        }

        // 1. Initialize Services
        WorkspaceManager workspaceManager = new WorkspaceManager(gradingBuildRoot, testersFolder, resourcesFolder);
        JavaCompilerService compiler = new JavaCompilerService(10, 1024 * 1024);
        ProgramExecutor executor = new ProgramExecutor(5, 1024 * 1024, new ArrayList<>());

        TestCaseRunner testRunner = new TestCaseRunner(executor);
        QuestionGrader questionGrader = new QuestionGrader(compiler, testRunner, workspaceManager);

        // 2. Define Question Specs
        List<QuestionSpec> specs = Arrays.asList(
            new QuestionSpec("Q1a", "Q1", "Q1aTester", List.of("Q1a.java")),
            new QuestionSpec("Q1b", "Q1", "Q1bTester", List.of("Q1b.java")),
            new QuestionSpec("Q2a", "Q2", "Q2aTester", List.of("Q2a.java")),
            new QuestionSpec("Q2b", "Q2", "Q2bTester", List.of("Q2b.java")),
            new QuestionSpec("Q3",  "Q3", "Q3Tester",  List.of("Q3.java"))
        );

        GradingEngine engine = new GradingEngine(questionGrader, specs);

        File[] studentDirs = reformattedSubmissionsDir.listFiles(File::isDirectory);

        if (studentDirs == null || studentDirs.length == 0) {
            System.out.println("No student directories found inside: " + reformattedSubmissionsDir.getAbsolutePath());
            return;
        }

        System.out.printf("Found %d student folder(s) for grading.\n\n", studentDirs.length);

        // 3. Process each student folder
        for (File rawStudentDir : studentDirs) {
            String username = rawStudentDir.getName();

            // Automatically resolve nested wrapper folders like workspace/2023-2024-chee.teo.2022/chee.teo.2022/
            File actualStudentRoot = resolveStudentRoot(rawStudentDir);

            System.out.println("=================================================");
            System.out.println("Grading Student: " + username);
            System.out.println("Resolved Directory: " + actualStudentRoot.getPath());

            try {
                List<QuestionResult> results = engine.gradeStudent(actualStudentRoot, username);

                double totalScore = 0;
                double totalMax = 0;
                for (QuestionResult res : results) {
                    System.out.println("  " + res.toString());
                    totalScore += res.getScore();
                    totalMax += res.getMaxScore();
                }     

                System.out.println("  ---------------------------------------------");
                System.out.printf("  TOTAL SCORE: %.1f / %.1f\n", totalScore, totalMax);

            } catch (Exception e) {
                System.err.println("  Unexpected error grading " + username + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("=================================================");
    }

    /**
     * Helper method that drills down if a student folder contains an extra single nested folder wrapper.
     */
    private static File resolveStudentRoot(File dir) {
        File[] subfolders = dir.listFiles(File::isDirectory);
        
        // If top folder doesn't contain Q1, Q2, or Q3 directly, check if it has a single subfolder containing them
        if (!containsQuestionFolders(dir) && subfolders != null && subfolders.length == 1) {
            File innerFolder = subfolders[0];
            if (containsQuestionFolders(innerFolder)) {
                return innerFolder;
            }
        }
        return dir;
    }

    private static boolean containsQuestionFolders(File dir) {
        File[] subfolders = dir.listFiles(File::isDirectory);
        if (subfolders == null) return false;
        for (File f : subfolders) {
            String name = f.getName().toUpperCase();
            if (name.equals("Q1") || name.equals("Q2") || name.equals("Q3")) {
                return true;
            }
        }
        return false;
    }
}