# CS102 Auto Grading System — Team Project Scaffold

**This is a folder/class starter template, not a working auto-grader.** All module classes have TODO placeholders. `Main` only prints a setup message; it does not grade student submissions.

## Run the scaffold

- macOS/Linux: `bash compile.sh` then `bash run.sh`
- Windows Command Prompt: `compile.bat` then `run.bat`
- Java JDK required. Compile/run scripts do not install a JDK.

## Proposed team allocation

1. Architecture/integration: `Main`, `config`, shared `model`, overall batch coordination (add `BatchGradingService` as needed).
2. Validation: `validation` package.
3. Compilation/execution: `compilation`, `execution`, and compiler/execution result models.
4. Grading: `grading` and test/question-result models.
5. Reporting: `reporting` and student-result model.
6. Console UI and QA: `ui` and `tests`.

Agree on method signatures, ownership of shared models, and error handling before implementation. The code in this template is deliberately empty and is not a completed solution.

## Project folders

- `src/autograder`: Java source organized by package.
- `classes`: Compiled application classes; empty in the downloadable scaffold.
- `config`: Example configuration files; configuration loading is not implemented.
- `assessment/testers`: Add instructor-provided tester Java files.
- `assessment/resources`: Add trusted input data and supporting class files needed for grading.
- `submissions`: Student ZIP inputs (empty here).
- `workspace`: Temporary extracted and grading workspaces (empty here).
- `output`: Generated results (empty here).
- `tests`: Add component/end-to-end tests.
- `lib`: Optional external open-source libraries.

## Notes for the team

- Confirm the professor's expectations around `compile.sh` / `run.sh` versus Windows `.bat` scripts. Both are included as a convenience, but Windows scripts have not been tested in this Linux environment.
- Do not store sensitive student submissions or generated results in a public repository.
- Ensure you attribute and document AI assistance as required by the course brief.
- Update the UML and folder structure when the team finalizes its actual implementation.
