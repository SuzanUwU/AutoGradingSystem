# CS102 Auto Grading System

The **CS102 Auto Grading System** is a Java-based desktop application designed to automate the grading workflow for CS102 programming submissions.

The system supports batch processing of student submissions, submission validation and correction, compilation, automated tester execution, scoring, generated tester comparison, plagiarism checking, manual review, and result export.

The application is implemented using **Java**, **Swing**, and Object-Oriented Programming principles.

---

## Features

### 1. Batch Submission Processing

The system can process multiple student `.zip` submissions from the:

```text
submissions/
```

folder in a single grading session.

Each submission is processed independently so that an issue with one student's submission does not prevent the remaining submissions from being validated or graded.

---

### 2. Submission Validation

Before grading begins, the system validates each student submission.

Validation includes checks for:

- ZIP extraction
- Submission folder structure
- Required question folders
- Required Java source files
- Java file headers
- Student information
- Unexpected or missing files
- Submission anomalies

The validation results are displayed in the GUI for each student.

---

### 3. Submission Correction and Anomaly Detection

The system attempts to automatically correct recoverable submission issues.

Examples include:

- Incorrect folder hierarchy
- Extra wrapper folders
- Misplaced Java files
- Incorrect file locations
- Recoverable submission structure problems
- Java header inconsistencies

Corrections and unresolved anomalies are recorded so that the instructor can review what was changed.

The **Correction Summary** panel provides an overview of:

- Student
- Folder issues
- Name issues
- Email issues
- Open issues
- Anomalies
- Total fixes
- Review status

Detailed correction information can also be viewed for individual students.

---

### 4. Automated Java Compilation

After validation, student Java files are compiled automatically.

The grading system creates isolated workspaces for each question and student before compilation.

This prevents one student's files from interfering with another student's grading process.

Compilation errors are captured and reported in the grading results.

---

### 5. Professor's Original Testers

The system executes the professor-provided testers for the following assessment questions:

```text
Q1a
Q1b
Q2a
Q2b
Q3
```

Each question is compiled and graded independently.

The student's score for each question is calculated from the tester output.

The total score obtained using the professor's testers is displayed as the **Original Total**.

---

### 6. Generated Testers — Bonus Feature

The system can also generate additional testers for comparison with the professor's original testers.

Generated testers are available for:

```text
Q1a
Q1b
Q2a
Q2b
Q3
```

The workflow is:

1. Generate testers
2. Grade submissions using the generated testers
3. Compare the generated tester results with the original grading results

The generated tester score is shown separately as the **Generated Total**.

Generated tester results are intended for comparison and are not used as the official exported grade.

---

### 7. Edge Case Testing — Bonus Feature

Additional edge-case tests can be executed against student submissions.

These tests are stored inside:

```text
edgecases/
```

The edge-case feature helps identify implementations that may pass the standard testers but fail under additional input conditions.

---

### 8. Plagiarism Checking

The system includes plagiarism checking functionality to assist with identifying similarities between student submissions.

Plagiarism results are displayed in the student results table under:

```text
Plagiarism Check
```

The plagiarism result is intended to assist manual review rather than automatically determine academic misconduct.

---

### 9. Manual Review and Scoring

Submissions requiring instructor attention can be marked for review.

The system provides:

- Review status
- Correction details
- Submission details
- Manual score functionality

This allows graders to review unusual or unresolved submissions before finalizing results.

---

### 10. Result Export

Grading results can be exported after grading is completed.

Generated files are stored in:

```text
output/
```

Supported outputs include:

- CSV grading results
- Detailed grading information
- PDF grading report
- Correction history
- Edge-case reports

Example output files include:

```text
gradedscores.csv
gradedscores_report.pdf
```

---

## User Interface

The graphical user interface provides the complete grading workflow.

### Main Workflow

```text
Select Submissions Folder
        ↓
Validate Submissions
        ↓
Review Corrections / Anomalies
        ↓
Start Grading using Original Testers
        ↓
Generate Testers (Optional)
        ↓
Grade using Generated Testers (Optional)
        ↓
Run Edge Cases (Optional)
        ↓
Review Results
        ↓
Export Results
```

The student table displays:

- Student
- Structure
- Headers
- Status
- Grading
- Original Total
- Generated Total
- Plagiarism Check
- Review Status

The interface supports both horizontal and vertical scrolling for larger batches of submissions.

---

## Project Structure

