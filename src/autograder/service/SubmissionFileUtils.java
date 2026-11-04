package autograder.service;

import java.io.File;
import java.util.*;

/** Shared CS102 file lookup and status formatting helpers. */
class SubmissionFileUtils {
    private static final String ZIP_EXTENSION = ".zip";
    static String getQuestionForFile(File javaFile, File root) {
        try {
            String relativePath = root.toPath().relativize(javaFile.toPath()).toString().replace("\\", "/");
            String[] parts = relativePath.split("/");
            for (String part : parts) {
                if (part.equalsIgnoreCase("Q1")) return "Q1";
                if (part.equalsIgnoreCase("Q2")) return "Q2";
                if (part.equalsIgnoreCase("Q3")) return "Q3";
            }
        } catch (IllegalArgumentException e) {
            System.out.println("file not inside submission root");
        }
        return null;
    }

    static String[] getRequiredFiles(String question) {
        if (question.equals("Q1")) return new String[] {"Q1a.java", "Q1b.java"};
        if (question.equals("Q2")) return new String[] {"Q2a.java", "Q2b.java"};
        if (question.equals("Q3")) return new String[] {"Q3.java"};
        return new String[0];
    }

    static String getOverallStatus(String structureStatus, String headerStatus) {
        if (structureStatus.equals("Review") || headerStatus.equals("Review")) return "Review";
        if (structureStatus.equals("Corrected") || headerStatus.equals("Corrected")) return "Corrected";
        return "Valid";
    }

    static String valueOrMissing(String value) {
        return value == null || value.isEmpty() ? "MISSING" : value;
    }

    static void collectJavaFiles(File folder, ArrayList<File> javaFiles) {
        if (folder == null || !folder.isDirectory()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) collectJavaFiles(file, javaFiles);
            else if (file.getName().toLowerCase().endsWith(".java")) javaFiles.add(file);
        }
    }

    static File[] findZipFiles(File folder) {
        if (folder == null || !folder.isDirectory()) return new File[0];
        File[] files = folder.listFiles(SubmissionFileUtils::isZipFile);
        if (files == null) return new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    static boolean isZipFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(ZIP_EXTENSION);
    }

    static String removeZipExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ZIP_EXTENSION.length());
    }

    static String getUsernameFromFileName(String fileName) {
        String name = fileName.toLowerCase().endsWith(ZIP_EXTENSION) ? removeZipExtension(fileName) : fileName;
        return name.replaceFirst("^\\d{4}-\\d{4}-", "");
    }
}
