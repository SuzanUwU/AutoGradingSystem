import autograder.model.SubmissionResult;
import autograder.service.AutoGradingService;
import autograder.validation.TemplateComparator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Basic tests for accidental submission of an original template. */
public class TemplateComparatorTest {
    private static final File TEMPLATE = new File("assessment/java/template");
    private static final String ISSUE = "Original resource folder submitted";
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        if (!new File(TEMPLATE, "Q1/Q1a.java").isFile()) {
            throw new IOException("Run this test from the project root.");
        }
        File temp = Files.createTempDirectory("template-test").toFile();
        TemplateComparator comparator = new TemplateComparator(TEMPLATE);

        File exact = copyTemplate(new File(temp, "exact"));
        check("Original template", comparator.isUnmodifiedTemplate(exact));

        File modified = copyTemplate(new File(temp, "modified"));
        Files.write(new File(modified, "Q1/Q1a.java").toPath(),
                "\nclass Extra {}\n".getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.APPEND);
        check("Modified Java code", !comparator.isUnmodifiedTemplate(modified));

        File extra = copyTemplate(new File(temp, "extra"));
        Files.write(new File(extra, "Q1/Helper.java").toPath(),
                "class Helper {}".getBytes(StandardCharsets.UTF_8));
        check("Extra Java file", !comparator.isUnmodifiedTemplate(extra));

        File empty = new File(temp, "empty");
        Files.createDirectories(empty.toPath());
        check("Empty submission", !comparator.isUnmodifiedTemplate(empty));
        check("Missing folder", !comparator.isUnmodifiedTemplate(new File(temp, "missing")));

        // Integration: a template ZIP should produce an issue and status Review.
        File zipFolder = new File(temp, "zips");
        Files.createDirectories(zipFolder.toPath());
        zip(exact, new File(zipFolder, "2023-2024-tmpl.pure.2024.zip"));
        zip(modified, new File(zipFolder, "2023-2024-tmpl.changed.2024.zip"));

        Map<String, SubmissionResult> results = new LinkedHashMap<>();
        new AutoGradingService(new File(temp, "workspace"))
                .processSubmissions(zipFolder, new AutoGradingService.Listener() {
                    @Override public void onLog(String message) { }
                    @Override public void onProgress(int processed, int total) { }
                    @Override public void onSubmissionFinished(SubmissionResult result) {
                        results.put(result.getUsername(), result);
                    }
                });
        SubmissionResult original = results.get("tmpl.pure.2024");
        SubmissionResult changed = results.get("tmpl.changed.2024");
        check("Original ZIP gets Review", original != null
                && original.getSubmissionIssues().contains(ISSUE)
                && "Review".equals(original.getOverallStatus()));
        check("Modified ZIP has no template warning", changed != null
                && !changed.getSubmissionIssues().contains(ISSUE));

        System.out.printf("RESULT: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    private static File copyTemplate(File destination) throws IOException {
        try (Stream<Path> paths = Files.walk(TEMPLATE.toPath())) {
            for (Path source : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Path target = destination.toPath().resolve(TEMPLATE.toPath().relativize(source));
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
        }
        return destination;
    }

    private static void zip(File folder, File output) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output.toPath()));
             Stream<Path> paths = Files.walk(folder.toPath())) {
            for (Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
                String relative = folder.toPath().relativize(file).toString().replace(File.separatorChar, '/');
                out.putNextEntry(new ZipEntry(relative));
                Files.copy(file, out);
                out.closeEntry();
            }
        }
    }

    private static void check(String name, boolean success) {
        System.out.println((success ? "PASS  " : "FAIL  ") + name);
        if (success) passed++; else failed++;
    }
}
