package ua.alcash.ui;

import ua.alcash.TestCase;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class TestsTableModel extends AbstractTableModel {
    private ArrayList<TestCase> testCases;
    private final String[] columnNames = {"Input", "Expected output", "Program output", "Result"};
    private final Class[] columnClasses = {String.class, String.class, String.class, String.class};

    TestsTableModel(ArrayList<TestCase> testCases) { this.testCases = testCases; }

    @Override
    public int getRowCount() { return testCases.size(); }

    @Override
    public int getColumnCount() { return columnNames.length; }

    @Override
    public Class<?> getColumnClass(int columnIndex) { return columnClasses[columnIndex]; }

    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return testCases.get(rowIndex).getInput();
            case 1:
                return testCases.get(rowIndex).getExpectedOutput();
            case 2:
                return testCases.get(rowIndex).getProgramOutput();
            case 3:
                return testCases.get(rowIndex).getExecutionResults("\n");
            default:
                throw new UnsupportedOperationException("Implementation error: invalid columnIndex.");
        }
    }

    void rowUpdated(int index) { fireTableRowsUpdated(index, index); }

    void rowInserted() { fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1); }

    void rowDeleted(int index) { fireTableRowsDeleted(index, index); }
}
