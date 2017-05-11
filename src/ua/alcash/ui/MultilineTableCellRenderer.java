package ua.alcash.ui;

import ua.alcash.Configuration;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Multiline Table Cell Renderer.
 * Taken from http://blog.botunge.dk/post/2009/10/09/JTable-multiline-cell-renderer.aspx
 */
public class MultilineTableCellRenderer extends JTextArea implements TableCellRenderer {
    private List<List<Integer>> rowColHeight = new ArrayList<>();

    int maxLength;
    int maxLines;

    public MultilineTableCellRenderer() {
        maxLength = Integer.parseInt(Configuration.get("test maximum displayed length"));
        maxLines = Integer.parseInt(Configuration.get("test maximum displayed lines"));
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
    }

    protected String shorten(String value) {
        String displayed = value;
        for (int i = 0, count = 0; i < displayed.length(); ++i) {
            if (displayed.charAt(i) == '\n') {
                ++count;
                if (count >= maxLines - 1) {
                    displayed = displayed.substring(0, i) + "\n...";
                    break;
                }
            }
        }
        if (displayed.length() > maxLength) {
            displayed = displayed.substring(0, maxLength - 3) + "...";
        }
        return displayed;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        if (isSelected) {
            setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setFont(table.getFont());
        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
                setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            setBorder(new EmptyBorder(1, 2, 1, 2));
        }
        if (value != null) {
            setText(shorten(value.toString()));
        } else {
            setText("");
        }
        adjustRowHeight(table, row, column);
        return this;
    }

    /**
     * Calculates the new preferred height for a given row, and sets the height on the table.
     */
    private void adjustRowHeight(JTable table, int row, int column) {
        // The trick to get this to work properly is to set the width of the column to the
        // text area. The reason for this is that getPreferredSize(), without a width tries
        // to place all the text in one line. By setting the size with the with of the column,
        // getPreferredSize() returns the proper height which the row should have in
        // order to make room for the text.
        int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth();
        setSize(new Dimension(cWidth, 1000));
        int prefH = getPreferredSize().height;
        while (rowColHeight.size() <= row) {
            rowColHeight.add(new ArrayList<>(column));
        }
        List<Integer> colHeights = rowColHeight.get(row);
        while (colHeights.size() <= column) {
            colHeights.add(0);
        }
        colHeights.set(column, prefH);
        int maxH = prefH;
        for (Integer colHeight : colHeights) {
            if (colHeight > maxH) {
                maxH = colHeight;
            }
        }
        if (table.getRowHeight(row) != maxH) {
            table.setRowHeight(row, maxH);
        }
    }
}
