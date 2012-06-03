package com.plexq.hermes;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.HashMap;
import java.sql.*;

public class TableMetadata {
	private static HashMap<String, TableMetadata> metadataCache = new HashMap<String, TableMetadata>();

	private Logger log = Logger.getLogger(TableMetadata.class);

	private ArrayList<String> primaryKeys;
	private TreeMap<String, Class> columnTypes;
    private TreeMap<String, Guidance> tableGuidance = new TreeMap<String, Guidance>();
	private boolean debug=true;

	public TableMetadata(Connection db, String tableName) throws SQLException {
		if (db != null) {
			primaryKeys = new ArrayList<String>();
			columnTypes = new TreeMap<String, Class>();

			//System.out.println("Dynamically initializing Table Representation");
			DatabaseMetaData dmd = db.getMetaData();
			ResultSet rs = null;

			rs = dmd.getPrimaryKeys(null, null, tableName);
			while (rs.next()) {
				primaryKeys.add(rs.getString(4));
			}

			rs = dmd.getColumns(null, null, tableName, null);
            int p = 1;
			while (rs.next()) {
				String name = rs.getString(4);
				int n = rs.getInt(5);
				if (debug) {
					log.info("Scanned column "+name+" is of type "+n);
				}
				if (n == Types.BIGINT) {
					columnTypes.put(name, Long.class);
				}
				else if (n == Types.BOOLEAN) {
					columnTypes.put(name, Boolean.class);
				}
				else if (n == Types.CHAR) {
					columnTypes.put(name, String.class);
				}
				else if (n == Types.DATE) {
					columnTypes.put(name, java.sql.Date.class);
				}
				else if (n == Types.DECIMAL) {
					columnTypes.put(name, Double.class);
				}
				else if (n == Types.DOUBLE) {
					columnTypes.put(name, Double.class);
				}
				else if (n == Types.FLOAT) {
					columnTypes.put(name, Float.class);
				}
				else if (n == Types.INTEGER) {
					columnTypes.put(name, Integer.class);
				}
				else if (n == Types.NUMERIC) {
					columnTypes.put(name, Double.class);
				}
				else if (n == Types.REAL) {
					columnTypes.put(name, Double.class);
				}
				else if (n == Types.SMALLINT) {
					columnTypes.put(name, Integer.class);
				}
				else if (n == Types.TIMESTAMP) {
					columnTypes.put(name, java.sql.Timestamp.class);
				}
				else if (n == Types.VARCHAR) {
					columnTypes.put(name, String.class);
				}
				else if (n == Types.TINYINT) {
					columnTypes.put(name, Integer.class);
				}
				else if (n == Types.LONGVARCHAR) {
					columnTypes.put(name, String.class);
				}
                else if (n == Types.CLOB) {
                    columnTypes.put(name, String.class);
                }
                else if (n == Types.BLOB) {
                    columnTypes.put(name, byte[].class);
                }
                else if (n == Types.VARBINARY) {
                    columnTypes.put(name, byte[].class);
                }
                else if (n == Types.BINARY) {
                    columnTypes.put(name, byte[].class);
                }
				else if (n == Types.BIT) {
					columnTypes.put(name, Boolean.class);
				}
				else {
					throw new RuntimeException("Type not supported: "+n);
				}
				/*
				if (n==Types.SQLXML) {
					columnTypes.put(name, String.class);
				}
				*/
				/*
				if (n==Types.NVARCHAR) {
					columnTypes.put(name, String.class);
				}
				*/
				/*
				if (n==Types.NCHAR) {
					columnTypes.put(name, String.class);
				}
				*/

                Guidance g = Guidance.fetchGuidance(db,null,tableName,name);
                g.setFieldPosition(p++);
                tableGuidance.put(name,g);

			}
		}
	}

	public ArrayList<String> getPrimaryKeys() {
		return primaryKeys;
	}

	public void setPrimaryKeys(ArrayList<String> primaryKeys) {
		this.primaryKeys = primaryKeys;
	}

	public TreeMap<String, Class> getColumnTypes() {
		return columnTypes;
	}

    public List<String> getColumnNames() {
        ArrayList<String> al = new ArrayList<String>();
        for (String s : columnTypes.keySet()) {
            al.add(s);
        }
        return al;
    }

	public void setColumnTypes(TreeMap<String, Class> columnTypes) {
		this.columnTypes = columnTypes;
	}

	/**
	 * TODO support schemas here somehow
	 * We cache metadata about tables through this call.  I can't personally imagine many scenarios where
	 * you might want to make this a LRU expiring cache, how many tables can you have?
	 * TODO implement a container size limit on this
	 * @param db A Database connection object
	 * @param tableName The name of the table we want to get meta data about
	 * @return a table meta data object containing the table's meta data
	 * @throws SQLException When there is a problem selecting the data from the database
	 */
	public static TableMetadata fetchMetadata(Connection db, String tableName) throws SQLException {
		if (metadataCache.containsKey(tableName)) {
			return metadataCache.get(tableName);
		}
		else {
			TableMetadata tm = new TableMetadata(db, tableName);
			metadataCache.put(tableName, tm);
			return tm;
		}
	}

    public TreeMap<String, Guidance> getTableGuidance() {
        return tableGuidance;
    }

    public void setTableGuidance(TreeMap<String, Guidance> tableGuidance) {
        this.tableGuidance = tableGuidance;
    }
}
