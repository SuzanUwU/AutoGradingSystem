# Auto Grading System

The **Auto Grading System** is a Java Swing desktop application for validating, grading, reviewing, and exporting programming assessment submissions.

The current application supports two assessment modes:

- **CS102 — Java**
- **IS1704 — Python**

The system is designed around a modular Object-Oriented structure so that validation, compilation, execution, grading, reporting, tester generation, models, and GUI responsibilities are separated into dedicated packages.

---

## Main Features

### 1. Batch Submission Processing

The application can process multiple student `.zip` submissions in one run.

Each submission is handled independently so that a problem with one student does not stop the rest of the batch.

The default input folder is:

```text
submissions/
├── java/
│   ├── original/
│   └── batch100/
└── python/
```

A different submissions folder can also be selected through the GUI using **Browse**.

---

### 2. Java and Python Assessment Modes

The assessment mode can be selected from the GUI.

#### CS102 — Java

Supports:

- submission validation and safe correction
- Java compilation and execution
- grading with professor-provided testers
- parameterized generated testers
- edge-case testing
- Java plagiarism similarity checking
- validation and grading review
- manual score adjustment
- CSV and PDF export

#### IS1704 — Python

Supports:

- ZIP and file validation
- required Python file checks
- student header checks
- starter-template comparison
- safe folder-layout normalization when eligible
- Python grading using the assessment test suite
- CSV and PDF export

Python validation does **not** silently modify student answers or identity information. Only eligible ZIP folder layouts are normalized into a corrected copy.

---

## Submission Validation

### Java Validation

Java validation is coordinated by `JavaValidationService` and separated across specialised classes such as:

- `ZipExtractor`
- `SubmissionValidator`
- `SubmissionCorrector`
- `TemplateComparator`
- `SubmissionInspector`
- `WorkspaceCleaner`
- `SubmissionDetailsBuilder`

The Java validation workflow checks:

- ZIP extraction
- username/submission folder structure
- required question folders
- required Java source files
- Name and Email ID headers
- unchanged starter/template submissions
- unresolved issues after correction

Recoverable structure and header problems are corrected only in the extracted workspace copy. The original student ZIP is not modified.

### Python Validation

`PythonValidationService` checks:

- ZIP safety and structure
- required Python files
- duplicate answer files
- Name and Email ID headers
- unchanged starter implementations
- unexpected files and other anomalies

When safe, an incorrect Python ZIP folder hierarchy can be normalized into:

```text
workspace/python/
```

---

## Validation Statuses

| Status | Meaning |
|---|---|
| `Valid` | No correction or instructor review is required |
| `Corrected` | Recoverable issues were corrected successfully |
| `Review` | Validation completed, but unresolved issues still need instructor judgement |
| `Failed` | The submission could not be processed successfully at the validation stage |

Examples of validation review issues include unresolved structure problems, missing required files, invalid headers, or detected anomalies.

---

## Submission Correction and Correction Summary

For Java submissions, the system can safely correct recoverable issues such as:

- incorrect top-level folder structure
- missing username folder
- recoverable Java file placement problems
- missing or inconsistent Name / Email ID headers when they can be corrected safely

The **Correction Summary** records information such as:

- Folder fixes
- Name fixes
- Email fixes
- Open issues
- Anomalies
- Total fixes
- Review status

Detailed information for an individual student is shown in the GUI without changing the original ZIP submission.

---

## Template Submission Detector — Bonus Feature

Java submissions are compared with the original assessment template before header correction.

`TemplateComparator` compares the submitted Java files against:

```text
assessment/java/template/
```

Comments and whitespace are ignored during comparison.

If the student's submitted Java files still match the original template, the submission is recorded as an anomaly and its overall validation status is set to **Review** for instructor judgement.

---

## Java Compilation and Safe Execution

After validation, Java grading prepares an isolated temporary workspace for each student and question.

The Java grading pipeline uses:

- `WorkspaceManager`
- `JavaCompilerService`
- `ProgramExecutor`
- `ProcessRunner`
- `QuestionGrader`
- `ScoreParser`

Key safeguards include:

- isolated question workspaces
- required-source checks
- compilation error capture
- separate JVM processes
- execution timeouts
- process termination for programs that exceed the time limit
- bounded output collection

This prevents one student's program from terminating, freezing, or interfering with the main grading application.

---

## Professor's Original Java Testers

The official Java testers are stored in:

```text
assessment/java/testers/
```

The current Java assessment contains:

| Question | Maximum Marks |
|---|---:|
| Q1a | 3 |
| Q1b | 3 |
| Q2a | 5 |
| Q2b | 5 |
| Q3 | 4 |
| **Total** | **20** |

