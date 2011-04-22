/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.test.it.keyupdate;

import com.akiban.server.IndexDef;
import com.akiban.server.RowDef;
import com.akiban.server.api.dml.scan.NewRow;
import com.akiban.server.test.it.ITBase;
import com.akiban.util.ArgumentValidation;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import static com.akiban.server.test.it.keyupdate.Schema.c_cid;
import static com.akiban.server.test.it.keyupdate.Schema.c_cx;
import static com.akiban.server.test.it.keyupdate.Schema.customerRowDef;
import static com.akiban.server.test.it.keyupdate.Schema.groupRowDef;
import static com.akiban.server.test.it.keyupdate.Schema.i_iid;
import static com.akiban.server.test.it.keyupdate.Schema.i_ix;
import static com.akiban.server.test.it.keyupdate.Schema.i_oid;
import static com.akiban.server.test.it.keyupdate.Schema.itemRowDef;
import static com.akiban.server.test.it.keyupdate.Schema.o_cid;
import static com.akiban.server.test.it.keyupdate.Schema.o_oid;
import static com.akiban.server.test.it.keyupdate.Schema.o_ox;
import static com.akiban.server.test.it.keyupdate.Schema.o_priority;
import static com.akiban.server.test.it.keyupdate.Schema.o_when;
import static com.akiban.server.test.it.keyupdate.Schema.orderRowDef;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public abstract class KeyUpdateBase extends ITBase {
    @Before
    public final void before() throws Exception
    {
        testStore = new TestStore(persistitStore());
        rowDefsToCounts = new TreeMap<Integer, Integer>();
        createSchema();
        confirmColumns();
        populateTables();
    }

    private void confirmColumns() {
        // TODO
    }

    @Test
    @SuppressWarnings("unused") // JUnit will invoke this
    public void testInitialState() throws Exception
    {
        checkDB();
        checkInitialState();
    }

    protected final void dbInsert(TestRow row) throws Exception
    {
        testStore.writeRow(session(), row);
        Integer oldCount = rowDefsToCounts.get(row.getTableId());
        oldCount = (oldCount == null) ? 1 : oldCount+1;
        rowDefsToCounts.put(row.getTableId(), oldCount);
    }

    protected final void dbUpdate(TestRow oldRow, TestRow newRow) throws Exception
    {
        testStore.updateRow(session(), oldRow, newRow, null);
    }

    protected final void dbDelete(TestRow row) throws Exception
    {
        testStore.deleteRow(session(), row);
        Integer oldCount = rowDefsToCounts.get(row.getTableId());
        assertNotNull(oldCount);
        rowDefsToCounts.put(row.getTableId(), oldCount - 1);
    }

    private int countAllRows() {
        int total = 0;
        for (Integer count : rowDefsToCounts.values()) {
            total += count;
        }
        return total;
    }

    protected final void checkDB()
            throws Exception
    {
        // Records
        RecordCollectingTreeRecordVisistor testVisitor = new RecordCollectingTreeRecordVisistor();
        RecordCollectingTreeRecordVisistor realVisitor = new RecordCollectingTreeRecordVisistor();
        testStore.traverse(session(), groupRowDef, testVisitor, realVisitor);
        assertEquals(testVisitor.records(), realVisitor.records());
        assertEquals("records count", countAllRows(), testVisitor.records().size());
        // Check indexes
        RecordCollectingIndexRecordVisistor indexVisitor;
        if (checkChildPKs()) {
            // Customer PK index - skip. This index is hkey equivalent, and we've already checked the full records.
            // Order PK index
            indexVisitor = new RecordCollectingIndexRecordVisistor();
            testStore.traverse(session(), orderRowDef.getPKIndexDef(), indexVisitor);
            assertEquals(orderPKIndex(testVisitor.records()), indexVisitor.records());
            assertEquals("order PKs", countRows(orderRowDef), indexVisitor.records().size());
            // Item PK index
            indexVisitor = new RecordCollectingIndexRecordVisistor();
            testStore.traverse(session(), itemRowDef.getPKIndexDef(), indexVisitor);
            assertEquals(itemPKIndex(testVisitor.records()), indexVisitor.records());
            assertEquals("order PKs", countRows(itemRowDef), indexVisitor.records().size());
        }
        // Order priority index
        indexVisitor = new RecordCollectingIndexRecordVisistor();
        testStore.traverse(session(), indexDef(orderRowDef, "priority"), indexVisitor);
        assertEquals(orderPriorityIndex(testVisitor.records()), indexVisitor.records());
        assertEquals("order PKs", countRows(orderRowDef), indexVisitor.records().size());
        // Order timestamp index
        indexVisitor = new RecordCollectingIndexRecordVisistor();
        testStore.traverse(session(), indexDef(orderRowDef, "when"), indexVisitor);
        assertEquals(orderWhenIndex(testVisitor.records()), indexVisitor.records());
        assertEquals("order PKs", countRows(orderRowDef), indexVisitor.records().size());
    }

    private int countRows(RowDef rowDef) {
        return rowDefsToCounts.get(rowDef.getRowDefId());
    }

    private IndexDef indexDef(RowDef rowDef, String indexName) {
        for (IndexDef indexDef : rowDef.getIndexDefs()) {
            if (indexName.equals(indexDef.getName())) {
                return indexDef;
            }
        }
        throw new NoSuchElementException(indexName);
    }

    protected final void checkInitialState() throws Exception
    {
        RecordCollectingTreeRecordVisistor testVisitor = new RecordCollectingTreeRecordVisistor();
        RecordCollectingTreeRecordVisistor realVisitor = new RecordCollectingTreeRecordVisistor();
        testStore.traverse(session(), groupRowDef, testVisitor, realVisitor);
        Iterator<TreeRecord> expectedIterator = testVisitor.records().iterator();
        Iterator<TreeRecord> actualIterator = realVisitor.records().iterator();
        Map<Integer, Integer> expectedCounts = new HashMap<Integer, Integer>();
        expectedCounts.put(customerRowDef.getRowDefId(), 0);
        expectedCounts.put(orderRowDef.getRowDefId(), 0);
        expectedCounts.put(itemRowDef.getRowDefId(), 0);
        Map<Integer, Integer> actualCounts = new HashMap<Integer, Integer>();
        actualCounts.put(customerRowDef.getRowDefId(), 0);
        actualCounts.put(orderRowDef.getRowDefId(), 0);
        actualCounts.put(itemRowDef.getRowDefId(), 0);
        while (expectedIterator.hasNext() && actualIterator.hasNext()) {
            TreeRecord expected = expectedIterator.next();
            TreeRecord actual = actualIterator.next();
            assertEquals(expected, actual);
            assertEquals(hKey((TestRow) expected.row()), actual.hKey());
            checkInitialState(actual.row());
            expectedCounts.put(expected.row().getTableId(), expectedCounts.get(expected.row().getTableId()) + 1);
            actualCounts.put(actual.row().getTableId(), actualCounts.get(actual.row().getTableId()) + 1);
        }
        assertEquals(3, expectedCounts.get(customerRowDef.getRowDefId()).intValue());
        assertEquals(9, expectedCounts.get(orderRowDef.getRowDefId()).intValue());
        assertEquals(27, expectedCounts.get(itemRowDef.getRowDefId()).intValue());
        assertEquals(3, actualCounts.get(customerRowDef.getRowDefId()).intValue());
        assertEquals(9, actualCounts.get(orderRowDef.getRowDefId()).intValue());
        assertEquals(27, actualCounts.get(itemRowDef.getRowDefId()).intValue());
        assertTrue(!expectedIterator.hasNext() && !actualIterator.hasNext());
    }

    protected final void checkInitialState(NewRow row)
    {
        RowDef rowDef = row.getRowDef();
        if (rowDef == customerRowDef) {
            assertEquals(row.get(c_cx), ((Long)row.get(c_cid)) * 100);
        } else if (rowDef == orderRowDef) {
            assertEquals(row.get(o_cid), ((Long)row.get(o_oid)) / 10);
            assertEquals(row.get(o_ox), ((Long)row.get(o_oid)) * 100);
        } else if (rowDef == itemRowDef) {
            assertEquals(row.get(i_oid), ((Long)row.get(i_iid)) / 10);
            assertEquals(row.get(i_ix), ((Long)row.get(i_iid)) * 100);
        } else {
            fail();
        }
    }

    /**
     * Given a list of records, a RowDef and a list of columns, extracts the index entries.
     * @param records the records to take entries from
     * @param rowDef the rowdef of records to look at
     * @param columns a union of either Integer (the column ID) or HKeyElement.
     * Any other types will throw a RuntimeException
     * @return a list representing indexes of these records
     */
    protected final List<List<Object>> indexFromRecords(List<TreeRecord> records, RowDef rowDef, Object... columns) {
        List<List<Object>> indexEntries = new ArrayList<List<Object>>();
        for (TreeRecord record : records) {
            if (record.row().getRowDef() == rowDef) {
                List<Object> indexEntry = new ArrayList<Object>(columns.length);
                for (Object column : columns) {
                    final Object indexEntryElement;
                    if (column instanceof Integer) {
                        indexEntryElement = record.row().get( (Integer)column );
                    }
                    else if (column instanceof HKeyElement) {
                        indexEntryElement = record.hKey().objectArray()[ ((HKeyElement) column).getIndex() ];
                    }
                    else {
                        String msg = String.format(
                                "column must be an Integer or HKeyElement: %s in %s:",
                                column == null ? "null" : column.getClass().getName(),
                                Arrays.toString(columns)
                        );
                        throw new RuntimeException(msg);
                    }
                    indexEntry.add(indexEntryElement);
                }
                indexEntries.add(indexEntry);
            }
        }
        Collections.sort(indexEntries,
                new Comparator<List<Object>>() {
                    @Override
                    public int compare(List<Object> x, List<Object> y) {
                        // compare priorities
                        Long px = (Long) x.get(0);
                        Long py = (Long) y.get(0);
                        return px.compareTo(py);
                    }
                }
        );
        return indexEntries;
    }

    protected final TestRow row(RowDef table, Object... values)
    {
        TestRow row = new TestRow(table.getRowDefId());
        int column = 0;
        for (Object value : values) {
            if (value instanceof Integer) {
                value = ((Integer) value).longValue();
            }
            row.put(column++, value);
        }
        row.hKey(hKey(row));
        return row;
    }

    protected static final class HKeyElement {
        private final int index;

        public static HKeyElement from(int index) {
            return new HKeyElement(index);
        }

        public HKeyElement(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    abstract protected void createSchema() throws Exception;
    abstract protected void populateTables() throws Exception;
    abstract protected boolean checkChildPKs();
    abstract protected HKey hKey(TestRow row);
    abstract protected List<List<Object>> orderPKIndex(List<TreeRecord> records);
    abstract protected List<List<Object>> itemPKIndex(List<TreeRecord> records);
    abstract protected List<List<Object>> orderPriorityIndex(List<TreeRecord> records);
    abstract protected List<List<Object>> orderWhenIndex(List<TreeRecord> records);

    protected TestStore testStore;
    protected Map<Integer,Integer> rowDefsToCounts;
    
}
