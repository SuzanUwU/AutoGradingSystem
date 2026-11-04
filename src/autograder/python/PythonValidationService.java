package autograder.python;

import autograder.model.SubmissionResult;
import autograder.service.AutoGradingService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Validates and safely normalizes PF1 submissions. Never executes student code. */
public final class PythonValidationService {
    private static final List<String> REQUIRED = List.of("q1a.py", "q1b.py", "q2a.py", "q2b.py", "q3a.py", "q3b.py", "q3c.py", "q4.py");
    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9._-]{2,79}");
    private static final Pattern NAME = Pattern.compile("(?im)^\\s*#\\s*Name\\s*:\\s*(.*?)\\s*$");
    private static final Pattern EMAIL = Pattern.compile("(?im)^\\s*#\\s*Email\\s*ID\\s*:\\s*(.*?)\\s*$");
    private final Path templateDirectory;
    private final Path correctedDirectory;

    public PythonValidationService(File templateDirectory) {
        this(templateDirectory, new File("output/is1704-corrected"));
    }
    public PythonValidationService(File templateDirectory, File correctedDirectory) {
        this.templateDirectory = templateDirectory.toPath();
        this.correctedDirectory = correctedDirectory.toPath();
    }

    public void processSubmissions(File directory, AutoGradingService.Listener listener) {
        File[] files = directory.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".zip"));
        if (files == null) files = new File[0];
        Arrays.sort(files, Comparator.comparing(File::getName));
        listener.onLog("IS1704: validating " + files.length + " ZIP submissions; originals will not be modified.");
        for (int i = 0; i < files.length; i++) {
            File zip = files[i];
            try {
                listener.onSubmissionFinished(validateAndCorrect(zip));
            } catch (Exception e) {
                listener.onLog("ERROR " + zip.getName() + ": " + e.getMessage());
                String user = zip.getName().replaceFirst("(?i)\\.zip$", "");
                listener.onSubmissionFinished(new SubmissionResult(user, "Failed", "Unknown", "Failed", e.toString(),
                    List.of(e.getMessage()), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(e.getMessage()), List.of()));
            }
            listener.onProgress(i + 1, files.length);
        }
    }

    private SubmissionResult validateAndCorrect(File file) throws IOException {
        String user = file.getName().replaceFirst("(?i)\\.zip$", "");
        List<String> structure = new ArrayList<>(), headers = new ArrayList<>(), anomalies = new ArrayList<>();
        List<String> q1 = new ArrayList<>(), q2 = new ArrayList<>(), q3 = new ArrayList<>();
        List<String> folderFix = new ArrayList<>(), nameFix = new ArrayList<>(), emailFix = new ArrayList<>(), open = new ArrayList<>();
        Map<String,byte[]> answers = new LinkedHashMap<>();
        Map<String,byte[]> otherFiles = new LinkedHashMap<>();
        boolean invalid = false, unsafe = false;
        if (!USERNAME.matcher(user).matches() || user.equalsIgnoreCase("your_email_id")) {
            structure.add("ZIP filename is not a valid student email ID: " + user);
            open.add("Verify ZIP owner against class roster");
        }
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > 200) throw new IOException("Too many ZIP entries");
                if (entry.isDirectory()) continue;
                String path = entry.getName().replace('\\', '/');
                if (path.startsWith("/") || path.matches("(?i)^[a-z]:.*") || Arrays.asList(path.split("/")).contains("..")) {
                    unsafe = true; structure.add("Unsafe ZIP entry: " + path); continue;
                }
                if (entry.getSize() > 1024 * 1024) { invalid = true; anomalies.add("Oversized file: " + path); continue; }
                byte[] bytes;
                try (InputStream input = zip.getInputStream(entry)) { bytes = input.readNBytes(1024 * 1024 + 1); }
                if (bytes.length > 1024 * 1024) { invalid = true; anomalies.add("Oversized file: " + path); continue; }
                String[] parts = path.split("/");
                String base = parts[parts.length-1].toLowerCase(Locale.ROOT);
                if (REQUIRED.contains(base)) {
                    if (answers.putIfAbsent(base, bytes) != null) { invalid = true; structure.add("Duplicate answer: " + base); }
                    if (parts.length != 2 || !parts[0].equals(user)) structure.add("Incorrect location: " + path);
                } else if (base.endsWith("_test.py")) {
                    otherFiles.putIfAbsent(base, bytes);
                } else if (!base.equals(".ds_store") && !path.startsWith("__MACOSX/")) {
                    anomalies.add("Unexpected file: " + path);
                }
            }
        }
        for (String question : REQUIRED) {
            byte[] bytes = answers.get(question);
            if (bytes == null) {
                invalid = true; structure.add("Missing required file: " + question);
                questionIssues(question, q1, q2, q3).add("Missing " + question);
                continue;
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            Path starter = templateDirectory.resolve(question);
            if (Files.isRegularFile(starter) && normalize(text).equals(normalize(Files.readString(starter)))) {
                anomalies.add("Unchanged starter implementation: " + question);
                questionIssues(question, q1, q2, q3).add("Unchanged " + question);
            }
            Matcher n = NAME.matcher(text), e = EMAIL.matcher(text);
            String name = n.find() ? n.group(1).trim() : "";
            String email = e.find() ? e.group(1).trim() : "";
            if (name.isEmpty() || name.equalsIgnoreCase("your name")) headers.add(question + ": missing Name header");
            if (email.isEmpty() || email.equalsIgnoreCase("your_email_id")) {
                headers.add(question + ": missing Email ID header");
                // Do not invent student identity; a blank email ID is flagged for review.
            } else if (!email.equalsIgnoreCase(user)) {
                headers.add(question + ": Email ID " + email + " disagrees with ZIP filename " + user);
            }
        }
        // Never silently change answers or identity information. Only normalize the ZIP folder layout.
        boolean canCorrect = !unsafe && !invalid && USERNAME.matcher(user).matches()
                && !user.equalsIgnoreCase("your_email_id") && !structure.isEmpty()
                && structure.stream().allMatch(s -> s.startsWith("Incorrect location:"));
        if (canCorrect) {
            Files.createDirectories(correctedDirectory);
            Path target = correctedDirectory.resolve(user + ".zip");
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target))) {
                for (Map.Entry<String,byte[]> item : answers.entrySet()) {
                    out.putNextEntry(new ZipEntry(user + "/" + item.getKey()));
                    out.write(item.getValue()); out.closeEntry();
                }
                for (Map.Entry<String,byte[]> item : otherFiles.entrySet()) {
                    out.putNextEntry(new ZipEntry(user + "/" + item.getKey()));
                    out.write(item.getValue()); out.closeEntry();
                }
            }
            folderFix.add("Normalized ZIP folder hierarchy to " + user + "/; saved " + target);
            structure.clear();
        }
        String status = invalid || unsafe ? "Failed" :
                (!headers.isEmpty() || !structure.isEmpty() || !anomalies.isEmpty() ? "Review" :
                (!folderFix.isEmpty() ? "Corrected" : "Valid"));
        open.addAll(structure); open.addAll(headers); open.addAll(anomalies);
        String details = "IS1704 — " + user + "\n" + String.join("\n", open) +
                (folderFix.isEmpty() ? "" : "\n" + String.join("\n", folderFix));
        return new SubmissionResult(user, structure.isEmpty() ? "Valid" : "Review",
                headers.isEmpty() ? "Valid" : "Review", status, details,
                structure, q1, q2, q3, folderFix, nameFix, emailFix, open, anomalies);
    }
    private static String normalize(String text) {
        return text.replace("\r\n", "\n").lines()
                .filter(line -> !line.matches("(?i)^\\s*#\\s*(name|email\\s*id)\\s*:.*"))
                .map(String::stripTrailing).reduce("", (a,b) -> a + "\n" + b).trim();
    }
    private static List<String> questionIssues(String q, List<String> q1, List<String> q2, List<String> q3) {
        if (q.startsWith("q1")) return q1;
        if (q.startsWith("q2")) return q2;
        return q3; // Q4 details also appear in the general issue list.
    }
}
