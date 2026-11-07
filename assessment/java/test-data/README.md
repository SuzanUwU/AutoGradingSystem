# Parameterized Tester Data

These files are used by the bonus `TesterGenerator`.

- Edit test values here instead of editing Java tester source code.
- Click **2. Generate Testers (Bonus)** in the GUI.
- Generated Java tester files are written to `generated_testers/`.
- The normal **3. Start Grading** flow is intentionally unchanged and still uses the professor-provided files in `assessment/java/testers/`.

Formats:
- Q1a: `input1|input2;expected1|expected2`
- Q1b: typed values such as `int:2`, `boolean:false`, `double:4.0`, `string:two`
- Q2a: `file;surname;expected`
- Q2b: `file;course;expected` (`DATA_EXCEPTION` means an exception is expected)
- Q3: shapes such as `R:R1:10:2` or `C:C1:5`, then the expected list string
