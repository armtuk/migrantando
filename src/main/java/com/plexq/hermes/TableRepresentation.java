/* vim: set tabstop=4 shiftwidth=4 noexpandtab ai: */
/*
        Copyright Alex R.M. Turner 2008
        This file is part of Hermes DB
        Hermes DB is free software; you can redistribute it and/or modify
        it under the terms of the Lesser GNU General Public License as published by
        the Free Software Foundation; version 3 of the License.

        Hermes DB is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the Lesser GNU General Public License
        along with Hermes DB if not, write to the Free Software
        Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
        This file is released under the LGPL v3.0
*/
package com.plexq.hermes;

import java.sql.*;
import java.util.*;
import java.text.*;

import org.apache.log4j.*;

import com.plexq.hermes.tool.*;

/**
 * The TableRepresentation class is an interface that allows a class to
 * easily load and save data for a pkey set to and from the database
 */
public class TableRepresentation implements Map<String, Object> {
	/**
	 * Log4j Logger
	 */
	private static Logger log = Logger.getLogger(TableRepresentation.class);
	/**
	 * Database Connection that we will use to perform operations
	 */
	protected Connection db;
	/**
	 * The table name of this representation
	 */
	protected String tableName;
	/**
	 * The primary key array for this table
	 */
	protected String[] pkey;
	/**
	 * The map of data in this table representation
	 */
	protected TreeMap<String, Object> tableData;
	/**
	 * The map of the types of each column
	 */
	protected TreeMap<String, Class> tableTypes = new TreeMap<String, Class>();
	/**
	 * Does this object contain a valid set of data
	 */
	protected boolean dataInvalid;
	/**
	 * Message describing why is the data invalid
	 */
	protected String invalidMsg = "Unknown";
	/**
	 * Which columns to always update to local value
	 */
	protected TreeMap<String, String> forceColumns = new TreeMap<String, String>();
	/**
	 * Which columns to never update
	 */
	protected TreeMap<String, String> unforceColumns = new TreeMap<String, String>();
	/**
	 * Force Global Update always
	 */
	protected boolean globalUpdate;
	/**
	 * Debug
	 */
	protected boolean debug;
	/**
	 * Ignore exceptions or not?
	 */
	protected boolean ignoreExceptions = false;

	/**
	 * Wether to use optimistic locking
	 */
	protected boolean useOptimisticLocking = false;

	/**
	 * Field to use for optimistic locking
	 */
	protected String optimisticLock="version";

    /**
     * Type name generator
     */
    private TypeNameFactory typeNameFactory;

    private List<IndexRepresentation> indices = new ArrayList<IndexRepresentation>();

    private TableMetadata metaData;

	/**
	 * Create a table representation object
	 *
	 * @param inDb        a Connection object that allows this object to perform queries
	 * @param inTableName the name of the table that this object will represent
	 * @param inPkey      a String object that is the pkey, if you need an array of pkeys, there is another constructor that provides that mechanism
	 */
	public TableRepresentation(Connection inDb, String inTableName, String inPkey) throws SQLException {
		db = inDb;
		tableName = inTableName;
		pkey = new String[1];
		pkey[0] = inPkey;
		init();
	}

	/**
	 * Create a table representation object
	 *
	 * @param inDb        a Connection object that allows this object to perform queries
	 * @param inTableName the name of the table that this object will represent
	 * @param inPkey      a String array object that is an array of the names of the columns that constitute the pkey of this table
	 */
	public TableRepresentation(Connection inDb, String inTableName, String[] inPkey) throws SQLException {
		db = inDb;
		tableName = inTableName;
		pkey = inPkey;
		init();
	}

	/**
	 * Create a table representation object
	 *
	 * @param inDb        a Connection object that allows this object to perform queries
	 * @param inTableName the name of the table that this object will represent
	 */
	public TableRepresentation(Connection inDb, String inTableName) throws SQLException {
		db = inDb;
		tableName = inTableName;
		init();
	}

