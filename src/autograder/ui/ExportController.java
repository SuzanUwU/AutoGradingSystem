package autograder.ui;

import autograder.reporting.CsvExporter;
import autograder.reporting.PdfReportExporter;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** ExportController responsibilities extracted from MainFrame. */
final class ExportController {
    private final MainFrame f;

    ExportController(MainFrame frame) { this.f = frame; }

    /** Exports official grades CSV and detailed PDF report. */
    void exportResults() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Results (CSV + PDF)");
        chooser.setSelectedFile(new File("gradedscores.csv"));

        if (chooser.showSaveDialog(f) != JFileChooser.APPROVE_OPTION) return;

        Path csvTarget = chooser.getSelectedFile().toPath().toAbsolutePath();
        if (!csvTarget.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) csvTarget = Path.of(csvTarget + ".csv");

        Path pdfTarget = csvTarget.resolveSibling(csvTarget.getFileName().toString().replaceFirst("(?i)\\.csv$", "_report.pdf"));

        if ((Files.exists(csvTarget) || Files.exists(pdfTarget))
                && JOptionPane.showConfirmDialog(f,
                "Replace existing export file(s)?\n" + csvTarget + "\n" + pdfTarget,
                "Replace Export", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            // Official school-format CSV.
            new CsvExporter().export(csvTarget, f.studentResults);

            // Detailed PDF containing grades, corrections and edge-case results.
            new PdfReportExporter().export(
                    pdfTarget,
                    f.studentResults,
                    f.edgeCaseResults,
                    f.isPythonMode() ? "IS1704 - Python" : "CS102 - Java"
            );

            f.uiState.appendLog("Saved CSV: " + csvTarget);
            f.uiState.appendLog("Saved PDF: " + pdfTarget);

            JOptionPane.showMessageDialog(f,
                    "Saved:\n" + csvTarget + "\n" + pdfTarget,
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            f.uiState.appendLog("[ERROR] Export: " + ex.getMessage());
            JOptionPane.showMessageDialog(f,
                    "Could not export the CSV/PDF.\n" + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}