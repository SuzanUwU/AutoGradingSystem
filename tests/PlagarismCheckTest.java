package autograder.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Run with java -ea autograder.model.PlagarismCheckTest. */
public final class PlagarismCheckTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        check(PlagarismCheck.tokenize("String s = \"a // b\"; /* hi */")
                .equals(PlagarismCheck.tokenize("String s=\"a // b\";//bye")), "Java comments");
        check(!PlagarismCheck.tokenize("String s = \"a b\";")
                .equals(PlagarismCheck.tokenize("String s = \"ab\";")), "String contents preserved");
        check(!PlagarismCheck.tokenize("a++")
                .equals(PlagarismCheck.tokenize("a + +")), "Operator boundaries");
        check(score("a b c d e f g h i j", "a b c d e f g x y z") == 70, "70% score");
        check(score("a b c d e f g h i j", "a b c d e f g h y z") == 80, "80% score");
        check(score("a b", "a b c d") == 50 && score("a b c d", "a b") == 50, "Symmetric scoring");
        check(score("", "") == 0, "Empty similarity");
        check(score("a b c", "x y z") == 0, "Different answers");
        check(score("a b c", "a b c") == 100, "Exact similarity");

        String tests = "public static void main(String[] args) {"
                + "System.out.println(\"supplied test data\");".repeat(40) + "}";
        String a = javaAnswer("int a = 1; int b = 2;", tests);
        String b = javaAnswer("String text = \"unrelated\"; while (text.isEmpty()) { text += \"x\"; } return text;", tests);
        check(score(a, b) > 80, "Regression fixture dominated by shared main");
        check(PlagarismCheck.similarity(answer(a), answer(b)) < 80, "Shared main excluded");
        check(answer(javaAnswer("return null;", tests)).isEmpty(), "Null stub skipped");
        check(answer(javaAnswer("return 0.0;", tests)).isEmpty(), "Numeric stub skipped");
        check(answer("class Q1a { void main() {} }").isEmpty(), "Missing target not compared");
        check(answer(javaAnswer("String s = \"} // do not modify\"; return s;", tests))
                .contains("\"} // do not modify\""), "Braces/comment markers in strings preserved");
        check(answer(javaAnswer("return new ArrayList<String>();", tests)).isEmpty(), "Empty collection starter");
        Path root = Files.createTempDirectory("autograder-similarity-");
        try {
            writeZip(root, "1111-2222-alice", Map.of("alice/Q1/Q1a.java", a));
            writeZip(root, "bob", Map.of("bob/Q1a.java", b));
            check(run(root).get("alice").getStatus().equals("No match found"), "Shared main no false flag");
            writeZip(root, "bob", Map.of("Q1a.java", javaAnswer("int a=1; /* comment */ int b=2;", "void main() {}")));
            var results = run(root);
            check(results.get("alice").getStatus().equals("Suspected - review"), "Identical bodies still flagged");
            check(results.get("bob").getDetails().contains("100.000%")
                    && results.get("bob").getDetails().contains("alice"), "Both students get evidence");
            writeZip(root, "bob", Map.of("Q1a.java", javaAnswer("int a=3; int b=4;", tests)));
            check(run(root).get("alice").getStatus().equals("Suspected - review"), "Java exactly 80%");
            writeZip(root, "bob", Map.of("Q1a.java", javaAnswer("int c=3; int b=4;", tests)));
            check(run(root).get("alice").getStatus().equals("No match found"), "Java below 80%");
            reset(root);

            writeZip(root, "alice", Map.of("Q1a.java", javaAnswer("return null;", tests)));
            writeZip(root, "bob", Map.of("Q1a.java", javaAnswer("return null;", tests)));
            check(run(root).get("alice").getStatus().equals("Not compared"), "Shared stubs skipped");
            check(run(root).get("alice").getDetails().contains("starter"), "Skipped reason");
            reset(root);

            writeZip(root, "alice", Map.of("Q1a.java", a));
            writeZip(root, "bob", Map.of("Q1b.java", a));
            check(run(root).get("alice").getStatus().equals("Not compared"), "Different questions separate");
            reset(root);
            writeZip(root, "alice", Map.of("Helper.java", a, "Q1aTester.java", a));
            check(run(root).get("alice").getStatus().equals("Could not check"), "No helpers/testers");
            reset(root);

            Files.writeString(root.resolve("broken.zip"), "not a zip");
            writeZip(root, "alone", Map.of("Q1a.java", a));
            results = run(root);
            check(results.get("broken").getStatus().equals("Could not check"), "Corrupt archive");
            check(results.get("alone").getStatus().equals("Not compared"), "Single student");
            reset(root);
            writeZip(root, "alice", Map.of("a/Q1a.java", a, "b/Q1a.java", a));
            check(run(root).get("alice").getStatus().equals("Could not check"), "Duplicate question");
            reset(root);
            writeZip(root, "alice", Map.of("Q1a.java", a));
            writeZip(root, "1111-2222-alice", Map.of("Q1a.java", a));
            check(run(root).get("alice").getStatus().equals("Could not check"), "Duplicate student");
        } finally {
            reset(root);
            Files.delete(root);
        }
        System.out.println("Passed " + assertions + " similarity regression checks.");
        if (args.length > 0) {
            var samples = new PlagarismCheck().check(Path.of(args[0]));
            for (var entry : samples.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue().getStatus());
                if (entry.getValue().getStatus().equals("Suspected - review"))
                    System.out.println(entry.getValue().getDetails());
            }
        }
    }

    private static String javaAnswer(String body, String main) {
        return "import java.util.*; class Q1a { public Object getIsogramWords(Object inputs) {"
                + body + "} " + main + "}";
    }
    private static List<String> answer(String text) throws IOException {
        return PlagarismCheck.answerTokens(text, "q1a.java");
    }
    private static double score(String a, String b) {
        return PlagarismCheck.similarity(PlagarismCheck.tokenize(a), PlagarismCheck.tokenize(b));
    }
    private static Map<String, PlagarismCheck.Result> run(Path folder) throws IOException {
        return new PlagarismCheck().check(folder);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        assertions++;
    }
    private static void reset(Path folder) throws IOException {
        try (var files = Files.list(folder)) {
            for (Path file : files.toList()) Files.delete(file);
        }
    }
    private static void writeZip(Path folder, String user, Map<String, String> files) throws IOException {
        try (var zip = new ZipOutputStream(Files.newOutputStream(folder.resolve(user + ".zip")))) {
            for (var file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
