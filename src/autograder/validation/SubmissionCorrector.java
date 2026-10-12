package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class SubmissionCorrector {

    public File correctStructure(File extractedRoot, String expectedUsername) throws IOException {
        File expectedFolder = new File(extractedRoot, expectedUsername);

        if (expectedFolder.isDirectory()) {
            System.out.println("[OK] Username folder is correct: " + expectedUsername);
            return expectedFolder;
        }

        if (hasDirectQuestionFolder(extractedRoot)) {
            if (!expectedFolder.mkdirs() && !expectedFolder.exists()) throw new IOException("Cannot create corrected folder: " + expectedFolder.getPath());

            moveQuestionFolder(extractedRoot, expectedFolder, "Q1");
            moveQuestionFolder(extractedRoot, expectedFolder, "Q2");
            moveQuestionFolder(extractedRoot, expectedFolder, "Q3");

            System.out.println("[CORRECTED] Added missing username folder: " + expectedUsername);
            return expectedFolder;
        }

        File singleFolder = getSingleTopLevelFolder(extractedRoot);

        if (singleFolder != null) {
            String oldName = singleFolder.getName();
            Files.move(singleFolder.toPath(), expectedFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[CORRECTED] Renamed top-level folder from " + oldName + " to " + expectedUsername);
            return expectedFolder;
        }

        System.out.println("[WARNING] Folder structure could not be corrected automatically.");
        return extractedRoot;
    }

    public void correctHeaders(File submissionFolder, String expectedUsername) throws IOException {
        ArrayList<File> javaFiles = new ArrayList<File>();
        collectJavaFiles(submissionFolder, javaFiles);

        String trustedName = findTrustedName(javaFiles, expectedUsername);
        boolean corrected = false;

        for (File javaFile : javaFiles) {
            if (correctHeader(javaFile, expectedUsername, trustedName)) corrected = true;
        }

        if (!corrected) System.out.println("[OK] No Java header corrections needed.");
    }

    private String findTrustedName(ArrayList<File> javaFiles, String expectedUsername) throws IOException {
        for (File javaFile : javaFiles) {
            List<String> lines = Files.readAllLines(javaFile.toPath());
            String name = getHeaderValue(lines, "Name:");
            String emailId = getHeaderValue(lines, "Email ID:");

            if (name != null && !name.isEmpty() && isStudentEmail(emailId, expectedUsername)) return name;
        }

        return null;
    }

    private boolean correctHeader(File javaFile, String expectedUsername, String trustedName) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());

        int nameIndex = findHeaderLine(lines, "Name:");
        int emailIndex = findHeaderLine(lines, "Email ID:");

        String name = getHeaderValue(lines, "Name:");
        String emailId = getHeaderValue(lines, "Email ID:");

        boolean nameMissing = name == null || name.isEmpty();
        boolean emailMissing = emailId == null || emailId.isEmpty();
        boolean fullEmail = emailId != null && emailId.equalsIgnoreCase(expectedUsername + "@computing.smu.edu.sg");
        boolean changed = false;

        // No Name or Email ID lines at all
        if (nameIndex == -1 && emailIndex == -1) {
            ArrayList<String> header = new ArrayList<String>();
            header.add("/*");
            header.add(" * Name: " + (trustedName == null ? "" : trustedName));
            header.add(" * Email ID: " + expectedUsername);
            header.add(" */");
            header.add("");
            header.addAll(lines);

            if (trustedName == null) System.out.println("[WARNING] Cannot determine student name for " + javaFile.getName());
            else System.out.println("[CORRECTED] Added Name to " + javaFile.getName());

            System.out.println("[CORRECTED] Added Email ID to " + javaFile.getName());
            Files.write(javaFile.toPath(), header);
            return true;
        }

        if (nameMissing) {
            if (trustedName != null) {
                if (nameIndex >= 0) lines.set(nameIndex, getPrefix(lines.get(nameIndex)) + "Name: " + trustedName);
                else {
                    emailIndex = findHeaderLine(lines, "Email ID:");
                    lines.add(emailIndex, " * Name: " + trustedName);
                }

                changed = true;
                System.out.println("[CORRECTED] Added Name to " + javaFile.getName());
            } else {
                System.out.println("[WARNING] Cannot determine student name for " + javaFile.getName());
            }
        }

        if (emailMissing) {
            emailIndex = findHeaderLine(lines, "Email ID:");

            if (emailIndex >= 0) {
                lines.set(emailIndex, getPrefix(lines.get(emailIndex)) + "Email ID: " + expectedUsername);
            } else {
                nameIndex = findHeaderLine(lines, "Name:");
                if (nameIndex >= 0) lines.add(nameIndex + 1, " * Email ID: " + expectedUsername);
            }

            changed = true;
            System.out.println("[CORRECTED] Added Email ID to " + javaFile.getName());
        } else if (fullEmail) {
            emailIndex = findHeaderLine(lines, "Email ID:");
            lines.set(emailIndex, getPrefix(lines.get(emailIndex)) + "Email ID: " + expectedUsername);

            changed = true;
            System.out.println("[CORRECTED] Email ID in " + javaFile.getName() + ": " + expectedUsername);
        }

        if (changed) Files.write(javaFile.toPath(), lines);
        return changed;
    }

    private boolean isStudentEmail(String emailId, String expectedUsername) {
        if (emailId == null) return false;

        return emailId.equalsIgnoreCase(expectedUsername)
                || emailId.equalsIgnoreCase(expectedUsername + "@computing.smu.edu.sg");
    }

    private int findHeaderLine(List<String> lines, String label) {
        for (int i = 0; i < lines.size(); i++) {
            if (cleanHeaderLine(lines.get(i)).startsWith(label)) return i;
        }

        return -1;
    }

    private String getHeaderValue(List<String> lines, String label) {
        int index = findHeaderLine(lines, label);
        if (index == -1) return null;

        String text = cleanHeaderLine(lines.get(index));
        return text.substring(label.length()).trim();
    }

    private String cleanHeaderLine(String line) {
        String text = line.trim();

        if (text.startsWith("/*")) text = text.substring(2).trim();
        if (text.startsWith("//")) text = text.substring(2).trim();
        if (text.startsWith("*")) text = text.substring(1).trim();

        return text;
    }

    private String getPrefix(String line) {
        String text = line.trim();

        if (text.startsWith("//")) return "// ";
        if (text.startsWith("*")) return " * ";
        if (text.startsWith("/*")) return "/* ";

        return "";
    }

    private void collectJavaFiles(File folder, ArrayList<File> javaFiles) {
        if (folder == null || !folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) collectJavaFiles(file, javaFiles);
            else if (file.getName().toLowerCase().endsWith(".java")) javaFiles.add(file);
        }
    }

    private boolean hasDirectQuestionFolder(File root) {
        return new File(root, "Q1").isDirectory()
                || new File(root, "Q2").isDirectory()
                || new File(root, "Q3").isDirectory();
    }

    private void moveQuestionFolder(File sourceRoot, File destinationRoot, String questionName) throws IOException {
        File source = new File(sourceRoot, questionName);
        if (!source.isDirectory()) return;

        File destination = new File(destinationRoot, questionName);
        Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private File getSingleTopLevelFolder(File root) {
        File[] children = root.listFiles();
        if (children == null) return null;

        File onlyFolder = null;
        int folderCount = 0;

        for (File child : children) {
            if (child.isDirectory()) {
                onlyFolder = child;
                folderCount++;
            }
        }

        return folderCount == 1 ? onlyFolder : null;
    }
}