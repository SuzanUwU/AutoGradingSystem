package autograder.ui;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Independent Lab 7 checker window. Does not change existing Java/Python lab-test grading. */
public final class Lab7CheckerFrame extends JFrame {
    private final JTextField folder = new JTextField(43);
    private final JTextArea log = new JTextArea(16, 72);
    private final JButton run = new JButton("Check Q2 + Q4 and Export Feedback CSV");

    public Lab7CheckerFrame(String defaultFolder) {
        super("IS1704 — Lab 7 Exercise Checker (Q2 + Q4)");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        folder.setText(defaultFolder);
        JPanel top = new JPanel(new BorderLayout(8,8));
        top.add(new JLabel("Student submissions folder:"), BorderLayout.WEST);
        top.add(folder, BorderLayout.CENTER);
        JButton browse = new JButton("Browse");
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(folder.getText());
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                folder.setText(fc.getSelectedFile().getAbsolutePath());
        });
        top.add(browse, BorderLayout.EAST);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        main.add(top,BorderLayout.NORTH);
        main.add(new JScrollPane(log),BorderLayout.CENTER);
        main.add(run,BorderLayout.SOUTH);
        setContentPane(main);
        run.addActionListener(e -> check());
        pack();
        setLocationRelativeTo(null);
    }

    private void check() {
        File dir = new File(folder.getText().trim());
        File script = new File("assessment/is1704/lab7/check_lab7.py");
        if (!dir.isDirectory() || !script.isFile()) {
            JOptionPane.showMessageDialog(this,"Check the submission directory and Lab 7 script path.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("lab7_feedback.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path output = fc.getSelectedFile().toPath().toAbsolutePath();
        if (!output.toString().toLowerCase().endsWith(".csv")) output = Path.of(output + ".csv");
        final Path csv = output;
        run.setEnabled(false);
        log.setText("Checking selected questions Q2a, Q2b, Q2c, Q4a, Q4b...\n");
        new SwingWorker<Integer,String>() {
            @Override protected Integer doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder(List.of(
                    "python", "-u", script.getAbsolutePath(),
                    "--submissions", dir.getAbsolutePath(),
                    "--output", csv.toString()));
                pb.redirectErrorStream(true);
                Process process = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))) {
                    String line;
                    while ((line=br.readLine())!=null) publish(line);
                }
                return process.waitFor();
            }
            @Override protected void process(List<String> lines) {
                for (String line:lines) log.append(line+"\n");
            }
            @Override protected void done() {
                run.setEnabled(true);
                try {
                    int code=get();
                    log.append(code==0 ? "\nFinished. Feedback: "+csv+"\n" : "\nChecker exited with code "+code+"\n");
                } catch(Exception ex) {log.append("\nERROR: "+ex.getMessage()+"\n");}
            }
        }.execute();
    }
}
