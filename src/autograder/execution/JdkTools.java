package autograder.execution;

import java.io.File;

/**
 * Locates JDK command-line tools (java, javac) so student code is compiled and run with the
 * same JDK that runs the grader, falling back to whatever is on the PATH.
 */
public final class JdkTools {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

    private JdkTools() {
    }

    /**
     * Returns the path of {@code toolName} in the current JDK's bin folder if it exists,
     * otherwise {@code toolName} itself so the operating system resolves it from the PATH.
     */
    public static String find(String toolName) {
        String fileName = WINDOWS ? toolName + ".exe" : toolName;
        File bundled = new File(new File(System.getProperty("java.home"), "bin"), fileName);
        return bundled.isFile() ? bundled.getPath() : toolName;
    }
}
