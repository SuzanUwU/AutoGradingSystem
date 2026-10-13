# CS102 Auto Grading System

The CS102 Auto Grading System is a Java application designed to automate the grading workflow for Java programming submissions.

The system is intended to support:

- Batch processing of student submissions
- Submission validation and correction
- Java compilation
- Tester execution
- Timeout handling
- Score calculation
- CSV score export
- Grading and anomaly reports

The project is implemented using Java and Object-Oriented Programming principles.

---

## Current Progress

The project has currently completed the core **Submission Validation and Correction** stage.

The system can now process multiple student ZIP submissions, detect structural problems, automatically correct recoverable submission errors, and validate the required Java files.

The actual compilation, tester execution, grading, and CSV export stages are still under development.

---

# Implemented Features

## 1. Batch Student ZIP Processing

The program reads all `.zip` files inside the:

```text
submissions/