Each question is prepared, compiled, executed, and scored independently.

The official Java result is shown as **Original Total**.

---

## Parameterized Tester Generation — Bonus Feature

Additional Java testers can be generated from configurable CSV test data.

Parameter files are stored in:

```text
assessment/java/test-data/
```

For example:

```text
assessment/java/test-data/Q1a.csv
```

The workflow is:

```text
CSV test parameters
        ↓
TesterGenerator
        ↓
generated_testers/*.java
        ↓
Grade with Generated Testers
        ↓
Generated Total
```

`GeneratedTesterService` uses `TesterGenerator` to create:

```text
generated_testers/Q1aTester.java
generated_testers/Q1bTester.java
generated_testers/Q2aTester.java
generated_testers/Q2bTester.java
generated_testers/Q3Tester.java
```

The generated testers use the same Java grading pipeline as the professor's original testers.

Generated results are displayed separately as **Generated Total** and are intended for comparison only. They are **not** used as the official exported grade.

---

## Edge Case Testing — Bonus Feature

Additional Java edge-case tests are stored in:

```text
edgecases/
```

They can be run after normal grading to test student implementations against additional inputs such as unusual values, null-related cases, or other cases outside the standard tester set.

Edge-case results do not change the official score. They can be included in the detailed PDF report for instructor review.

---

## Java Plagiarism Similarity Check — Bonus Feature

Java validation also runs a similarity check using the original student ZIP submissions.

`PlagiarismCheck`:

- examines supported Java answer files only
- extracts the relevant answer method for each question
- tokenizes the answer logic
- compares students answering the same question
- calculates normalized token-based edit-distance similarity

A similarity of **80% or above** is reported as:

```text
Suspected - review
```

The plagiarism result is only an indicator for instructor review. It does **not** automatically determine academic misconduct or change a student's grade.

---

## Grading Status and Review Issues

Java grading can produce the following grading states:

| Grading Status | Meaning |
|---|---|
| `Not graded` | Grading has not started |
| `Graded` | Grading completed normally |
| `Manual` | At least one question has a manual score adjustment |
| `Review` | Grading produced an abnormal question result that needs instructor attention |
| `Failed` | The grading workflow could not complete properly for the student |

Examples of **Grading Review** findings include compilation errors, runtime errors, timeouts, interrupted execution, missing sources, workspace-preparation errors, or other abnormal question results.

### Validation Review vs Grading Review

The **Review Issues** dialog tracks validation and grading review separately.

- **Validation Review** — the submission itself still has a validation issue requiring instructor judgement.
- **Grading Review** — grading ran, but one or more grading results require instructor attention.

The instructor can add separate notes and resolve each review independently.

The Review Status column can show:

- `Validation Pending`
- `Grading Pending`
- `Both Pending`
- `Resolved`
- `-` when no review is required

Resolving one review type does not automatically resolve the other.

---

## Manual Score Adjustment

For Java grading, an instructor can manually adjust an individual question score after automatic grading.

A manual score must:

- be within the question's allowed mark range
- include a reason for the adjustment

The final total is recalculated using the manual score, and the reason is retained for reporting.

---

## Result Export

The GUI **Export Results** action currently generates two files:

1. Official school-format CSV
2. Detailed PDF report

If the selected CSV filename is:

```text
gradedscores.csv
```

the PDF is saved beside it as:

```text
gradedscores_report.pdf
```

The detailed PDF can include:

- batch validation and grading summary
- individual student results
- question scores
- validation issues
- correction information
- manual adjustments
- review information
- edge-case results
- review queue information

The project also contains reporting helper classes for detailed CSV, correction-history, and edge-case CSV generation, but the current main **Export Results** button exports only the official CSV and detailed PDF.

---

## User Interface

The Java Swing GUI provides:

- Java / Python assessment mode selection
- submissions folder browser
- validation controls
- submission summary counters
- searchable/filterable student results
- correction summary and details
- system log and progress bar
- status colouring
- horizontal and vertical scrolling for larger batches

The main student table contains:

- Student
- Structure
- Headers
- Status
- Grading
- Original Total / Total
- Generated Total (Java mode)
- Plagiarism Check
- Review Status

### Java Mode Workflow

```text
Select Submissions Folder
        ↓
Validate Submissions
        ↓
Start Grading (Original Testers)
        ↓
Generate Testers / Grade Generated (Optional)
        ↓
Run Edge Cases (Optional)
        ↓
Review Issues / Manual Score (Optional)
        ↓
Export Results
```

### Python Mode Workflow

```text
Select Submissions Folder
        ↓
Validate Submissions
        ↓
Start Python Grading
        ↓
Export Results
```