	/**
	 * Create a table representation object
	 *
	 * @param inTableName the name of the table that this object will represent
	 * @param inPkey      a String array that is the names of the columns that constitute the primary key in this table
	 */
	public TableRepresentation(String inTableName, String[] inPkey) throws SQLException {
		tableName = inTableName;
		pkey = inPkey;
		init();
	}

	/**
	 * Create a table representation object
	 *
	 * @param inTableName the name of the table that this object will represent
	 * @param inPkey      a String that is the name of the primary key in this table
	 */
	public TableRepresentation(String inTableName, String inPkey) throws SQLException {
		tableName = inTableName;
		pkey = new String[1];
		pkey[0] = inPkey;
		init();
	}

	/**
	 * private method to initialize this object
	 */
	protected void init() throws SQLException {
		tableData = new TreeMap<String, Object>();
		dataInvalid = false;

		if (db != null) {
			//System.out.println("Dynamically initializing Table Representation");
			try {
				metaData = TableMetadata.fetchMetadata(db, tableName);

                String schemaName = null;

				DatabaseMetaData dmd = db.getMetaData();
				ResultSet rs = null;

				ArrayList<String> al = metaData.getPrimaryKeys();
				pkey = new String[al.size()];
				for (int t = 0; t < al.size(); t++) {
					pkey[t] = al.get(t);
				}

				for (String name : metaData.getColumnTypes().keySet()) {
					tableData.put(name, null);

					tableTypes.put(name, metaData.getColumnTypes().get(name));
				}

                rs = dmd.getIndexInfo(null,null,tableName,false,true);

                TreeMap<String, IndexRepresentation> indexColumns = new TreeMap<String, IndexRepresentation>();

                while (rs.next()) {
                    String iName = rs.getString(5);

                    if (iName!=null) {
                        IndexRepresentation ir;

                        if (indexColumns.containsKey(iName)) {
                            ir = indexColumns.get(iName);
                        }
                        else {
                            ir = new IndexRepresentation();
                        }

                        ir.setUnique(rs.getBoolean(4));
                        ir.setTableName(tableName);
                        ir.getColumns().add(rs.getString(9));
                        ir.setFilterCondition(rs.getString(12));
                    }

                }

                for (Entry<String, IndexRepresentation> e : indexColumns.entrySet()) {
                    IndexRepresentation ir = e.getValue();

                    StringBuilder sb = new StringBuilder();
                    for (String s : ir.getColumns()) {
                        sb.append("_");
                        sb.append(s);
                    }
                    if (e.getValue().isUnique()) {
                        ir.setName(tableName+sb.toString()+"ui");
                    }
                    else {
                        ir.setName(tableName+sb.toString()+"i");
                    }

                    getIndices().add(ir);
                }
			}
			catch (SQLException se) {
                throw se;
			}
		}
	}

	/**
	 * Load this object from the database.  primary key values must be provided already in order to perform a load, otherwise there is no way to figure out what data to load
	 */
	public void load() throws SQLException {

		if (db == null) {
			throw new PersistenceException("Database connection has not been specified when trying to load an object from the database");
		}

		ArrayList<Object> al = buildTableSelect();
		String query = (String) al.get(0);

		PreparedStatement st = db.prepareStatement(query);
		for (int t = 1; t < al.size(); t++) {
			st.setObject(t, al.get(t));
		}
		ResultSet rs = st.executeQuery();
		ResultSetMetaData rsmd = rs.getMetaData();
		while (rs.next()) {
			for (int t = 1; t <= rsmd.getColumnCount(); t++) {
				tableData.put(rsmd.getColumnName(t), rs.getObject(t));
			}
		}
	}

