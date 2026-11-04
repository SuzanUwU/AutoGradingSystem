package autograder.validation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects a submission that is just the original resource folder: every Java file the student
 * submitted is identical to the instructor's template once comments and whitespace are ignored.
 */
public class TemplateComparator {
    private static final String JAVA_EXTENSION = ".java";
    private static final String MAC_METADATA_FOLDER = "__MACOSX";
    private static final String BLOCK_COMMENT = "(?s)/\\*.*?\\*/";
    private static final String LINE_COMMENT = "//[^\\r\\n]*";
    private static final String WHITESPACE = "\\s+";

    private final File templateRoot;
        /**
     * @param templateRoot folder holding the original Java files, e.g. assessment/template/Q1/Q1a.java
     */
    public TemplateComparator(File templateRoot) {
        this.templateRoot = templateRoot;
    }
    public boolean isUnmodifiedTemplate(File studentRoot) throws IOException{
        List<File> studentFiles = new ArrayList<>();
        collectJavaFiles(studentRoot, studentFiles); //find all the the java files that student submitted
        if (studentFiles.isEmpty()){
            return false; //submit empty folder, no warning(go to person2)
        }
        for (File studentFile : studentFiles){
            File templateFile = new File(templateRoot, relativePath(studentRoot,studentFile));
            if(!templateFile.isFile()) return false; // new file that is not existed in original folder
            if (!normalize(read(studentFile)).equals(normalize(read(templateFile)))) return false;//one file was modified

        }
        return true;
    }
    private void collectJavaFiles(File folder, List<File> javaFiles){
        File[] files = folder.listFiles();
        if (files == null) return; // not a folder or cannot be read
        for(File file: files){
            if (file.isDirectory()){
                if (file.getName().equals(MAC_METADATA_FOLDER)) continue; // macOS zip metadata, not student code
                collectJavaFiles(file, javaFiles);//recursive to the subfolder
            }
            else if (file.getName().endsWith(JAVA_EXTENSION)){
                javaFiles.add(file);
        }
        }
        
    }

    /** Removes block comments, line comments and all whitespace so only the code itself is compared. */
    String normalize(String source) {
        return source.replaceAll(BLOCK_COMMENT, "") //  remove /* ... */ in case student only fill in the name and email infomation
                    .replaceAll(LINE_COMMENT, "") // remove // ... in case student only adds a one-line comment
                    .replaceAll(WHITESPACE, "");
    }

    private String relativePath(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    private String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

}
