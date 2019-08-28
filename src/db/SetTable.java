/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package db;

import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.AbstractTableModel;


public class SetTable extends AbstractTableModel {
    private ResultSet st;
    public SetTable(ResultSet st) {
    this.st = st;
    }

 public int getColumnCount() {
        try {
            if (st == null) {
                return 0;
            } else {
                return st.getMetaData().getColumnCount();
            }
        } catch (SQLException e) {
            System.out.println("resultset generating error while getting column count");
            System.out.println(e.getMessage());
            return 0;
        }
    }

    public int getRowCount() {
        try {
            if (st == null) {
                return 0;
            } else {
                st.last();
                return st.getRow();
            }
        } catch (SQLException e) {
            System.out.println("resultset generating error while getting rows count");
            System.out.println(e.getMessage());
            return 0;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex > getRowCount()
                || columnIndex < 0 || columnIndex > getColumnCount()) {
            return null;
        }
        try {
            if (st == null) {
                return null;
            } else {
                st.absolute(rowIndex + 1);
                return st.getObject(columnIndex + 1);
            }
        } catch (SQLException e) {
            System.out.println("resultset generating error while fetching rows");
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        try {
            return st.getMetaData().getColumnName(columnIndex + 1);
        } catch (SQLException e) {
            System.out.println("resultset generating error while fetching column name");
            System.out.println(e.getMessage());
        }
        return super.getColumnName(columnIndex);
    }

}

