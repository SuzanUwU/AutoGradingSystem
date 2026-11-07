package autograder.ui;

import javax.swing.*;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;


/** TesterActions responsibilities extracted from MainFrame. */
final class TesterActionsController {
    private final MainFrame f;
    TesterActionsController(MainFrame frame) { this.f = frame; }

    /** Handles generate testers. */
    void generateTesters() {
        // Path dataFolder = Path.of("assessment", "test-data");
        // Path outputFolder = Path.of("generated_testers");

        try {
            List<Path> generated = new autograder.testgen.GeneratedTesterService().generate();

            f.uiState.appendLog("\n=== PARAMETERIZED TESTER GENERATION ===");
            for (Path file : generated) f.uiState.appendLog("Generated: " + file);
            f.uiState.appendLog("Generated testers are ready. Click Grade with Generated Testers.");

            f.generatedTestersReadyForCurrentRun = true;
            f.gradeGeneratedButton.setEnabled(!f.busy && !f.isPythonMode() && !f.studentResults.isEmpty() && generatedTestersReady());
            JOptionPane.showMessageDialog(f,
                    generated.size() + " tester files generated successfully.\n\n"
                            + "Next: click Grade with Generated Testers.",
                    "Testers Generated",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            f.uiState.appendLog("[ERROR] Tester generation: " + e.getMessage());
            JOptionPane.showMessageDialog(f,
                    "Could not generate tester files.\n" + e.getMessage(),
                    "Tester Generation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Checks whether the generated testers can be used. */
    boolean generatedTestersReady() {
        return new autograder.testgen.GeneratedTesterService().isReady();
    }
}
