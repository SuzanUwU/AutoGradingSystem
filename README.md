# CS102 Auto Grading System

The CS102 Auto Grading System is a Java application designed to automate the
grading of Java programming submissions.

The system will support submission validation, compilation, tester execution,
score calculation, and result generation.

## Current Progress

The following features are currently implemented:

### Student ZIP Extraction

The program reads a student ZIP file from the `submissions` folder and
extracts it into the `workspace` folder.

Example:

```text
submissions/
    2023-2024-chee.teo.2022.zip
```

### Submission Validation

The program currently:

- Finds the `Q1`, `Q2`, and `Q3` folders.
- Supports submissions with an additional outer folder.
- Checks for the required Java source files.

Required files:

```text
Q1/
    Q1a.java
    Q1b.java

Q2/
    Q2a.java
    Q2b.java

Q3/
    Q3.java
```

For example, the following structures can be detected:

```text
Q1/
Q2/
Q3/
```

```text
01400003/
    Q1/
    Q2/
    Q3/
```

```text
RenameToYourStudentID/
    Q1/
    Q2/
    Q3/
```

The current program flow is:

```text
Student ZIP
    ↓
Extract submission
    ↓
Find Q1 / Q2 / Q3
    ↓
Check required Java files
    ↓
Display validation results
```

## How to Run

### Requirements

Make sure Java JDK is installed.

Check using:

```text
java -version
javac -version
```

### Windows

1. Open Command Prompt in the project folder.

2. Place one student ZIP file inside:

```text
submissions/
```

3. Compile the project:

```text
compile.bat
```

You should see:

```text
Compilation successful.
```

4. Run the project:

```text
run.bat
```

Example output:

```text
Processing: 2023-2024-chee.teo.2022.zip

Extracting submission...
Extraction successful.

Searching for question folders...
[OK] Q1 found
    [OK] Q1a.java
    [OK] Q1b.java
[OK] Q2 found
    [OK] Q2a.java
    [OK] Q2b.java
[OK] Q3 found
    [OK] Q3.java
```

At the current stage, the program processes the first ZIP file found in the
`submissions` folder. For testing, keep one ZIP file in the folder at a time.

### macOS / Linux

```text
bash compile.sh
bash run.sh
```

## Project Folders

- `src/autograder` - Java source code.
- `assessment/testers` - Instructor-provided tester files.
- `assessment/resources` - Supporting grading files.
- `submissions` - Student ZIP submissions.
- `workspace` - Extracted student submissions.
- `classes` - Compiled application classes.
- `output` - Generated grading results.
- `tests` - Project tests.

## Next Steps

The next features to be implemented include:

- Copying tester files into the grading workspace.
- Compiling student code with tester files.
- Running tester programs.
- Handling compilation/runtime errors and timeouts.
- Calculating student scores.
- Processing multiple submissions.
- Exporting results to the provided CSV score sheet.

## Notes

- Do not commit student ZIP submissions to GitHub.
- Do not commit generated `.class` files or workspace contents.
- Update this README as new features are completed.
