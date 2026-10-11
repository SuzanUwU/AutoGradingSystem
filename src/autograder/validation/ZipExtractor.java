package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor {

    public void extract(File zipFile, File destination) throws IOException {

        if (destination.exists()) {
            deleteDirectory(destination);
        }

        if (!destination.mkdirs() && !destination.exists()) {
            throw new IOException("Cannot create workspace folder.");
        }

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

    private void extractEntry(ZipEntry entry,
            ZipInputStream zipInputStream, File destination)
            throws IOException {

        File targetFile = new File(destination, entry.getName());

        String destinationPath = destination.getCanonicalPath();
        String targetPath = targetFile.getCanonicalPath();

        if (!targetPath.startsWith(
                destinationPath + File.separator)) {

            throw new IOException(
                    "Invalid ZIP entry: " + entry.getName());
        }

        if (entry.isDirectory()) {
            targetFile.mkdirs();
            return;
        }

        File parentFolder = targetFile.getParentFile();

        if (parentFolder != null) {
            parentFolder.mkdirs();
        }

        Files.copy(
                zipInputStream,
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteDirectory(File directory) throws IOException {

        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {

                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    deleteFile(file);
                }
            }
        }

        deleteFile(directory);
    }

    private void deleteFile(File file) throws IOException {

        if (!file.delete()) {
            throw new IOException(
                    "Cannot delete: " + file.getPath());
        }
    }
}