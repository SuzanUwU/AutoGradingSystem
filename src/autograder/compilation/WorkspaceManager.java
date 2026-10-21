package autograder.compilation;

import autograder.model.QuestionSpec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds an isolated grading folder for each student and question part.
 *
 * <p>Each folder holds the student's Java sources, then the trusted assessment resources and the
 * tester, copied in that order so trusted files always overwrite any student copies. Student
 * {@code .class} files are never copied, so stale or tampered bytecode cannot affect grading.
 */
public class WorkspaceManager {
    private static final String JAVA_EXTENSION = ".java";

    private final File gradingRoot;
    private final File testersFolder;
    private final File resourcesFolder;

    /**
     * @param gradingRoot     folder under which per-student, per-question workspaces are created
     * @param testersFolder   folder holding the trusted tester sources, e.g. assessment/testers
     * @param resourcesFolder folder holding trusted resources per question folder, e.g. assessment/resources/Q2
     */
    public WorkspaceManager(File gradingRoot, File testersFolder, File resourcesFolder) {
        this.gradingRoot = gradingRoot;
        this.testersFolder = testersFolder;
        this.resourcesFolder = resourcesFolder;
    }

    /**
     * Creates a fresh workspace at {@code gradingRoot/<username>/<questionId>} for one question part.
     *
     * @param studentRoot corrected submission folder containing Q1, Q2, Q3
     * @param username    student username, used as the workspace folder name
     * @param question    question part to prepare
     * @return the prepared workspace, listing any required student sources that are missing
     * @throws IOException if the workspace cannot be written or the trusted tester is missing
     */
    public QuestionWorkspace prepare(File studentRoot, String username, QuestionSpec question) throws IOException {
        File directory = resolveWorkspaceDirectory(username, question.getQuestionId());
        resetDirectory(directory);

        File studentFolder = findChildFolder(studentRoot, question.getFolderName());
        if (studentFolder != null) copyStudentSources(studentFolder, directory);
        copyTrustedResources(new File(resourcesFolder, question.getFolderName()), directory);
        copyTester(question, directory);

        List<String> missingSources = new ArrayList<>();
        for (String source : question.getRequiredSources()) {
            if (!new File(directory, source).isFile()) missingSources.add(source);
        }
        return new QuestionWorkspace(username, question.getQuestionId(), directory, missingSources);
    }

    private File resolveWorkspaceDirectory(String username, String questionId) throws IOException {
        File directory = new File(new File(gradingRoot, username), questionId);
        String rootPath = gradingRoot.getCanonicalPath() + File.separator;
        if (!directory.getCanonicalPath().startsWith(rootPath)) {
            throw new IOException("Workspace path escapes grading folder: " + username + "/" + questionId);
        }
        return directory;
    }

    private void copyStudentSources(File studentFolder, File directory) throws IOException {
        File[] files = studentFolder.listFiles(file -> file.isFile() && file.getName().endsWith(JAVA_EXTENSION));
        if (files == null) return;
        for (File file : files) copyInto(file, directory);
    }

    private void copyTrustedResources(File trustedFolder, File directory) throws IOException {
        File[] files = trustedFolder.listFiles(file -> file.isFile() && !file.isHidden());
        if (files == null) return;
        for (File file : files) {
            copyInto(file, directory);
        }
    }

    private void copyTester(QuestionSpec question, File directory) throws IOException {
        File tester = new File(testersFolder, question.getTesterSourceFile());
        if (!tester.isFile()) throw new IOException("Tester file not found: " + tester.getPath());
        copyInto(tester, directory);
    }

    private File findChildFolder(File parent, String folderName) {
        File[] folders = parent.listFiles(File::isDirectory);
        if (folders == null) return null;
        for (File folder : folders) {
            if (folder.getName().equalsIgnoreCase(folderName)) return folder;
        }
        return null;
    }

    private void copyInto(File file, File directory) throws IOException {
        Files.copy(file.toPath(), new File(directory, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void resetDirectory(File directory) throws IOException {
        Path path = directory.toPath();
        if (Files.exists(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                List<Path> sorted = new ArrayList<>();
                paths.sorted(Comparator.reverseOrder()).forEach(sorted::add);
                for (Path entry : sorted) Files.delete(entry);
            }
        }
        Files.createDirectories(path);
    }
}