	/**
	 * Save the object to the database
	 */
	public void save() throws SQLException {
		if (dataInvalid) {
			log.info("I:" + invalidMsg);
			return;
		}

		if (db == null) {
			throw new PersistenceException("Database connection has not been specified when trying to save an object to the database");
		}

		// If we don't have a pkey in this object, then don't bother
		// trying to run a row check
		boolean runCheck = true;
		for (int t = 0; t < pkey.length; t++) {
			if (tableData.get(pkey[t]) == null) {
				System.out.println("a pkey was null "+pkey[t]);
				runCheck = false;
			}
			/*
					 else {
					   log.debug("Pkey "+t+": "+pkey[t]+" = "+tableData.get(pkey[t]));
					 }
					 */
		}

		// If we don't have a pkey at all - then we can't check
		if (pkey.length==0) {
			runCheck=false;
		}

		boolean doUpdate = runCheck;
		ResultSet rs = null;
		if (runCheck) {
			// Figure out if we have an existing row that matches our pkey
			ArrayList<Object> al = buildTableSelect();
			String query = (String) al.get(0);
			PreparedStatement stmt = db.prepareStatement(query);
			for (int t = 1; t < al.size(); t++) {
				stmt.setObject(t, al.get(t));
			}
			rs = stmt.executeQuery();

			if (!rs.next()) {
				doUpdate = false;
				if (debug) {
					System.out.println("N");
				}
			}
		}

		StringBuffer query = new StringBuffer("");
		ArrayList<Object> al = new ArrayList<Object>();
		al.add("");
		if (doUpdate) {
			TreeMap<String, Object> updateVals = new TreeMap<String, Object>();
			ResultSetMetaData rsmd = rs.getMetaData();
			String s = null;
			for (int t = 0; t < rsmd.getColumnCount(); t++) {
				s = rsmd.getColumnName(t + 1);
				/* Don't update if we are null and system is not
							if (tableData.get(s)==null && rs.getString(t+1)!=null) {
							  log.info("Mine is null, System is:"+rs.getString(t+1));
							  updateVals.put(s,tableData.get(s));
							}
							*/
				//log.info("For '"+s+"' Mine :"+tableData.get(s)+" System:"+rs.getString(t+1));
				if (tableData.get(s) != null && !(tableData.get(s)).equals(rs.getObject(t + 1))) {
					updateVals.put(s, tableData.get(s));
				}
				if (forceColumns.get(s) != null || (globalUpdate && unforceColumns.get(s) == null)) {
					updateVals.put(s, tableData.get(s));
				}
			}

			// If we have columns that need updating
			if (!updateVals.isEmpty()) {
				if (useOptimisticLocking) {
					StringBuffer sb = new StringBuffer();
					sb.append("select "+optimisticLock+">? from "+tableName+" where ");
					for (String x : pkey) {
						sb.append(x);
						sb.append("=? and ");
					}
					PreparedStatement olStmt = db.prepareStatement(sb.substring(0,sb.length()-4));
					for (int c = 0; c<pkey.length; c++) {
						olStmt.setObject(c+1, getObject(pkey[c]));
					}
					ResultSet olRs = olStmt.executeQuery();
					if (olRs.next()) {
						if (olRs.getBoolean(1)) {
							throw new ConcurrentRowUpdateException("Failed to update "+tableName+", data in database is newer than the record we were asked to save");
						}
					}
				}

				query.append("update ");
				query.append(tableName);
				query.append(" set ");
				Set<String> uks = updateVals.keySet();
				Iterator<String> it = uks.iterator();
				//System.out.print("(");
				while (it.hasNext()) {
					String key = (String) it.next();
					//System.out.print(key+",");
					if (tableData.get(key) == null) {
						query.append(key);
						query.append("=null, ");
					} else {
						query.append(key);
						query.append("=?, ");
						al.add(updateVals.get(key));
					}
				}
				//System.out.println(")");

				query = new StringBuffer(query.substring(0, query.length() - 2));

				query.append(" where ");
				for (int t = 0; t < pkey.length; t++) {
					Object pkeyData = tableData.get(pkey[t]);
					query.append(pkey[t]);
					query.append("=? and ");
					al.add(pkeyData);
				}
				query = new StringBuffer(query.substring(0, query.length() - 5));
			}
			else {
				if (debug) {
					System.out.println("E");
				}
			}
		} else {
			for (int t = 0; t < pkey.length; t++) {
				if (tableData.get(pkey[t]) == null) {
					Statement stmt = db.createStatement();
					rs = stmt.executeQuery("select nextval('" + pkey[t] + "_seq')");
					rs.next();
					if (tableTypes.get(pkey[t]) == Long.class) {
						tableData.put(pkey[t], rs.getLong(1));
					}
					else if (tableTypes.get(pkey[t]) == Integer.class) {
						tableData.put(pkey[t], rs.getInt(1));
					}
					else {
						throw new PersistenceException("Type of pkey '"+pkey[t]+"' is "+tableTypes.get(pkey[t])+" which I can't convert a sequence result to");
					}
				}
			}

			query.append("insert into ");
			query.append(tableName);
			query.append(" (");
			StringBuffer sb = new StringBuffer("");
			Iterator<String> it = tableData.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				query.append(key);
				query.append(",");

				if (tableData.get(key) == null) {
					sb.append("null, ");
				} else {
					sb.append("?, ");
					al.add(tableData.get(key));
				}
			}

			sb.delete(sb.length() - 2, sb.length());
			query.delete(query.length() - 1, query.length());

			query.append(") values (");
			query.append(sb.toString());
			query.append(")");
		}

