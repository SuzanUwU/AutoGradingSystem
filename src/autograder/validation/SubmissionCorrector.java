package autograder.validation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
public class SubmissionCorrector {
    private final Consumer<String> logger;
    // for details csv
    private final Consumer<String> issueRecorder;

    public SubmissionCorrector(Consumer<String> logger) {
        // this(System.out::println);
        this(logger, null);
    }

    public SubmissionCorrector(Consumer<String> logger, Consumer<String> issueRecorder) {
        this.logger = logger == null ? System.out::println : logger;
        this.issueRecorder = issueRecorder == null ? issue -> {} : issueRecorder;
    }
    public File correctStructure(File extractedRoot, String expectedUsername) throws IOException {
        File expectedFolder = new File(extractedRoot, expectedUsername);
        if (expectedFolder.isDirectory()) {
            logger.accept("[OK] Username folder is correct: " + expectedUsername);
            return expectedFolder;
        }
        if (hasDirectQuestionFolder(extractedRoot)) {
            if (!expectedFolder.mkdirs() && !expectedFolder.exists()) {
                throw new IOException("Cannot create corrected folder: " + expectedFolder.getPath());
            }
            for (String question : new String[] {"Q1", "Q2", "Q3"}) {
                moveQuestionFolder(extractedRoot, expectedFolder, question);
            }
            logger.accept("[CORRECTED] Added missing username folder: " + expectedUsername);
            issueRecorder.accept("Missing Username Folder");
            return expectedFolder;
        }
        File singleFolder = getSingleTopLevelFolder(extractedRoot);
        if (singleFolder != null) {
            String oldName = singleFolder.getName();
            Files.move(singleFolder.toPath(), expectedFolder.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.accept("[CORRECTED] Renamed top-level folder from " + oldName + " to " + expectedUsername);
            issueRecorder.accept("Misnamed Top-Level Folder");
            return expectedFolder;
        }
        logger.accept("[WARNING] Folder structure could not be corrected automatically.");
        return extractedRoot;
    }
    public void correctHeaders(File submissionFolder, String expectedUsername) throws IOException {
        ArrayList<File> javaFiles = new ArrayList<>();
        collectJavaFiles(submissionFolder, javaFiles);
        String trustedName = findTrustedName(javaFiles, expectedUsername);
        boolean corrected = false;
        for (File javaFile : javaFiles) {
            if (correctHeader(javaFile, expectedUsername, trustedName)) corrected = true;
        }
        if (!corrected) logger.accept("[OK] No Java header corrections needed.");
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
        if (nameIndex == -1 && emailIndex == -1) {
            ArrayList<String> header = new ArrayList<>();
            header.add("/*");
            header.add(" * Name: " + (trustedName == null ? "" : trustedName));
            header.add(" * Email ID: " + expectedUsername);
            header.add(" */");
            header.add("");
            header.addAll(lines);
            if (trustedName == null) logger.accept("[WARNING] Cannot determine student name for " + javaFile.getName());
            else logger.accept("[CORRECTED] Added Name to " + javaFile.getName());
            logger.accept("[CORRECTED] Added Email ID to " + javaFile.getName());
            Files.write(javaFile.toPath(), header);
            return true;
        }
        if (nameMissing) {
            if (trustedName == null) {
                logger.accept("[WARNING] Cannot determine student name for " + javaFile.getName());
            } else {
                if (nameIndex >= 0) lines.set(nameIndex, getPrefix(lines.get(nameIndex)) + "Name: " + trustedName);
                else lines.add(emailIndex, " * Name: " + trustedName);
                logger.accept("[CORRECTED] Added Name to " + javaFile.getName());
                changed = true;
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
            logger.accept("[CORRECTED] Added Email ID to " + javaFile.getName());
            changed = true;
        } else if (fullEmail) {
            emailIndex = findHeaderLine(lines, "Email ID:");
            lines.set(emailIndex, getPrefix(lines.get(emailIndex)) + "Email ID: " + expectedUsername);
            logger.accept("[CORRECTED] Email ID in " + javaFile.getName() + ": " + expectedUsername);
            changed = true;
        }
        if (changed) Files.write(javaFile.toPath(), lines);
        return changed;
    }
    private boolean isStudentEmail(String emailId, String expectedUsername) {
        return emailId != null && (emailId.equalsIgnoreCase(expectedUsername)
                || emailId.equalsIgnoreCase(expectedUsername + "@computing.smu.edu.sg"));
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
        return cleanHeaderLine(lines.get(index)).substring(label.length()).trim();
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
        Files.move(source.toPath(), new File(destinationRoot, questionName).toPath(), StandardCopyOption.REPLACE_EXISTING);
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