```text
AutoGradingSystem/
│
├── src/
│   └── autograder/
│       ├── compilation/
│       ├── execution/
│       ├── grading/
│       ├── model/
│       ├── reporting/
│       ├── testgen/
│       ├── ui/
│       ├── validation/
│       └── Main.java
│
├── assessment/
│   └── java/
│       ├── resources/
│       ├── test-data/
│       └── testers/
│
├── submissions/
│
├── generated_testers/
│
├── edgecases/
│
├── workspace/
│
├── temp_workspaces/
│
├── output/
│
├── config/
│
├── lib/
│
├── classes/
│
├── compile.bat
├── run.bat
├── compile.sh
├── run.sh
└── README.md
```

---

## Requirements

The project requires a Java Development Kit (**JDK**) with both `java` and `javac` available.

Check your installation using:

```bash
java -version
javac -version
```

The required third-party Java libraries are already included in the:

```text
lib/
```

folder.

These include Apache PDFBox dependencies used for PDF report generation.

---

# How to Run

## Windows

Open Command Prompt or PowerShell inside the project folder.

### Step 1 — Compile

```powershell
.\compile.bat
```

A successful compilation should display:

```text
Compilation successful.
```

### Step 2 — Run

```powershell
.\run.bat
```

The **CS102 Auto Grading System** GUI should open.

---

## macOS / Linux

### Step 1 — Compile

```bash
bash compile.sh
```

### Step 2 — Run

```bash
bash run.sh
```

---

# How to Use

### Step 1 — Prepare submissions

Place student ZIP files inside:

```text
submissions/
```

Alternatively, use the **Browse** button in the GUI to select another submissions folder.

### Step 2 — Validate

Click:

```text
Validate Submissions
```

The system will inspect all submissions and display their validation status.

### Step 3 — Review corrections

Check the **Correction Summary** and **Full Details** panels for automatically corrected issues or anomalies requiring attention.

### Step 4 — Start normal grading

Click:

```text
Start Grading (Original Testers)
```

The system will compile and execute the professor's original testers.

### Step 5 — Generated tester comparison (Optional)

Click:

```text
Generate Testers
```

followed by:

```text
Grade with Generated Testers
```

The generated results can then be compared against the original tester scores.

### Step 6 — Additional checks

Optional actions include:

```text
Run Edge Cases
Review Issues
Manual Score
```

### Step 7 — Export

Click:

```text
Export Results
```

to generate the grading output files.

---

## Assessment Configuration

The current Java assessment contains:

| Question | Maximum Marks |
|---|---:|
| Q1a | 3 |
| Q1b | 3 |
| Q2a | 5 |
| Q2b | 5 |
| Q3 | 4 |
| **Total** | **20** |

The assessment files and trusted resources are stored under:

```text
assessment/java/
```

---

## Design Overview

The system separates its responsibilities into several packages.

| Package | Responsibility |
|---|---|
| `validation` | Submission extraction, inspection, validation and correction |
| `compilation` | Preparing workspaces and compiling Java files |
| `execution` | Executing programs and managing external processes |
| `grading` | Running testers and calculating scores |
| `testgen` | Generating additional testers |
| `reporting` | CSV, PDF and correction report generation |
| `model` | Data objects used throughout the grading system |
| `ui` | Java Swing user interface and controllers |

This structure keeps validation, grading, reporting, and interface logic separated and easier to maintain.

---

## Important Notes

- Original professor testers are used for the official grading result.
- Generated testers are used only as an additional comparison.
- Automatically detected anomalies should still be reviewed when necessary.
- Plagiarism checking is intended as an indicator for manual review.
- Student submissions are processed separately to prevent one failed submission from stopping the entire batch.
- Temporary grading workspaces are isolated from the original student submission files.

---

## Contributors

| Member | Contribution |
|---|---|
| Thet Htar Nwe | Main GUI & Workflow Integration; Batch Processing & Error Handling; Validation Workflow Integration; Progress Tracking & System Log; Submission Correction & Correction History |
| Thet Htar Nwe — Bonus | Generated Tester Files; 50 Additional Dummy Submissions |
| Team Member 2 | Add contribution here |
| Team Member 3 | Add contribution here |
| Team Member 4 | Add contribution here |
| Team Member 5 | Add contribution here |
| Team Member 6 | Add contribution here |
---

## Module

**CS102 — Programming Fundamentals II**

Singapore Management University

---

## Project Status

The system currently supports the complete Java grading workflow including:

- Submission validation
- Submission correction
- Batch processing
- Java compilation
- Original tester grading
- Generated tester comparison
- Edge-case testing
- Plagiarism checking
- Manual review
- Score calculation
- CSV export
- PDF report generation

Further improvements may focus on extending assessment support and improving automation and usability.