Java-only bonus/action panels are hidden in Python mode.

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
│   ├── java/
│   │   ├── resources/
│   │   ├── template/
│   │   ├── test-data/
│   │   └── testers/
│   └── python/
│       ├── reference/
│       ├── template/
│       └── testers/
│
├── submissions/
├── generated_testers/
├── edgecases/
├── workspace/
│   ├── java/
│   └── python/
├── temp_workspaces/
├── tests/
├── config/
├── lib/
├── classes/
│
├── compile.bat
├── run.bat
├── compile.sh
├── run.sh
└── README.md
```

---

## Package Responsibilities

| Package | Responsibility |
|---|---|
| `validation` | Extracts, validates, corrects, inspects, and records submission issues |
| `compilation` | Prepares Java question workspaces and compiles source/tester files |
| `execution` | Runs external processes safely with timeout and process control |
| `grading` | Executes Java/Python grading workflows and calculates results |
| `testgen` | Generates optional parameterized Java tester files |
| `reporting` | Generates official CSV and detailed PDF output, plus reporting helpers |
| `model` | Shared result and assessment data objects, including plagiarism results |
| `ui` | Java Swing interface, controllers, review workflow, filters, and progress updates |

---

## Requirements

### Java

A Java Development Kit (**JDK**) is required because the application uses both `java` and `javac`.

Check with:

```bash
java -version
javac -version
```

### Python Mode

Python grading requires a working Python installation accessible to the grading process.

### Included Java Libraries

Required PDFBox dependencies are already included under:

```text
lib/
```

The current project includes Apache PDFBox 3.0.8 and its required supporting JARs.

---

# How to Run

## Windows

Open PowerShell or Command Prompt in the project root.

### 1. Compile

```powershell
.\compile.bat
```

Expected output:

```text
Compilation successful.
```

### 2. Run

```powershell
.\run.bat
```

---

## macOS / Linux

### 1. Compile

```bash
bash compile.sh
```

### 2. Run

```bash
bash run.sh
```

Run the scripts from the project root so the assessment, library, workspace, and submission paths resolve correctly.

---

# How to Use

### Step 1 — Select assessment mode

Choose either:

```text
CS102 - Java
```

or:

```text
IS1704 - Python
```

### Step 2 — Select submissions

Java mode defaults to: submissions/java/original/
Python mode defaults to: submissions/python/

Use Browse to select another submissions folder when needed, such as: submissions/java/batch100/

### Step 3 — Validate

Click:

```text
1. Validate Submissions
```

Review the validation summary, student table, Correction Summary, Full Details, and System Log.

### Step 4 — Grade

For Java:

```text
Start Grading (Original Testers)
```

For Python:

```text
Start Python Grading
```

### Step 5 — Java optional checks

Java mode also provides:

```text
Generate Testers
Grade with Generated Testers
Run Edge Cases (Bonus)
Review Issues
Manual Score (Optional)
```

### Step 6 — Export

Click:

```text
Export Results
```

and choose the destination for the official CSV. The detailed PDF is created beside it automatically.

---

## Important Notes

- The original student ZIP files are not modified by Java correction.
- Python validation only normalizes eligible folder layouts and does not alter student answer code or identity information.
- Professor-provided Java testers remain the source of the official Java score.
- Generated Java tester results are comparison-only and are not exported as the official grade.
- Edge-case results do not change official marks.
- Plagiarism similarity results are indicators for manual review only.
- Validation Review and Grading Review are tracked independently.
- A failed or abnormal submission is isolated so processing can continue for the remaining students.
- Java grading uses isolated temporary workspaces to reduce interference between students/questions and old compiled files.
- The files under `config/` are present in the project, but the current application does not load them as runtime configuration.

---

## Contributors

| Member | Contribution |
|---|---|
| Thet Htar Nwe | Main GUI & Workflow Integration, Batch Processing & Error Handling, Validation Workflow Integration, Progress Tracking & System Log, Submission Correction & Correction History, Validation and Grading Review Issues Workflow |
| Thet Htar Nwe — Bonus | Parameterized/Generated Tester Files; 50 Additional Dummy Submissions |
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

## Current Project Status

The current system supports an end-to-end Java grading workflow and a Python assessment workflow through the same desktop application.

Implemented functionality includes:

- Java and Python assessment-mode switching
- batch submission validation
- safe submission correction/normalization
- validation status tracking
- template/starter submission detection
- Java compilation and controlled execution
- Java and Python grading
- official Java tester grading
- parameterized generated Java tester comparison
- Java edge-case testing
- Java plagiarism similarity checking
- separate validation and grading review workflow
- manual Java score adjustment
- official CSV export
- detailed PDF report generation


Further improvements may focus on extending assessment support and improving automation and usability.