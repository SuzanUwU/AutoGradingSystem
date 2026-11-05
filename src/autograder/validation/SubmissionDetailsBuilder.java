package autograder.validation;

import java.io.*;
import java.util.*;

/** Builds the detailed human-readable validation report shown in the GUI. */
class SubmissionDetailsBuilder {
    String buildDetails(File zipFile, String username, File root, String structureStatus, String headerStatus,
            String overallStatus, FinalInspection inspection, SubmissionState state, SubmissionValidator validator) throws IOException {
        StringBuilder details = new StringBuilder();
        details.append("STUDENT\nUsername: ").append(username)
                .append("\nOverall status: ").append(overallStatus).append("\n");
        details.append("\nSUBMISSION\n[OK] ZIP filename identifies: ").append(username)
                .append("\nZIP file: ").append(zipFile.getName())
                .append("\nUsername folder: ").append(structureStatus)
                .append("\nWorkspace path: ").append(root.getPath()).append("\n");
        details.append("\nQUESTION FOLDERS\n");
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            details.append(entry.getValue() == null ? "[MISSING] " : "[OK] ")
                    .append(entry.getKey()).append("\n");
        }
        details.append("\nREQUIRED JAVA FILES\n");
        for (Map.Entry<String, File> entry : inspection.questionFolders.entrySet()) {
            if (entry.getValue() == null) continue;
            for (String fileName : SubmissionFileUtils.getRequiredFiles(entry.getKey())) {
                File javaFile = new File(entry.getValue(), fileName);
                details.append(javaFile.isFile() ? "[OK] " : "[MISSING] ")
                        .append(entry.getKey()).append("/").append(fileName).append("\n");
            }
        }
        details.append("\nJAVA HEADERS - FINAL STATE\n");
        if (inspection.javaFiles.isEmpty()) {
            details.append("[MISSING] No Java files found\n");
        } else {
            for (File javaFile : inspection.javaFiles) {
                String path = root.toPath().relativize(javaFile.toPath()).toString();
                String[] header = validator.getHeaderValues(javaFile);
                boolean valid = validator.hasValidHeader(javaFile, username);
                details.append(valid ? "[OK] " : "[REVIEW] ").append(path).append("\n")
                        .append("    Name: ").append(SubmissionFileUtils.valueOrMissing(header[0])).append("\n")
                        .append("    Email ID: ").append(SubmissionFileUtils.valueOrMissing(header[1])).append("\n");
            }
        }
        details.append("\nSUMMARY\nStructure: ").append(structureStatus)
                .append("\nHeaders: ").append(headerStatus).append("\n")
                .append("\nCHANGES / WARNINGS\n");
        if (state.notableMessages.isEmpty()) details.append("None\n");
        else for (String message : state.notableMessages) details.append(message).append("\n");
        return details.toString();
    }
}
