package autograder.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** Clears the temporary grading workspace before a new batch. */
class WorkspaceCleaner {
    private final File workspaceFolder;
    WorkspaceCleaner(File workspaceFolder) { this.workspaceFolder = workspaceFolder; }
    void cleanWorkspace() throws IOException {
        if (!workspaceFolder.exists()) {
            if (!workspaceFolder.mkdirs()) throw new IOException("Cannot create workspace folder.");
            return;
        }

        File[] files = workspaceFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            deleteDirectory(file.toPath());
        }
    }

    private void deleteDirectory(Path root) throws IOException {
        if (!Files.exists(root)) return;

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                deleteWithRetry(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                deleteWithRetry(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteWithRetry(Path path) throws IOException {
        final int maxAttempts = 40;
        final long waitMillis = 250;
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                File file = path.toFile();
                if (file.exists() && !file.canWrite()) file.setWritable(true);
                Files.deleteIfExists(path);
                return;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(waitMillis);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Workspace cleanup interrupted: " + path, interrupted);
                    }
                }
            }
        }

        throw new IOException("Cannot delete after " + maxAttempts + " attempts: " + path, lastException);
    }
}
