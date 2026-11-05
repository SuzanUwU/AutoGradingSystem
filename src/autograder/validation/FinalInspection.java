package autograder.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/** Snapshot of a submission after automatic corrections have run. */
    class FinalInspection {
        boolean structureIssue;
        boolean headerIssue;
        Map<String, File> questionFolders;
        final ArrayList<File> javaFiles = new ArrayList<>();
    }
