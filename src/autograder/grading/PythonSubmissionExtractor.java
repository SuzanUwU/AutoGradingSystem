package autograder.grading;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Extract only expected answer files into an isolated grading directory.
 * For trusted demo submissions; not a sandbox for arbitrary untrusted code.
 */
public final class PythonSubmissionExtractor {
    private static final Set<String> ANSWERS = Set.of(
            "q1a.py", "q1b.py", "q2a.py", "q2b.py",
            "q3a.py", "q3b.py", "q3c.py", "q4.py");
    private static final long MAX_FILE_BYTES = 1024 * 1024;
    private PythonSubmissionExtractor() {}

    public static void extract(File archive, File destination) throws IOException {
        Files.createDirectories(destination.toPath());
        try (ZipFile zip = new ZipFile(archive)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String path = entry.getName().replace('\\', '/');
                if (path.startsWith("/") || path.matches("(?i)^[a-z]:.*")
                        || Arrays.asList(path.split("/")).contains("..")) {
                    throw new IOException("Unsafe ZIP entry: " + path);
                }
                String[] segments = path.split("/");
                String name = segments[segments.length - 1];
                if (!ANSWERS.contains(name)) continue;
                Path target = destination.toPath().resolve(name);
                if (Files.exists(target)) throw new IOException("Duplicate answer: " + name);
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    long total = 0;
                    while ((count = in.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_FILE_BYTES) throw new IOException("Answer too large: " + name);
                        out.write(buffer, 0, count);
                    }
                }
            }
        }
    }
}