		if (query.toString() != "") {
			PreparedStatement ps = db.prepareStatement(query.toString());
			if (debug) {
				log.info("Query " + query.toString());
			}
			try {
				for (int t = 1; t < al.size(); t++) {
					ps.setObject(t, al.get(t));
				}
				ps.executeUpdate();
			}
			catch (SQLException se) {
				System.out.println("Failed query " + query + " : " + se.getMessage());
				se.printStackTrace();
				if (!ignoreExceptions) {
					throw se;
				}
			}
		}
	}

	public ArrayList<Object> buildTableSelect() {
		ArrayList<Object> al = new ArrayList<Object>();
		al.add("");
		StringBuffer query = new StringBuffer("select * from ");
		Object pkeyData = null;
		query.append(tableName);
		query.append(" where ");
		for (int i = 0; i < pkey.length; i++) {
			query.append(pkey[i]);
			pkeyData = tableData.get(pkey[i]);
			if (pkeyData == null) {
				query.append(" is null and ");
			} else {
				query.append("=? and ");
				al.add(pkeyData);
			}
		}

		query = new StringBuffer(query.substring(0, query.length() - 5));
		al.set(0, query.toString());

		return al;
	}

	/**
	 * Set a given field to a given piece of data
	 *
	 * @param inField Which field to set
	 * @param inDataA What to set it to
	 */
	public void set(String inField, String[] inDataA) {
		String inData = inDataA[0];
		set(inField, inData);
	}

	public void set(String inField, String inData) {
		if (debug) {
			log.info("Asked to set " + inField + " to " + inData);
		}
		if (tableTypes.containsKey(inField)) {
			// Null is valid, and doesn't need parsing
			if (inData == null) {
				tableData.put(inField, inData);
				return;
			}

			Class c=null;

			try {
				c = tableTypes.get(inField);
				if (debug) {
					log.info("Setting Data to " + c + " class");
				}
				if (c.equals(String.class)) {
					tableData.put(inField, inData);
				}
				else if (c.equals(Integer.class)) {
					tableData.put(inField, Integer.parseInt(inData));
				}
				else if (c.equals(Long.class)) {
					tableData.put(inField, Long.parseLong(inData));
				}
				else if (c.equals(Double.class)) {
					tableData.put(inField, Double.parseDouble(inData));
				}
				else if (c.equals(Float.class)) {
					tableData.put(inField, Float.parseFloat(inData));
				}
				else if (c.equals(java.sql.Date.class)) {
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
					if (inData.charAt(2) == '-') {
						sdf = new SimpleDateFormat("MM-dd-yyyy");
					}
					if (inData.charAt(7) == '-') {
						sdf = new SimpleDateFormat("yyyy-MM-dd");
					}
					if (inData.charAt(7) == '/') {
						sdf = new SimpleDateFormat("yyyy/MM/dd");
					}
					try {
						java.util.Date d = sdf.parse(inData);
						java.sql.Date sd = new java.sql.Date(d.getTime());
						tableData.put(inField, sd);
					}
					catch (ParseException pe) {
						throw new RuntimeException("Failed to parse date " + inData+" for type Date");
					}
				}
				else if (c.equals(java.sql.Timestamp.class)) {
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
					if (inData.length() > 18) {
						sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSS");
						if (inData.charAt(2) == '-') {
							sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss:SSS");
						}
						if (inData.charAt(7) == '-') {
							sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
						}
						if (inData.charAt(7) == '/') {
							sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");
						}
					}
					if (inData.length() > 15) {
						sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
						if (inData.charAt(2) == '-') {
							sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
						}
						if (inData.charAt(7) == '-') {
							sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						}
						if (inData.charAt(7) == '/') {
							sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						}
					} else {
						if (inData.charAt(2) == '-') {
							sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm");
						}
						if (inData.charAt(7) == '-') {
							sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						}
						if (inData.charAt(7) == '/') {
							sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
						}
					}
					try {
						java.util.Date d = sdf.parse(inData);
						java.sql.Timestamp st = new java.sql.Timestamp(d.getTime());
						tableData.put(inField, st);
					}
					catch (ParseException pe) {
						throw new RuntimeException("Failed to parse date " + inData+" for type Timestamp");
					}
				}
				else if (c.equals(Boolean.class)) {
					Boolean b = false;
					if (inData.equals("on")) {
						b = true;
					} else if (inData.equals("t")) {
						b = true;
					} else if (inData.equals("1")) {
						b = true;
					} else if (inData.equals("y")) {
						b = true;
					} else if (inData.equals("Y")) {
						b = true;
					}
					tableData.put(inField, b);
				}
			}
			catch (NumberFormatException nfe) {
				throw new RuntimeException("failed to parse value for " + inField + "data was "+inData+" type was "+c);
			}
		} 
		else {
			throw new RuntimeException("Can't work with an unkown type for column "+inField+","+inData);
		}
	}

	/**
	 * Set a given field to a given piece of data
	 *
	 * @param inField Which field to set
	 * @param inData  What to set it to
	 */
	public void set(String inField, int inData) {
		tableData.put(inField, new Integer(inData));
	}

	/**
	 * Set a given field to a given piece of data
	 *
	 * @param inField Which field to set
	 * @param inData  What to set it to
	 */
	public void set(String inField, long inData) {
		tableData.put(inField, new Long(inData));
	}

	/**
	 * Set a given field to a given piece of data
	 *
	 * @param inField Which field to set
	 * @param b       What to set it to
	 */
	public void setBoolean(String inField, Boolean b) {
		if (b.booleanValue()) {
			tableData.put(inField, "t");
		} else {
			tableData.put(inField, "f");
		}
	}

	/**
	 * Set a given field to a given piece of data
	 *
	 * @param inField Which field to set
	 * @param b       What to set it to
	 */
	public void set(String inField, Boolean b) {
		setBoolean(inField, b);
	}

	/**
	 * Set a boolean field to a string which will contain Y/y,N/n
	 *
	 * @param inField Which field to set
	 * @param inData  What to set it to
	 */
	public void setYN(String inField, String inData) {
		if (inData == null) {
			tableData.put(inField, inData);
			return;
		}
		if (inData.equals("Y")) {
			setBoolean(inField, true);
		} else if (inData.equals("y")) {
			setBoolean(inField, true);
		} else if (inData.equals("N")) {
			setBoolean(inField, false);
		} else if (inData.equals("n")) {
			setBoolean(inField, false);
		} else {
			log.warn("warning didn't find Y or N for bool, found '" + inData + "'");
			tableData.put(inField, inData);
		}
	}

	/**
	 * Set a field to a date
	 *
	 * @param inField The field name
	 * @param inData  The date in the format MM-DD-YYYY or MM/DD/YYYY
	 */
	public void setDate(String inField, String inData) {
		String month = inData.substring(0, 2);
		String day = inData.substring(3, 5);
		String year = inData.substring(6);

		tableData.put(inField, year + "-" + month + "-" + day);
	}

	/**
	 * Set a Field to an object
	 *
	 * @param inField The field name
	 * @param inData  The data object
	 */
	public void setObject(String inField, Object inData) {
		if (inData instanceof String[]) {
			String[] s = (String[]) inData;
			set(inField, s);
		} else {
			//System.out.println("Settings " + inField + " to " + inData);
			tableData.put(inField, inData);
		}
	}

	/**
	 * Retrieve a column value as an Object
	 * @param inField
	 * @return
	 */
	public Object get(Object inField) {
		return tableData.get(inField);
	}

	/**
	 * Retrieve a column value as an integer
	 * @param inField
	 * @return
	 */
	public int getInt(String inField) {
		return Integer.parseInt((String) tableData.get(inField));
	}

	/**
	 * Retrieve a column value as an object
	 * @param inField
	 * @return
	 */
	public Object getObject(String inField) {
		return tableData.get(inField);
	}

	/**
	 * Mark this object's data as invalid, which means it will simply no-op when asked to save
	 */
	public void setDataInvalid() {
		dataInvalid = true;
	}

	/**
	 * Set a message describing why the data in this object is invalid
	 * @param msg
	 */
	public void setDataInvalid(String msg) {
		setDataInvalid();
		invalidMsg = msg;
	}

	/**
	 * Is the data in this object invalid?
	 * @return a boolean
	 */
	public boolean isDataInvalid() {
		return dataInvalid;
	}

	/**
	 * Set the database connection this object is associated with
	 * @param inDb
	 */
	public void setDatabase(Connection inDb) {
		db = inDb;
	}

	/**
	 * Set the database connection this object is associated with
	 * @param c
	 */
	public void setConnection(Connection c) {
		setDatabase(c);
	}

	/**
	 * Force a given column to save itself even if it's value doesn't look like it's changed
	 * @param inColumn
	 */
	public void forceColumnUpdate(String inColumn) {
		forceColumns.put(inColumn, "yes");
	}

	/**
	 * If we tell the object to force all columns to update, we can exclude columns from that list with this method
	 * @param inColumn
	 */
	public void unforceColumnUpdate(String inColumn) {
		forceColumns.remove(inColumn);
		unforceColumns.put(inColumn, "yes");
	}

	/**
	 * Force all columns to update when we save
	 * @param b
	 */
	public void setGlobalUpdate(boolean b) {
		globalUpdate = b;
	}

	/**
	 * Do we force all columns to update when we save
	 * @return
	 */
	public boolean getGlobalUpdate() {
		return globalUpdate;
	}

	public void setDebug(boolean b) {
		debug = b;
	}

	public boolean getDebug() {
		return debug;
	}

	/**
	 * Pass a map object full of name value pairs and it will populate the object with those name value pairs
	 * If those name value pairs are in camelCase, it will automatically convert them to underscore notation:
	 * so myTableColumn will become my_table_column
	 */
	public void setMap(Map<String, Object> t) {
		Iterator<String> it = t.keySet().iterator();
		while (it.hasNext()) {
			String k = it.next();
			if (debug) {
				System.out.println("k is " + k);
			}
			String v = Utility.toUnderscores(k);
			Object value = t.get(k);
			if (tableData.containsKey(v)) {
				//System.out.println("setting "+v+":"+value.getClass());
				// If it's an empty string and the class of the type is not a string,
				// set it to null.
				// This will probably bugger TableRepresentations that don't
				// use the type map, we should figure something
				// out for that.
				if (value instanceof String[] && !tableTypes.get(v).equals(String.class)) {
					if (debug) {
						System.out.println("Checking for blank string");
					}
					String valueS = ((String[]) value)[0];
					if (valueS.trim().equals("")) {
						value = null;
					}
				}
				setObject(v, value);
			}
		}
	}

	/**
	 * Set the corresponding java type for a column
	 * @param column
	 * @param type
	 */
	public void setType(String column, Class type) {
		tableTypes.put(column, type);
	}

	/**
	 * Should we silently ignore exceptions?  Why you might ask would we want to do this.
	 * Well, if we are doing a bulk load from a dataprovider that doesn't understand the
	 * meaning of referential integrity, we get crap records, that we don't want to throw
	 * the whole job.
	 *
	 * @param b a boolean indicating wether or not to ignore exceptions
	 */
	public void setIgnoreExceptions(boolean b) {
		ignoreExceptions = b;
	}

	/**
	 * return the type map for this object
	 * @return
	 */
	public Map<String, Class> getTypeMap() {
		return tableTypes;
	}


	/**
	 * return the name of the table that this object is holding data for
	 * @return
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Set the table name to something new - used for object re-use or for moving data from one table to another
	 * @param s
	 */
	public void setTableName(String s) {
		tableName=s;
	}

	/**
	 * return a Set object that holds all the key in the table Data map
	 * @return
	 */
	public Set keySet() {
		return tableData.keySet();
	}

	public boolean containsKey(Object test) {
		return tableData.containsKey(test);
	}

	public boolean containsValue(Object test) {
		return tableData.containsValue(test);
	}

	public int size() {
		return tableData.size();
	}

	public Set<Map.Entry<String, Object>> entrySet() {
		return tableData.entrySet();
	}

	public boolean equals(Object o) {
		if (o instanceof TreeMap) {
			TreeMap<String, Object> t = (TreeMap<String, Object>) o;
			if (tableData.equals(t)) {
				return true;
			}
		}
		return false;
	}

	public int hashCode() {
		return tableData.hashCode();
	}

	public boolean isEmpty() {
		return tableData.isEmpty();
	}

	public Object put(String k, Object v) {
		setObject(k, v);
		return v;
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		tableData.putAll(m);
	}

	public Object remove(Object k) {
		Object o = tableData.get(k);
		tableData.remove(k);
		tableTypes.remove(k);
		return o;
	}

	public Collection<Object> values() {
		return tableData.values();
	}

	/**
	 * Reset this object for re-use
	 *   resets tableData, tableType, tableGuidance, unforceColumns, forceColumns and globalUpdate
	 */
	public void clear() {
		tableData.clear();
		tableTypes.clear();
		unforceColumns.clear();
		forceColumns.clear();
		globalUpdate=false;
	}

	public String[] getPrimaryKeys() {
		return pkey;
	}

	public void setPrimaryKeys(String[] s) {
		pkey=s;
	}

	public void setPrimaryKey(String s) {
		pkey=new String[1];
		pkey[0]=s;
	}

	public void renameColumn(String oldName, String newName) {
		Class c=tableTypes.get(oldName);
		Object data=tableData.get(oldName);
        Guidance g = metaData.getTableGuidance().get(oldName);

		tableTypes.put(newName,c);
		tableData.put(newName,data);
		metaData.getTableGuidance().put(newName,g);

		tableTypes.remove(oldName);
		tableData.remove(oldName);
		metaData.getTableGuidance().remove(oldName);
	}

	/** Fetch all the rows from this table and put them in a ResultsData object
	 * Don't use this if the table is big, use a CursorData object instead
	 * TODO build a CursorData object for operation with a cursor
	 * @return A ResultsData object with all the data loaded in it
	 * @throws SQLException thrown if the object could not load the data from the database for some reason
	 */
	public ResultsData fetchTable() throws SQLException {
		StringBuffer order=new StringBuffer();
		for (String a : pkey) {
			order.append(a+" asc, ");
		}
		return fetchTable(order.substring(0,order.length()-2));
	}

	/** Fetch all the rows from this table and put them in a ResultsData object
	 * Don't use this if the table is big, use a CursorData object instead
	 * TODO build a CursorData object for operation with a cursor
	 * @param order An order by string to pass to the SQL Query TODO make this less SQLy and use column/order pairs instead
	 * @return A ResultsData object with all the data loaded in it
	 * @throws SQLException thrown if the object could not load the data from the database for some reason
	 */
	public ResultsData fetchTable(String order) throws SQLException {
		QueryBuilder qb=QueryBuilderFactory.getQueryBuilder(db);
		String table=qb.addTable(tableName);
		for (String column : tableData.keySet()) {
			qb.addColumn(table, column);
		}
		// TODO fix this so it's not a hack.  This is a cheat and maynot work in future or with all databases
		qb.addOrder(table, order, "");

		return new ResultsData(db, qb);
	}

	/** Fetch all the rows from this table and put them in a ResultsData object given a pre-existing QueryBuilder
	 * that can be used to construct thinks like limit, order by and where clauses before being passed in
	 * Don't use this if the table is big, use a CursorData object instead
	 * @param queryBuilder b A query builder object
	 * @return A ResultsData object with all the data loaded in it
	 * @throws SQLException thrown if the object could not load the data from the database for some reason
	 */
	public ResultsData fetchTable(QueryBuilder queryBuilder) throws SQLException {
		String table=queryBuilder.addTable(tableName);
		for (String column : tableData.keySet()) {
			queryBuilder.addColumn(table, column);
		}

		return new ResultsData(db, queryBuilder);
	}

	public String[] getPkey() {
		return pkey;
	}

    public String getTypeName(String column) throws TableBuildException {
        if (typeNameFactory == null) {
            throw new TableBuildException("typeNameFactory is null, and you can't ask for a type name from a table representation without setting the type name factory");
        }
        return typeNameFactory.getTypeName(tableTypes.get(column));
    }

	/**
	 * Remove an entry from the data map
	 * This is sometimes useful if you want to stash temporary information in a TR object
	 * @param key The key of the value you wish to remove
	 */
	public void unset(String key) {
		tableData.remove(key);
	}

	public String getOptimisticLock() {
		return optimisticLock;
	}

	public void setOptimisticLock(String optimisticLock) {
		this.optimisticLock = optimisticLock;
	}

	public boolean isUseOptimisticLocking() {
		return useOptimisticLocking;
	}

	public void setUseOptimisticLocking(boolean useOptimisticLocking) {
		this.useOptimisticLocking = useOptimisticLocking;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("TableRepresentation for table '");
		sb.append(tableName);
		sb.append("' {");
		for (Entry<String,Object> e : tableData.entrySet()) {
			String v = e.getValue().toString();
			if (v.length()>20) {
				v = v.substring(1,20)+"...";
			}

			String prefix= "    ";
			for (String x : pkey) {
				if (x.equals(e.getKey())) {
					prefix=" PK ";
				}
			}

			sb.append(prefix);
			sb.append(e.getKey());
			sb.append(" : ");
			sb.append(v);
			sb.append("\n");
		}

		sb.append("}\n");

		return sb.toString();
	}

    public Connection getDb() {
        return db;
    }

    public void setDb(Connection db) {
        this.db = db;
    }

    public TreeMap<String, Object> getTableData() {
        return tableData;
    }

    public void setTableData(TreeMap<String, Object> tableData) {
        this.tableData = tableData;
    }

    public TreeMap<String, Class> getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(TreeMap<String, Class> tableTypes) {
        this.tableTypes = tableTypes;
    }

    public String getInvalidMsg() {
        return invalidMsg;
    }

    public void setInvalidMsg(String invalidMsg) {
        this.invalidMsg = invalidMsg;
    }

    public TreeMap<String, String> getForceColumns() {
        return forceColumns;
    }

    public void setForceColumns(TreeMap<String, String> forceColumns) {
        this.forceColumns = forceColumns;
    }

    public TreeMap<String, String> getUnforceColumns() {
        return unforceColumns;
    }

    public void setUnforceColumns(TreeMap<String, String> unforceColumns) {
        this.unforceColumns = unforceColumns;
    }

    public List<IndexRepresentation> getIndices() {
        return indices;
    }

    public void setIndices(List<IndexRepresentation> indices) {
        this.indices = indices;
    }

    public TableMetadata getMetaData() {
        return metaData;
    }

    public void setMetaData(TableMetadata metaData) {
        this.metaData = metaData;
    }

    public TypeNameFactory getTypeNameFactory() {
        return typeNameFactory;
    }

    public void setTypeNameFactory(TypeNameFactory typeNameFactory) {
        this.typeNameFactory = typeNameFactory;
    }

    public boolean hasLongOrIntId() {
        return metaData.hasLongOrIntId();
    }
}
