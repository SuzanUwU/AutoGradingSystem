package autograder.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** WorkspaceLocator responsibilities extracted from MainFrame. */
final class WorkspaceLocator {
    private final MainFrame f;
    WorkspaceLocator(MainFrame frame) { this.f = frame; }

    /** Handles find student root. */
    File findStudentRoot(File workspace, String username) {
        File[] folders = workspace.listFiles(File::isDirectory);

        if (folders == null) {
            return null;
        }

        List<File> matches = new ArrayList<>();

        for (File folder : folders) {
            if (folder.getName().equals(username)) {
                matches.add(folder);
                continue;
            }

            File nestedStudent = new File(folder, username);

            if (nestedStudent.isDirectory()) {
                matches.add(nestedStudent);
            }
        }

        // Avoid selecting an arbitrary folder if duplicates exist.
        if (matches.size() != 1) {
            return null;
        }

        return resolveStudentRoot(matches.get(0));
    }

    /** Handles resolve student root. */
    File resolveStudentRoot(File folder) {
        if (new File(folder, "Q1").isDirectory()
                || new File(folder, "Q2").isDirectory()
                || new File(folder, "Q3").isDirectory()) {
            return folder;
        }

        File[] children = folder.listFiles(File::isDirectory);

        if (children != null && children.length == 1) {
            File child = children[0];

            if (new File(child, "Q1").isDirectory()
                    || new File(child, "Q2").isDirectory()
                    || new File(child, "Q3").isDirectory()) {
                return child;
            }
        }

        return folder;
    }
}
