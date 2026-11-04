package autograder.ui;

import autograder.python.PythonGradingService;
import autograder.model.SubmissionResult;
import autograder.model.QuestionResult;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Background coordinator for IS1704 grading; keeps process logic out of MainFrame. */
public final class PythonGradingController {
    public interface Listener {
        /** Called on Swing's event dispatch thread. */
        void onResult(int row, List<QuestionResult> questions, String error);
        /** Called once, on Swing's event dispatch thread. */
        void onFinished(String error);
    }
    private final File assessmentDir;
    private final File submissionsFolder;
    private final Listener listener;

    public PythonGradingController(File assessmentDir, File submissionsFolder, Listener listener) {
        this.assessmentDir = assessmentDir;
        this.submissionsFolder = submissionsFolder;
        this.listener = listener;
    }

    public void grade(List<SubmissionResult> submissions) {
        new SwingWorker<Void, Update>() {
            @Override protected Void doInBackground() {
                PythonGradingService grader = new PythonGradingService(assessmentDir, "python");
                for (int row = 0; row < submissions.size(); row++) {
                    SubmissionResult submission = submissions.get(row);
                    String username = submission.getUsername();
                    try {
                        if ("Failed".equals(submission.getOverallStatus())) {
                            publish(new Update(row, List.of(), "Skipped: validation failed."));
                            continue;
                        }
                        File original = new File(submissionsFolder, username + ".zip");
                        File corrected = new File("output/is1704-corrected", username + ".zip");
                        File selected = "Corrected".equals(submission.getOverallStatus()) && corrected.isFile()
                                ? corrected : original;
                        if (!selected.isFile()) throw new IOException("ZIP not found: " + selected);
                        Path temporary = Files.createTempDirectory("is1704-grading-");
                        try {
                            File answers = temporary.resolve("answers").toFile();
                            PythonSubmissionExtractor.extract(selected, answers);
                            publish(new Update(row, grader.gradeStudent(answers), ""));
                        } finally {
                            try (var paths = Files.walk(temporary)) {
                                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                                    try { Files.deleteIfExists(path); }
                                    catch (IOException ignored) { /* best-effort cleanup */ }
                                });
                            }
                        }
                    } catch (Exception ex) {
                        publish(new Update(row, List.of(), ex.toString()));
                    }
                }
                return null;
            }
            @Override protected void process(List<Update> updates) {
                for (Update update : updates) listener.onResult(update.row, update.questions, update.error);
            }
            @Override protected void done() {
                String error = "";
                try { get(); }
                catch (Exception ex) { error = ex.toString(); }
                listener.onFinished(error);
            }
        }.execute();
    }

    private static final class Update {
        final int row;
        final List<QuestionResult> questions;
        final String error;
        Update(int row, List<QuestionResult> questions, String error) {
            this.row = row; this.questions = questions; this.error = error;
        }
    }
}
