package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor {
    public void extract(File zipFile, File destination) throws IOException {
        if (destination.exists()) deleteDirectory(destination);
        if (!destination.mkdirs() && !destination.exists()) throw new IOException("Cannot create workspace folder.");

        InputStream inputStream = Files.newInputStream(zipFile.toPath());
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        try {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                extractEntry(entry, zipInputStream, destination);
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        } finally {
            zipInputStream.close();
        }
    }

    private void extractEntry(ZipEntry entry, ZipInputStream zipInputStream, File destination) throws IOException {
        File targetFile = new File(destination, entry.getName());
        String destinationPath = destination.getCanonicalPath();
        String targetPath = targetFile.getCanonicalPath();

        if (!targetPath.startsWith(destinationPath + File.separator)) throw new IOException("Invalid ZIP entry: " + entry.getName());

        if (entry.isDirectory()) {
            if (!targetFile.mkdirs() && !targetFile.exists()) throw new IOException("Cannot create directory: " + targetFile.getPath());
            return;
        }

        File parentFolder = targetFile.getParentFile();
        if (parentFolder != null) parentFolder.mkdirs();
        Files.copy(zipInputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteDirectory(File directory) throws IOException {
        Path root = directory.toPath();
        if (!Files.exists(root)) return;

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                deleteWithRetry(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException exception) throws IOException {
                if (exception != null) throw exception;
                deleteWithRetry(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteWithRetry(Path path) throws IOException {
        final int maxAttempts = 5;
        final long waitTimeMillis = 200;
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
                        Thread.sleep(waitTimeMillis);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Deletion interrupted: " + path, interrupted);
                    }
                }
            }
        }

        throw new IOException("Cannot delete after " + maxAttempts + " attempts: " + path, lastException);
    }
}