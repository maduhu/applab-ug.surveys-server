package applab.surveys.server.test;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

import org.w3c.dom.*;

import applab.server.XmlHelpers;
import applab.surveys.server.Select;

/**
 * Given the result of a remote Select call, parse the results into a ResultSet
 * 
 * 
 */
public class RemoteResultSet implements ResultSet {
    boolean isClosed;
    ArrayList<Row> rows;

    // NOTE that index = 0 is "before the first row", so the active row is rows[currentRowIndex - 1];
    int currentRowIndex;

    public RemoteResultSet(Document remoteResultsXml) {
        this.rows = new ArrayList<Row>();

        Element rootNode = remoteResultsXml.getDocumentElement();

        // validate the root name/namespace
        if (!Select.NAMESPACE.equals(rootNode.getNamespaceURI()) || !Select.RESPONSE_ELEMENT_NAME.equals(rootNode.getLocalName())) {
            throw new IllegalArgumentException("Root element must have name='" + Select.RESPONSE_ELEMENT_NAME + "' and namespace='"
                    + Select.NAMESPACE + "'");
        }

        // and parse the child nodes (one per row)
        for (Node childNode = rootNode.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                if (!Select.NAMESPACE.equals(childNode.getNamespaceURI()) || !Select.ROW_ELEMENT_NAME.equals(childNode.getLocalName())) {
                    throw new IllegalArgumentException("Row elements must have name='" + Select.ROW_ELEMENT_NAME + "' and namespace='"
                            + Select.NAMESPACE + "'");
                }

                this.rows.add(new Row((Element)childNode));
            }
        }
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        // specific row
        if (row >= 0) {
            // make sure we are never positioned more than one past the end
            this.currentRowIndex = Math.min(row, this.rows.size() + 1);
        }
        else {
            this.currentRowIndex = Math.max(0, this.rows.size() + 1 + row);
        }

