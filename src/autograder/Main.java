
package autograder;

// Import the main GUI window of the AutoGradingSystem.
import autograder.ui.MainFrame;

// Swing utilities for safely starting the GUI.
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main.java
 *
 * Entry point of the AutoGradingSystem application.
 *
 * Responsibilities:
 * 1. Apply the operating system's default look and feel.
 * 2. Launch the main GUI on the Swing Event Dispatch Thread (EDT).
 */
public class Main {

    public static void main(String[] args) {

        // Set the GUI appearance to match the user's operating system
        // (e.g., Windows look and feel).
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception e) {
            // If the system look and feel cannot be applied,
            // the application continues using the default appearance.
            System.out.println("Could not apply system look and feel.");
        }

        // Swing GUI components should be created and updated on the
        // Event Dispatch Thread (EDT) to ensure thread safety.
        SwingUtilities.invokeLater(() -> {

            // Create the main application window by callingthe MainFrame.java constructor to initialize the GUI components.
            MainFrame frame = new MainFrame();

            // Display the window to the user.
            frame.setVisible(true);
        });
    }
}
