package autograder.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Builds the correction table and its detailed inspection panel. */
final class CorrectionPanelBuilder {
    private final MainFrame f;
    CorrectionPanelBuilder(MainFrame frame) { this.f = frame; }

    /** Handles create correction panel. */
    JPanel createCorrectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        f.mostFixesLabel.setFont(f.mostFixesLabel.getFont().deriveFont(Font.BOLD));
        panel.add(f.mostFixesLabel, BorderLayout.NORTH);

        f.correctionTable.setFillsViewportHeight(true);
        f.correctionTable.setRowHeight(24);
        f.correctionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        f.correctionTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        f.correctionTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        f.correctionTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        f.correctionTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        f.correctionTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        f.correctionTable.getColumnModel().getColumn(5).setPreferredWidth(70);
        f.correctionTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        f.correctionTable.getColumnModel().getColumn(7).setPreferredWidth(85);

        DefaultTableCellRenderer countRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int count = value instanceof Number ? ((Number) value).intValue() : 0;
                int modelColumn = table.convertColumnIndexToModel(column);

                label.setHorizontalAlignment(JLabel.CENTER);
                label.setFont(table.getFont().deriveFont(Font.PLAIN));
                label.setForeground(Color.BLACK);
                label.setBackground(Color.WHITE);

                if (modelColumn == 4 || modelColumn == 5) {
                    // Open Issues / Anomalies: any positive number needs attention.
                    if (count == 1) {
                        label.setBackground(new Color(254, 243, 199));
                        label.setForeground(new Color(146, 64, 14));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    } else if (count >= 2) {
                        label.setBackground(new Color(254, 226, 226));
                        label.setForeground(new Color(153, 27, 27));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                } else {
                    // Folder / Name / Email / Total Fixes: stronger colour = more automatic fixes.
                    if (count >= 1 && count <= 2) {
                        label.setBackground(new Color(254, 249, 195));
                    } else if (count >= 3 && count <= 5) {
                        label.setBackground(new Color(254, 215, 170));
                        label.setForeground(new Color(154, 52, 18));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    } else if (count >= 6) {
                        label.setBackground(new Color(254, 202, 202));
                        label.setForeground(new Color(153, 27, 27));
                        label.setFont(table.getFont().deriveFont(Font.BOLD));
                    }
                }

                if (isSelected) label.setBorder(BorderFactory.createLineBorder(table.getSelectionBackground(), 2));
                else label.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
                return label;
            }
        };
        for (int column = 1; column <= 6; column++) f.correctionTable.getColumnModel().getColumn(column).setCellRenderer(countRenderer);
        f.correctionTable.getColumnModel().getColumn(7).setCellRenderer(f.layout.createStatusRenderer());

        panel.add(new JScrollPane(f.correctionTable), BorderLayout.CENTER);

        f.correctionDetailArea.setEditable(false);
        f.correctionDetailArea.setLineWrap(true);
        f.correctionDetailArea.setWrapStyleWord(true);
        f.correctionDetailArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        f.correctionDetailArea.setText("Select a student to see exact correction files and anomalies.");
        JScrollPane correctionDetailsScroll = new JScrollPane(f.correctionDetailArea);
        correctionDetailsScroll.setBorder(BorderFactory.createTitledBorder("Selected Student"));
        correctionDetailsScroll.setPreferredSize(new Dimension(0, 150));
        panel.add(correctionDetailsScroll, BorderLayout.SOUTH);
        return panel;
    }

}
