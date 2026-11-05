package autograder.testgen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Generates and verifies the Java assessment's optional testers. */
public final class GeneratedTesterService {
    /** Generates all parameterized testers from assessment CSV data. */
    public List<Path> generate() throws IOException {
        return new TesterGenerator(Path.of("assessment", "test-data"), Path.of("generated_testers")).generateAll();
    }

    /** Returns whether all expected generated testers exist. */
    public boolean isReady() {
        File folder = new File("generated_testers");
        if (!folder.isDirectory()) return false;
        for (String name : new String[] {"Q1aTester.java", "Q1bTester.java", "Q2aTester.java", "Q2bTester.java", "Q3Tester.java"}) {
            if (!new File(folder, name).isFile()) return false;
        }
        return true;
    }
}
