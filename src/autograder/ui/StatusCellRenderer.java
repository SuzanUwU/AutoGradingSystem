package autograder.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Consistent status coloring in validation and correction tables. */
public final class StatusCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        String status = value == null ? "" : value.toString();
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(table.getFont().deriveFont(Font.BOLD));

        if (!isSelected) {
            label.setBackground(Color.WHITE);
            label.setForeground(Color.BLACK);

            switch (status) {
                case "Valid": label.setBackground(new Color(220,245,224)); label.setForeground(new Color(28,94,45)); break;
                case "Corrected": label.setBackground(new Color(219,234,254)); label.setForeground(new Color(30,64,175)); break;
                case "Review": label.setBackground(new Color(254,243,199)); label.setForeground(new Color(146,64,14)); break;
                case "Failed": label.setBackground(new Color(254,226,226)); label.setForeground(new Color(153,27,27)); break;

                // Plagiarism check statuses.
                case "Clear": label.setBackground(new Color(220,245,224)); label.setForeground(new Color(28,94,45)); break;
                case "Suspected - review": label.setBackground(new Color(254,243,199)); label.setForeground(new Color(146,64,14)); break;
                case "Could not check":
                case "Checking...": label.setBackground(new Color(245,245,245)); label.setForeground(Color.DARK_GRAY); break;

                default: break;
            }
        }

        label.setBorder(isSelected
                ? BorderFactory.createLineBorder(table.getSelectionBackground(), 2)
                : BorderFactory.createEmptyBorder(1,4,1,4));

        return label;
    }
}