        return isCurrentRowInResults();
    }

    @Override
    public void afterLast() throws SQLException {
        this.currentRowIndex = this.rows.size() + 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        this.currentRowIndex = 0;
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLException("this ResultSet does not support cancelRowUpdates");
    }

    @Override
    public void clearWarnings() throws SQLException {
    }

    @Override
    public void close() throws SQLException {
        this.isClosed = true;
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLException("this ResultSet does not support deleteRow");
    }

    @Override
    public int findColumn(String columnName) throws SQLException {
        Row currentRow = getCurrentRow();
        if (currentRow == null) {
            throw new IllegalStateException("ResultSet is not positioned on a valid row");
        }

        return currentRow.getColumn(columnName).getIndex();
    }

    @Override
    public boolean first() throws SQLException {
        return absolute(1);
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array getArray(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getAsciiStream(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(String columnIndex, int arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getBinaryStream(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getBoolean(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte getByte(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getBytes(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getCharacterStream(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getConcurrency() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getCursorName() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(String arg0, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return Double.parseDouble(getString(columnIndex));
    }

    @Override
    public double getDouble(String columnName) throws SQLException {
        return Double.parseDouble(getString(columnName));
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return Float.parseFloat(getString(columnIndex));
    }

    @Override
    public float getFloat(String columnName) throws SQLException {
        return Float.parseFloat(getString(columnName));
    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return Integer.parseInt(getString(columnIndex));
    }

    @Override
    public int getInt(String columnName) throws SQLException {
        return Integer.parseInt(getString(columnName));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return Long.parseLong(getString(columnIndex));
    }

    @Override
    public long getLong(String columnName) throws SQLException {
        return Long.parseLong(getString(columnName));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw new SQLException("metadata not currently supported");
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public Object getObject(String columnName) throws SQLException {
        return getString(columnName);
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> customMapping) throws SQLException {
        String stringValue = getString(columnIndex);
        return customMapping.get(stringValue);
    }

    @Override
    public Object getObject(String columnName, Map<String, Class<?>> customMapping) throws SQLException {
        String stringValue = getString(columnName);
        return customMapping.get(stringValue);
    }

    @Override
    public Ref getRef(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRow() throws SQLException {
        return this.currentRowIndex;
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(String columnName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnName) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return Short.parseShort(getString(columnIndex));
    }

    @Override
    public short getShort(String columnName) throws SQLException {
        return Short.parseShort(getString(columnName));
    }

    @Override
    public Statement getStatement() throws SQLException {
        return null; // no statement used for this result set
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        Row currentRow = getCurrentRow();
        if (currentRow == null) {
            throw new IllegalStateException("ResultSet is not positioned on a valid row");
        }
        Column column = currentRow.getColumn(columnIndex); 
        return column.getValue();
    }

    @Override
    public String getString(String columnName) throws SQLException {
        Row currentRow = getCurrentRow();
        if (currentRow == null) {
            throw new IllegalStateException("ResultSet is not positioned on a valid row");
        }
        Column column = currentRow.getColumn(columnName); 
        return column.getValue();
    }

    @Override
    public Time getTime(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(int arg0, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(String arg0, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(int arg0, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(String arg0, Calendar arg1) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getType() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public URL getURL(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getURL(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String arg0) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return this.currentRowIndex == this.rows.size() + 1 || this.rows.isEmpty();
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return this.currentRowIndex == 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.isClosed;
    }

    @Override
    public boolean isFirst() throws SQLException {
        return this.currentRowIndex == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        return this.currentRowIndex == this.rows.size();
    }

    @Override
    public boolean last() throws SQLException {
        this.currentRowIndex = this.rows.size();
        return this.isCurrentRowInResults();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public boolean next() throws SQLException {
        return relative(1);
    }

    @Override
    public boolean previous() throws SQLException {
        return relative(-1);
    }

    @Override
    public void refreshRow() throws SQLException {
        // do nothing
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        this.currentRowIndex = Math.max(0, Math.min(this.currentRowIndex + rows, this.rows.size() + 1));
        return isCurrentRowInResults();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        // do nothing
    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        // do nothing
    }

    @Override
    public void updateArray(int arg0, Array arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateArray(String arg0, Array arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateAsciiStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateBigDecimal(int arg0, BigDecimal arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateBigDecimal(String arg0, BigDecimal arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support update");
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(int arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBinaryStream(String arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBinaryStream");
    }

    @Override
    public void updateBlob(int arg0, Blob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBlob(String arg0, Blob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBlob(int arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBlob(String arg0, InputStream arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBlob(int arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBlob(String arg0, InputStream arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBlob");
    }

    @Override
    public void updateBoolean(int arg0, boolean arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBoolean");
    }

    @Override
    public void updateBoolean(String arg0, boolean arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBoolean");
    }

    @Override
    public void updateByte(int arg0, byte arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateByte");
    }

    @Override
    public void updateByte(String arg0, byte arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateByte");
    }

    @Override
    public void updateBytes(int arg0, byte[] arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBytes");
    }

    @Override
    public void updateBytes(String arg0, byte[] arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateBytes");
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateCharacterStream");
    }

    @Override
    public void updateClob(int arg0, Clob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateClob(String arg0, Clob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateClob(int arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateClob(String arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateClob(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateClob(String arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateClob");
    }

    @Override
    public void updateDate(int arg0, Date arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateDate");
    }

    @Override
    public void updateDate(String arg0, Date arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateDate");
    }

    @Override
    public void updateDouble(int arg0, double arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateDouble");
    }

    @Override
    public void updateDouble(String arg0, double arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateDouble");
    }

    @Override
    public void updateFloat(int arg0, float arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateFloat");
    }

    @Override
    public void updateFloat(String arg0, float arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateFloat");
    }

    @Override
    public void updateInt(int arg0, int arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateInt");
    }

    @Override
    public void updateInt(String arg0, int arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateInt");
    }

    @Override
    public void updateLong(int arg0, long arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateLong");
    }

    @Override
    public void updateLong(String arg0, long arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateLong");
    }

    @Override
    public void updateNCharacterStream(int arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(String arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(String arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNCharacterStream");
    }

    @Override
    public void updateNClob(int arg0, NClob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNClob(String arg0, NClob arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNClob(int arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNClob(String arg0, Reader arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNClob(int arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNClob(String arg0, Reader arg1, long arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNClob");
    }

    @Override
    public void updateNString(int arg0, String arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNString");
    }

    @Override
    public void updateNString(String arg0, String arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNString");
    }

    @Override
    public void updateNull(int arg0) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNull");
    }

    @Override
    public void updateNull(String arg0) throws SQLException {
        throw new SQLException("this ResultSet does not support updateNull");
    }

    @Override
    public void updateObject(int arg0, Object arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateObject");
    }

    @Override
    public void updateObject(String arg0, Object arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateObject");
    }

    @Override
    public void updateObject(int columnIndex, Object arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateObject");
    }

    @Override
    public void updateObject(String columnName, Object arg1, int arg2) throws SQLException {
        throw new SQLException("this ResultSet does not support updateObject");
    }

    @Override
    public void updateRef(int columnIndex, Ref arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateRef");
    }

    @Override
    public void updateRef(String columnName, Ref arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateRef");
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLException("this ResultSet does not support updateRowId");
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateRowId");
    }

    @Override
    public void updateRowId(String columnName, RowId x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateRowId");
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateSQLXML");
    }

    @Override
    public void updateSQLXML(String columnName, SQLXML x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateSQLXML");
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateShort");
    }

    @Override
    public void updateShort(String columnName, short x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateShort");
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateString");
    }

    @Override
    public void updateString(String columnName, String x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateString");
    }

    @Override
    public void updateTime(int columnIndex, Time arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateTime");
    }

    @Override
    public void updateTime(String columnName, Time arg1) throws SQLException {
        throw new SQLException("this ResultSet does not support updateTime");
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateTimestamp");
    }

    @Override
    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
        throw new SQLException("this ResultSet does not support updateTimestamp");
    }

    @Override
    public boolean wasNull() throws SQLException {
        throw new SQLException("this ResultSet does not support wasNull");
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return null;
    }

    private boolean isCurrentRowInResults() {
        return 0 < this.currentRowIndex && this.currentRowIndex <= this.rows.size();
    }

    private Row getCurrentRow() {
        if (isCurrentRowInResults()) {
            return this.rows.get(this.currentRowIndex - 1);
        }
        else {
            return null;
        }
    }

    private class Row {
        private HashMap<String, Column> columnsByName;
        private HashMap<Integer, Column> columnsByIndex;

        public Row(Element remoteRowXml) {
            this.columnsByName = new HashMap<String, Column>();
            this.columnsByIndex = new HashMap<Integer, Column>();

            assert (Select.NAMESPACE.equals(remoteRowXml.getNamespaceURI())) : "caller must validate namespace";
            assert (Select.ROW_ELEMENT_NAME.equals(remoteRowXml.getLocalName())) : "caller must validate local name";

            // walk through the child nodes and populate the columns
            int currentIndex = 1;
            for (Node childNode = remoteRowXml.getFirstChild(); childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    // <columnName>data</columnName>
                    Element columnElement = (Element)childNode;
                    Column newColumn = new Column(columnElement.getLocalName(), XmlHelpers.getContent(columnElement), currentIndex);
                    this.columnsByName.put(newColumn.getName(), newColumn);
                    this.columnsByIndex.put(newColumn.getIndex(), newColumn);
                    currentIndex++;
                }
            }
        }

        private Column getColumn(String columnName) throws SQLException {
            Column targetColumn = this.columnsByName.get(columnName);
            if (targetColumn == null) {
                throw new SQLException("Column name '" + columnName + "' does not exist");
            }
            return targetColumn;
        }

        private Column getColumn(int columnIndex) throws SQLException {
            Column targetColumn = this.columnsByIndex.get(columnIndex);
            if (targetColumn == null) {
                throw new SQLException("Column index '" + columnIndex + "' does not exist");
            }
            return targetColumn;
        }
    }

    private class Column {
        private String name;
        private String value;
        private int index;

        public Column(String name, String value, int index) {
            this.name = name;
            this.value = value;
            this.index = index;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public int getIndex() {
            return this.index;
        }
    }
}
