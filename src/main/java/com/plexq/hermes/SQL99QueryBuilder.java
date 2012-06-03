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

import org.apache.log4j.*;

import java.util.*;
import java.text.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * The QueryBuilder class is designed to build a SQL query programatically.  It can be used to either build string based statement or a PreparedStatement object pre-filled with objects ready for execution.
 */
public abstract class SQL99QueryBuilder implements QueryBuilder {
	/**
	 * The Logger for log4j
	 */
	private static Logger log = Logger.getLogger(SQL99QueryBuilder.class);

	/**
	 * A Map object off tables and their associated handle
	 */
	protected TreeMap<String, String> tables = new TreeMap<String, String>();
	/**
	 * A Map object of columns
	 */
	protected TreeMap<String, String> columns = new TreeMap<String, String>();
	/**
	 * An ArrayList of order statements
	 */
	protected ArrayList<String> order = new ArrayList<String>();
	/**
	 * An array of direct join pieces
	 */
	protected ArrayList<JoinData> directJoins = new ArrayList<JoinData>();
	/**
	 * A list of the whereGroups in the system
	 */
	//protected TreeMap<String, TreeMap<String, Object>> whereGroups = new TreeMap<String, TreeMap<String, Object>>();
	protected TreeMap<String, TreeMap<String, TreeMap<String,Object[]>>> whereGroups = new TreeMap<String, TreeMap<String, TreeMap<String,Object[]>>>();
	protected TreeMap<String, String> whereGroupOps=new TreeMap<String, String>();
	/**
	 * A Map of places where parameters occur
	 */
	protected TreeMap<String, Integer[]> parameterPositionMap = new TreeMap<String, Integer[]>();
	/**
	 * A Map of internal positions to PreparedStatement positions
	 */
	protected TreeMap<Integer, Integer> parameterPositionMapMap = new TreeMap<Integer, Integer>();

	/**
	 * Which letter we are currently using for table handles
	 */
	protected static int tableIndex = 65;
	/**
	 * Which letter we are using as a prefix for table handles
	 */
	protected static String tableIndexPrefix = "A";
	/**
	 * Are we in debug mode
	 */
	protected boolean debug = false;

	/**
	 * The query that will be output
	 */
	protected StringBuffer query;

	/**
	 * The limit
	 */
	protected int limit = -1;

	/**
	 * The offset
	 */
	protected int offset = -1;

	/**
	 * The time-stamp format
	 */
	protected String timestampFormat = "MM/DD/YYYY HH:MI:SSam";

	/**
	 * The date format
	 */
	protected String dateFormat = "MM/DD/YYYY";

	/**
	 * Integer token that will be assigned to arguments for the preparedStatement
	 */
	protected static int tokenCounter = 0;

	/**
	 * Reserved two letter words
	 */
	protected static ArrayList<String> reserved = new ArrayList<String>();

	static {
		reserved.add("AS");
		reserved.add("AT");
		reserved.add("BY");
		reserved.add("DO");
		reserved.add("IN");
		reserved.add("IS");
		reserved.add("LN");
		reserved.add("NO");
		reserved.add("OF");
		reserved.add("ON");
		reserved.add("OR");
		reserved.add("TO");
	}

	/**
	 * The constructor
	 */
	public SQL99QueryBuilder() {
		addWhereGroup("_default", "and");
	}

	/**
	 * Add a table to the query.
	 *
	 * @param inTableName a String that is the name of the table in the database
	 * @return A String object that is the table's handle
	 */
	public String addTable(String inTableName) {
		inTableName = inTableName.toLowerCase();
		if (!tables.containsKey(inTableName)) {
			tables.put(inTableName, tableIndexPrefix + (char) tableIndex);
			if (debug) {
				log.debug("Adding table " + inTableName);
			}
			// Loop around
			tableIndex++;
			// Do this to avoid building a query select AS.* from table AS

			String s = tableIndexPrefix + (char) tableIndex;
			while (reserved.contains(s)) {
				tableIndex++;
				s = tableIndexPrefix + (char) tableIndex;
			}

			if (tableIndex > 90) {
				tableIndexPrefix = "" + (char) (tableIndexPrefix.charAt(0) + 1);
				if (tableIndexPrefix.charAt(0) > 90) {
					tableIndexPrefix = "A";
				}
				tableIndex = 65;
			}
		}
		return tables.get(inTableName);
	}

	/**
	 * Add a column to the left hand side of the expression, i.e. this column
	 * Will be in the ResultSet of the query
	 *
	 * @param inTableHandle A table handle as return by addTable()
	 * @param inColumnName  The name of the column in the database
	 */
	public void addColumn(String inTableHandle, String inColumnName) {
		String ref = inTableHandle + "." + inColumnName;
		columns.put(ref, "1");
		if (debug) {
			log.debug("Adding column " + inTableHandle + "." + inColumnName + "\n");
		}
	}


	public void addColumn(String inTableHandle, String inColumnName, String inAliasName) {
		String ref = inTableHandle + "." + inColumnName + " as " + inAliasName;
		columns.put(ref, "1");
		if (debug) {
			log.debug("Adding column " + inTableHandle + "." + inColumnName + " as " + inAliasName);
		}
	}

	public void addColumn(String inTableHandle, String[] inColumnNames) {
		for (String s : inColumnNames) {
			addColumn(inTableHandle, s);
		}
	}

	public void addColumn(String inTableHandle, String[] inColumnNames, String[] inAliasNames) {
		for (int t = 0; t < inColumnNames.length; t++) {
			addColumn(inTableHandle, inColumnNames[t], inAliasNames[t]);
		}
	}

	/**
	 * Add a formatted date column to the left hand side of the expression, i.e. this column
	 * Will be in the ResultSet of the query alias as the alias name given.
	 * <p/>
	 * HH	hour of day (01-12)
	 * HH12	hour of day (01-12)
	 * HH24	hour of day (00-23)
	 * MI	minute (00-59)
	 * SS	second (00-59)
	 * MS	millisecond (000-999)
	 * US	microsecond (000000-999999)
	 * SSSS	seconds past midnight (0-86399)
	 * AM or A.M. or PM or P.M.	meridian indicator (uppercase)
	 * am or a.m. or pm or p.m.	meridian indicator (lowercase)
	 * Y,YYY	year (4 and more digits) with comma
	 * YYYY	year (4 and more digits)
	 * YYY	last 3 digits of year
	 * YY	last 2 digits of year
	 * Y	last digit of year
	 * IYYY	ISO year (4 and more digits)
	 * IYY	last 3 digits of ISO year
	 * IY	last 2 digits of ISO year
	 * I	last digits of ISO year
	 * BC or B.C. or AD or A.D.	era indicator (uppercase)
	 * bc or b.c. or ad or a.d.	era indicator (lowercase)
	 * MONTH	full uppercase month name (blank-padded to 9 chars)
	 * Month	full mixed-case month name (blank-padded to 9 chars)
	 * month	full lowercase month name (blank-padded to 9 chars)
	 * MON	abbreviated uppercase month name (3 chars)
	 * Mon	abbreviated mixed-case month name (3 chars)
	 * mon	abbreviated lowercase month name (3 chars)
	 * MM	month number (01-12)
	 * DAY	full uppercase day name (blank-padded to 9 chars)
	 * Day	full mixed-case day name (blank-padded to 9 chars)
	 * day	full lowercase day name (blank-padded to 9 chars)
	 * DY	abbreviated uppercase day name (3 chars)
	 * Dy	abbreviated mixed-case day name (3 chars)
	 * dy	abbreviated lowercase day name (3 chars)
	 * DDD	day of year (001-366)
	 * DD	day of month (01-31)
	 * D	day of week (1-7; Sunday is 1)
	 * W	week of month (1-5) (The first week starts on the first day of the month.)
	 * WW	week number of year (1-53) (The first week starts on the first day of the year.)
	 * IW	ISO week number of year (The first Thursday of the new year is in week 1.)
	 * CC	century (2 digits)
	 * J	Julian Day (days since January 1, 4712 BC)
	 * Q	quarter
	 * RM	month in Roman numerals (I-XII; I=January) (uppercase)
	 * rm	month in Roman numerals (i-xii; i=January) (lowercase)
	 * TZ	time-zone name (uppercase)
	 * tz	time-zone name (lowercase)
	 *
	 * @param inTableHandle A table handle as return by addTable()
	 * @param inColumnName  The name of the column in the database
	 * @param inAliasName   the alias to user for this column
	 */
	public abstract void addDateColumn(String inTableHandle, String inColumnName, String inAliasName);

	public void setDateFormat(String s) {
		dateFormat = s;
	}

	public void setTimestampFormat(String s) {
		timestampFormat = s;
	}


	/**
	 * Add a column wrapped by a function so it will become like lower(&lt;column&gt;)
	 *
	 * @param inTableHandle a table handle as returned by addTable();
	 * @param inColumnName  the name of a column in that table in the database
	 * @param prefix        what to put before the column
	 * @param suffix        what to put after the column
	 * @param alias         The name to alias to column to
	 *                      <blockquote>
	 *                      qb.addColumnWithFunc(handle, "first_name","lower(",")", "first_name_lower");
	 *                      </blockquote>
	 */
	public void addColumnWithFunc(String inTableHandle, String inColumnName, String prefix, String suffix, String alias) {
		inColumnName = inColumnName.toLowerCase();
		String ref = prefix + inTableHandle + "." + inColumnName + suffix + " as "+alias;
		columns.put(ref, "1");
	}

	/**
	 * Add a where clause to the statement
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column name in the table from the database
	 * @param inCondition   a condition statement
	 * @param inValue       an Object containing the parameter for the prepared statement
	 *                      <blockquote>
	 *                      String name=request.getParameter("name");
	 *                      qb.addWhere(handle, "first_name", "=", name);
	 *                      </blockquote>
	 */
	public Integer addWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue) {
		return addWhere(inTableHandle, inColumnName, inCondition, inValue, "_default");
	}

	/**
	 * Add a where clause to the statement
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column name in the table from the database
	 * @param inCondition   a condition statement
	 * @param inValue       an Object containing the parameter for the prepared statement
	 * @param inWhereGroup  a String name of a where group
	 *                      <blockquote>
	 *                      String name=request.getParameter("name");
	 *                      qb.addWhere(handle, "first_name", "='?'", new Object[] {name}, "names");
	 *                      </blockquote>
	 */
	public Integer addWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue, String inWhereGroup) {
		TreeMap<String, Object[]> where = (whereGroups.get(inWhereGroup).get("where"));
		if (inValue == null) {
			throw new NullPointerException("params for addWhere cannot be null");
		}

		if (inCondition.indexOf("?")==-1) {
			where.put(inTableHandle + "." + inColumnName + inCondition + "?", new Object[]{inValue});
			parameterPositionMap.put(inTableHandle + "." + inColumnName + inCondition + "?", new Integer[]{tokenCounter});
		}
		else {
			where.put(inTableHandle + "." + inColumnName + inCondition, new Object[]{inValue});
			parameterPositionMap.put(inTableHandle + "." + inColumnName + inCondition, new Integer[]{tokenCounter});
		}

		return tokenCounter++;
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a List object of values for the in group
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      ArrayList a;
	 *                      a.add(1);
	 *                      a.add(4);
	 *                      a.add(5);
	 *                      qb.addIn(handle, "type_id",a);
	 *                      yields:
	 *                      select * from foo where type_id in (1,3,5)
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addIn(String inTableHandle, String inColumnName, List<Object> inValues) {
		return addIn(inTableHandle, inColumnName, inValues.toArray(), "_default");
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      an Object[] of objects of values for the in group
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addIn(handle, "type_id",new Object[] {1,3,5});
	 *                      yields:
	 *                      select * from foo where (type_id in (1,3,5))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addIn(String inTableHandle, String inColumnName, Object[] inValues) {
		return addIn(inTableHandle, inColumnName, inValues, "_default");
	}

	/**
	 * Add an in clause to the statement
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      an Object[] of objectcs of values for the in group
	 * @param inWhereGroup  a where group name
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addWhereGroup("other","or");
	 *                      qb.addWhereGroup("main","or");
	 *                      qb.addIn(handle,"type_id" new Object[] {1,5,7},"main");
	 *                      qb.addIn(handle,"some_id",new Object[] {1,3,5},"other")
	 *                      yields:
	 *                      select * from foo where (type_id in (1,5,7)) and (some_id in (1,3,5))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addIn(String inTableHandle, String inColumnName, Object[] inValues, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("in"));
		StringBuffer sb = new StringBuffer("");
		sb.append(inTableHandle);
		sb.append(".");
		sb.append(inColumnName);
		sb.append(" in (");
		ArrayList<Integer> al = new ArrayList<Integer>();
		for (int t = 0; t < inValues.length; t++) {
			if (t > 0) {
				sb.append(",");
			}
			sb.append("?");
			al.add(tokenCounter++);
		}
		sb.append(")");

		tableIn.put(sb.toString(), inValues);
		parameterPositionMap.put(sb.toString(), al.toArray(new Integer[al.size()]));
		return al.toArray(new Integer[al.size()]);
	}

	/**
	 * Add an in clause to the statment with a function wrapped around the column
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      an List object containing the values for the in group
	 * @param inPrefix      a prefix
	 * @param inSuffix      a suffix
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      ArrayList a;
	 *                      a.add("phil");
	 *                      a.add("steve");
	 *                      a.add("joe");
	 *                      qb.addFuncIn(handle, "first_name",a, "lower(",")");
	 *                      yields:
	 *                      select * from foo where (first_name in ('phil','steve','joe'))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addFuncIn(String inTableHandle, String inColumnName, List<Object> inValues, String inPrefix, String inSuffix) {
		return addFuncIn(inTableHandle, inColumnName, inValues.toArray(), inPrefix, inSuffix, "_default");
	}

	/**
	 * Add an in clause to the statment with a function wrapped around the column
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      an Object[] containing a list for the in group
	 * @param inPrefix      a prefix
	 * @param inSuffix      a suffix
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addFuncIn(handle, "first_name", new Object[] {'phil','steve','joe'}, "lower(",")");
	 *                      yields:
	 *                      select * from foo where (first_name in ('phil','steve','joe'))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addFuncIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix) {
		return addFuncIn(inTableHandle, inColumnName, inValues, inPrefix, inSuffix, "_default");
	}

	/**
	 * Add an in clause to the statment with a function wrapped around the column
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      an Object[] containing a list for the in group
	 * @param inPrefix      a prefix
	 * @param inSuffix      a suffix
	 * @param inWhereGroup  a where group name
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addIn(handle,"last_name",new Object[] {'smith'});
	 *                      qb.addWhereGroup("names");
	 *                      qb.addFuncIn(handle, "first_name", new Object[] {'phil','steve','joe'}, "lower(",")","names");
	 *                      yields:
	 *                      select * from foo where (last_name in 'smith') and (first_name in ('phil','steve','joe'))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addFuncIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("in"));
		StringBuffer sb = new StringBuffer("");
		sb.append(inPrefix);
		sb.append(inTableHandle);
		sb.append(".");
		sb.append(inColumnName);
		sb.append(inSuffix);
		sb.append(" in (");
		ArrayList<Integer> al = new ArrayList<Integer>();
		for (int t = 0; t < inValues.length; t++) {
			if (t > 0) {
				sb.append(",");
			}
			sb.append("?");
			al.add(t);
		}
		sb.append(")");

		tableIn.put(sb.toString(), inValues);
		parameterPositionMap.put(sb.toString(), al.toArray(new Integer[al.size()]));
		return al.toArray(new Integer[al.size()]);
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a List object of values for the not in group
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      ArrayList a;
	 *                      a.add(1);
	 *                      a.add(4);
	 *                      a.add(5);
	 *                      qb.addNotIn(handle, "type_id",a);
	 *                      yields:
	 *                      select * from foo where type_id not in (1,3,5)
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addNotIn(String inTableHandle, String inColumnName, List<Object> inValues) {
		return addNotIn(inTableHandle, inColumnName, inValues.toArray(), "_default");
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a Object[] of values for the not in group
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addNotIn(handle, "type_id",new Object[] {1,3,5});
	 *                      yields:
	 *                      select * from foo where type_id not in (1,3,5)
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addNotIn(String inTableHandle, String inColumnName, Object[] inValues) {
		return addNotIn(inTableHandle, inColumnName, inValues, "_default");
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a Object[] of values for the not in group
	 * @param inWhereGroup  a where group name
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addIn(handle, "some_id", new Object[] {4,7,9});
	 *                      qb.addNotIn(handle, "type_id",new Object[] {1,3,5},"name");
	 *                      yields:
	 *                      select * from foo where (some_id in (4,7,9)) and (type_id not in (1,3,5))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addNotIn(String inTableHandle, String inColumnName, Object[] inValues, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("in"));
		StringBuffer sb = new StringBuffer("");
		sb.append(inTableHandle);
		sb.append(".");
		sb.append(inColumnName);
		sb.append(" not in (");
		Integer[] n = new Integer[inValues.length];
		for (int t = 0; t < inValues.length; t++) {
			if (t > 0) {
				sb.append(",");
			}
			sb.append("?");
			n[t] = tokenCounter++;
		}
		sb.append(")");

		tableIn.put(sb.toString(), inValues);
		parameterPositionMap.put(sb.toString(), n);
		return n;
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a Object[] of values for the not in group
	 * @param inPrefix      the start of the function
	 * @param inSuffix      the end of the function
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addNotIn(handle, "type_id",new Object[] {1,3,5},"count(",")");
	 *                      yields:
	 *                      select * from foo where count(type_id) not in (1,3,5)
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addFuncNotIn(String inTableHandle, String inColumnName, List<Object> inValues, String inPrefix, String inSuffix) {
		return addFuncNotIn(inTableHandle, inColumnName, inValues.toArray(), inPrefix, inSuffix, "_default");
	}

	/**
	 * Add an in clause to the statment
	 *
	 * @param inTableHandle a table handle as returned by addTable()
	 * @param inColumnName  a column from the table in the database
	 * @param inValues      a Object[] of values for the not in group
	 * @param inPrefix      the start of the function
	 * @param inSuffix      the end of the function
	 * @param inWhereGroup  which where group to put this in
	 *                      <blockquote>
	 *                      <pre>
	 *                      ...
	 *                      qb.addIn(handle,"foo",new Object[] {'fish','lemon'});
	 *                      qb.addNotIn(handle, "type_id",new Object[] {1,3,5},"count(",")","first");
	 *                      yields:
	 *                      select * from foo where (foo in ('fish','lemon') and (count(type_id) not in (1,3,5))
	 *                      </pre>
	 *                      </blockquote>
	 */
	public Integer[] addFuncNotIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("in"));
		StringBuffer sb = new StringBuffer("");
		sb.append(inPrefix);
		sb.append(inTableHandle);
		sb.append(".");
		sb.append(inColumnName);
		sb.append(" not in (");
		ArrayList<Integer> al = new ArrayList<Integer>();
		for (int t = 0; t < inValues.length; t++) {
			if (t > 0) {
				sb.append(",");
			}
			sb.append("?");
			al.add(tokenCounter++);
		}
		sb.append(")");

		tableIn.put(sb.toString(), inValues);
		parameterPositionMap.put(sb.toString(), al.toArray(new Integer[al.size()]));
		return al.toArray(new Integer[al.size()]);
	}

	/**
	 * Add a subquery in the where statement with an in group
	 *
	 * @param inTableHandle a table handle as returned by addTable
	 * @param inColumnName  a column name that is whats in the subquery
	 * @param inSubQuery    a string subquery
	 *                      <blockquote>
	 *                      <pre>
	 *                      select * from table where column in (&lt;subquery&gt;);
	 *                      </pre>
	 *                      </blockquote>
	 */
	public void addSubQueryIn(String inTableHandle, String inColumnName, String inSubQuery) {
		addSubQueryIn(inTableHandle, inColumnName, inSubQuery, "_default");
	}

	/**
	 * Add a subquery in the where statement with an in group
	 *
	 * @param inTableHandle a table handle as returned by addTable
	 * @param inColumnName  a column name that is whats in the subquery
	 * @param subQuery      a string subquery
	 * @param inWhereGroup  group the where group to put this in
	 *                      <blockquote>
	 *                      <pre>
	 *                      select * from table where (column in (&lt;subquery&gt;)) and (other_column in (&lt;other subquery&gt;)
	 *                      </pre>
	 *                      </blockquote>
	 */
	public void addSubQueryIn(String inTableHandle, String inColumnName, String subQuery, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("where"));
		tableIn.put(inTableHandle + "." + inColumnName + " in (" + subQuery + ")", new Object[]{});
	}

	/**
	 * Add a subquery in the where statement with an in group
	 *
	 * @param inTableHandle a table handle as returned by addTable
	 * @param inColumnName  a column name that is whats in the subquery
	 * @param prefix        a prefix for the column
	 * @param suffix        a suffix for the column
	 * @param subQuery      a string subquery
	 *                      <blockquote>
	 *                      <pre>
	 *                      select * from table where prefix(column) in (&lt;subquery&gt;);
	 *                      </pre>
	 *                      </blockquote>
	 */
	public void addSubQueryIn(String inTableHandle, String inColumnName, String prefix, String suffix, String subQuery) {
		addSubQueryIn(inTableHandle, inColumnName, prefix, suffix, subQuery, "_default");
	}

	/**
	 * Add a subquery in the where statement with an in group
	 *
	 * @param inTableHandle a table handle as returned by addTable
	 * @param inColumnName  a column name that is whats in the subquery
	 * @param prefix        a prefix for the column
	 * @param suffix        a suffix for the column
	 * @param subQuery      a string subquery
	 * @param inWhereGroup  a where group name
	 *                      <blockquote>
	 *                      <pre>
	 *                      select * from table where (prefix(column) in (&lt;subquery&gt;)) and (other things here);
	 *                      </pre>
	 *                      </blockquote>
	 */
	public void addSubQueryIn(String inTableHandle, String inColumnName, String prefix, String suffix, String subQuery, String inWhereGroup) {
		TreeMap<String, Object[]> tableIn = whereGroups.get(inWhereGroup).get("where");
		tableIn.put(inTableHandle + "." + prefix + inColumnName + suffix + " in (" + subQuery + ")", new Object[]{});
	}

	/**
	 * Add a subquery that has a condition in the statement
	 *
	 * @param inSubQuery  the subquery to use
	 * @param inCondition the condition that the subquery should meet
	 *                    <blockquote>
	 *                    <pre>
	 *                    qb.addWhereSubQuery(subquery, "='1'");
	 *                    select * from table where (subquery)='1'
	 *                    </pre>
	 *                    </blockquote>
	 */
	public void addWhereSubQuery(QueryBuilder inSubQuery, String inCondition) {
		addWhereSubQuery(inSubQuery, inCondition, "_default");
	}

	/**
	 * Add a subquery that has a condition in the statement
	 *
	 * @param inSubQuery   a QueryBuilder object containing the subquery to use
	 * @param inCondition  the condition that the subquery should meet
	 * @param inWhereGroup the where group the put this sub query in
	 *                     <blockquote>
	 *                     <pre>
	 *                     qb.addWhereSubQuery(subquery, "='1'", "group_1");
	 *                     select * from table where ((subquery)='1') and (other stuff)
	 *                     </pre>
	 *                     </blockquote>
	 */
	public void addWhereSubQuery(QueryBuilder inSubQuery, String inCondition, String inWhereGroup) {
		TreeMap<String, Object[]> where = whereGroups.get(inWhereGroup).get("where");
		ArrayList<Object> a = inSubQuery.getQueryStringBare();
		String query = a.get(0).toString();
		a.remove(0);
		where.put("(" + query + ")" + inCondition, a.toArray());
	}

	/**
	 * Add a subquery that has a condition in the statement
	 *
	 * @param inSubQuery  a String object containing a subquery to use
	 * @param inCondition the condition that the subquery should meet
	 * @param inParams    an Object[] of parameters for the query
	 *                    <blockquote>
	 *                    <pre>
	 *                    qb.addWhereSubQuery("select count(*) from person", "='1'");
	 *                    select * from table where ((select count(*) from person)='1')
	 *                    </pre>
	 *                    </blockquote>
	 */
	public Integer[] addWhereSubQuery(String inSubQuery, String inCondition, Object[] inParams) {
		return addWhereSubQuery(inSubQuery, inCondition, inParams, "_default");
	}

	/**
	 * Add a subquery that has a condition in the statement
	 *
	 * @param inSubQuery   a String object containing a subquery to use
	 * @param inCondition  the condition that the subquery should meet
	 * @param inParams     an Object[] of parameters for the query
	 * @param inWhereGroup the where group the put this sub query in
	 *                     <blockquote>
	 *                     <pre>
	 *                     qb.addWhereSubQuery("select count(*) from person", "='1'", "group_1");
	 *                     select * from table where ((subquery)='1') and (other stuff)
	 *                     </pre>
	 *                     </blockquote>
	 */
	public Integer[] addWhereSubQuery(String inSubQuery, String inCondition, Object[] inParams, String inWhereGroup) {
		TreeMap<String, Object[]> where = (whereGroups.get(inWhereGroup).get("where"));
		String s = "(" + inSubQuery + ")" + inCondition;
		where.put(s, inParams);
		if (inParams.length > 0) {
			Integer[] n = new Integer[inParams.length];
			for (int t = 0; t < inParams.length; t++) {
				n[t] = tokenCounter++;
			}
			parameterPositionMap.put(s, n);
			return n;
		}
		return null;
	}

	public Integer[] addSubQueryIn(String inTableHandle, String inColumnName, QueryBuilder inQB) {
		return addSubQueryIn(inTableHandle, inColumnName, inQB, "_default");
	}

	public Integer[] addSubQueryIn(String inTableHandle, String inColumnName, QueryBuilder inQB, String inWhereGroup) {
		// Fix this to update inQB to have the right table refs
		TreeMap<String, Object[]> tableIn = (whereGroups.get(inWhereGroup).get("in"));
		ArrayList<Object> a = inQB.getQueryStringBare();
		String query = (String) a.get(0);
		a.remove(0);
		Integer[] n = null;
		if (a.size() > 0) {
			n = new Integer[a.size()];
			for (int t = 0; t < a.size(); t++) {
				n[t] = tokenCounter++;
			}
		}

		String s = inTableHandle + "." + inColumnName + " in (" + query + ")";
		tableIn.put(s, a.toArray());
		/** Remeber where stuff goes */
		parameterPositionMap.put(s, n);

		return n;
	}


	public Integer addLowerWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue) {
		return addLowerWhere(inTableHandle, inColumnName, inCondition, inValue, "_default");
	}

	public Integer addLowerWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue, String inWhereGroup) {
		TreeMap<String, Object[]> where = whereGroups.get(inWhereGroup).get("where");
		String s = "lower(" + inTableHandle + "." + inColumnName + ")" + inCondition + "?";
		where.put(s, new Object[]{inValue});

		Integer n = tokenCounter++;
		parameterPositionMap.put(s, new Integer[]{n});
		return n;
	}

	public void addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition) {
		addFuncWhere(inTableHandle, inColumnName, inPrefix, inSuffix, inCondition, new Object[]{}, "_default");
	}

	public Integer[] addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition, Object[] params) {
		return addFuncWhere(inTableHandle, inColumnName, inPrefix, inSuffix, inCondition, params, "_default");
	}

	public Integer[] addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition, Object[] inValues, String inWhereGroup) {
		TreeMap<String, Object[]> where = whereGroups.get(inWhereGroup).get("where");
		String s = inPrefix + inTableHandle + "." + inColumnName + inSuffix + inCondition;
		where.put(s, inValues);
		Integer[] n = null;
		if (inValues.length > 0) {
			n = new Integer[inValues.length];
			for (int t = 0; t < inValues.length; t++) {
				n[t] = tokenCounter++;
			}
		}

		parameterPositionMap.put(s, n);

		return n;
	}

	public void addTableWhere(String inTableHandle1, String inColumnName1, String inTableHandle2, String inColumnName2) {
		addTableWhere(inTableHandle1, inColumnName1, inTableHandle2, inColumnName2, "_default");
	}

	public void addTableWhere(String inTableHandle1, String inColumnName1, String inTableHandle2, String inColumnName2, String inWhereGroup) {
		TreeMap<String, Object[]> where = whereGroups.get(inWhereGroup).get("where");
		where.put(inTableHandle1 + "." + inColumnName1 + "=" + inTableHandle2 + "." + inColumnName2, new Object[]{});
	}

	public void addOrder(String inTableHandle, String inColumn, String inOrder) {
		order.add(inTableHandle + "." + inColumn + " " + inOrder);
	}

	public void addDirectJoin(String inTableHandle1, String inColumn1, String inTableHandle2, String inColumn2, String type) {
		JoinData jd = new JoinData();
		jd.setFromTableHandle(inTableHandle1);
		jd.setFromColumn(inColumn1);

		jd.setToTableHandle(inTableHandle2);
		jd.setToColumn(inColumn2);

		jd.setJoinType(type);

		directJoins.add(jd);
	}

	public PreparedStatement getPreparedStatement(Connection db) throws java.sql.SQLException {
		ArrayList<Object> a = getQueryStringBare();

		if (debug) {
			log.debug("Preparing " + a.get(0));

		}

		PreparedStatement pstmt = db.prepareStatement((String) a.get(0));

		for (int t = 1; t < a.size(); t++) {
			if (debug) {
				log.debug("setting " + t + " to " + a.get(t));
			}
			pstmt.setObject(t, a.get(t));
		}

		return pstmt;
	}

	public String getQueryString() {
		ArrayList<Object> a = getQueryStringBare();

		StringBuffer sb = new StringBuffer((String) a.get(0));
		if (debug) {
			log.debug("Query String Bare is " + sb);
		}

		int y;
		for (int t = 1; t < a.size(); t++) {
			y = sb.indexOf("?");
			Object o = a.get(t);

			if (o instanceof String) {
				sb.replace(y, y + 1, "'" + ((String) o).replace("'", "\\'") + "'");
			} else if (o instanceof java.sql.Date) {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
				sb.replace(y, y + 1, "'" + sdf.format((java.sql.Date) o) + "'");
			} else if (o instanceof java.util.Date) {
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
				sb.replace(y, y + 1, "'" + sdf.format((java.util.Date) o) + "'");
				/*
			} else if (o instanceof java.sql.Timestamp) {
				// We should really do this down to microseconds or better
				// but I can't get to java docs right now
				SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss Z");
				sb.replace(y, y + 1, "'" + sdf.format((java.sql.Date) o) + "'");
				*/
			} else {
				sb.replace(y, y + 1, o.toString());
			}
		}

		return sb.toString();
	}

	public ArrayList<Object> getQueryStringBare() {
		ArrayList<Object> a = new ArrayList<Object>();

		a.add("");

		query = new StringBuffer("select ");

		Integer position = 1;

		// Select columns
		query = appendColumnsString(query);

		query.append(" from ");

		log.debug("Building froms");

		query = appendTablesString(query);

		query = appendWhereString(query, a, position);

		//System.out.println("Query at end of Where: "+query);

		query = appendOrderString(query);

		query = appendOffsetLimit(query);

		//System.out.println("Query at end of Bare call: "+query);

		a.set(0, query.toString());

		return a;
	}

	public String toString() {
		return getQueryString();
	}

	public String addWhereGroup(String name, String op) {
		whereGroupOps.put(name, op);
		TreeMap<String, TreeMap<String, Object[]>> i = new TreeMap<String, TreeMap<String, Object[]>>();
		TreeMap<String, Object[]> k;
		k = new TreeMap<String, Object[]>();
		i.put("where", k);
		k = new TreeMap<String, Object[]>();
		i.put("in", k);
		k = new TreeMap<String, Object[]>();
		i.put("sub", k);
		whereGroups.put(name, i);

		return name;
	}

	public void setDebug(boolean b) {
		debug = b;
	}

	public void resetTableIndex() {
		tableIndex = 65;
		tableIndexPrefix = "A";
		log.warn("Resetting table index on QueryBuilder");
	}

	public static void concatenate(ArrayList<Object> a, Object[] o) {
		a.addAll(Arrays.asList(o));
	}

	/**
	 * Return the actual position of a parameter in a prepared statement by passing
	 * in a token that was handed out during query building.
	 * <pre>
	 * QueryBuilder qb=new QueryBuilder();
	 * String test=qb.addTable("test");
	 * qb.addColumn(test,"count");
	 * Integer i=qb.addWhere(test,"count>","10");
	 * PreparedStatement pstmt=qb.getPreparedStatement();
	 * ResultSet rs=pstmt.executeQuery();
	 * // Great - lets do it again - but wihtout re-parsing everything
	 * pstmt.setInt(getParameterPosition(i, 20);
	 * ResultSet rs=pstmt.executeQuery();
	 * </pre>
	 *
	 * @param key a token that was given when building
	 * @return The integer position in the preparedStatement of this variable
	 */
	public Integer getParameterPosition(Integer key) {
		return parameterPositionMapMap.get(key);
	}

	protected StringBuffer appendColumnsString(StringBuffer query) {
		Iterator<String> it;
		String s;

		it = columns.keySet().iterator();
		while (it.hasNext()) {
			s = it.next();
			query.append(s);
			query.append(",");
		}

		return new StringBuffer(query.substring(0, query.length() - 1));
	}

	protected StringBuffer appendTablesString(StringBuffer query) {
		Iterator<String> it;
		String s;
		ArrayList<String> directJoinTables = new ArrayList<String>();

		if (directJoins.size() > 0) {
			TreeMap<String, String> m = new TreeMap<String, String>();

			for (String k : tables.keySet()) {
				String v = tables.get(k);
				m.put(v, k);
			}

			// Direct Joins
			for (int t = 0; t < directJoins.size(); t++) {
				JoinData jd = directJoins.get(t);
				String h1 = jd.getFromTableHandle();
				if (t == 0) {
					query.append(m.get(h1));
					query.append(" as ");
					query.append(h1);
				}
				query.append(" ");
				query.append(jd.getJoinType());
				query.append(" join ");
				String h2 = jd.getToTableHandle();
				query.append(m.get(h2));
				query.append(" as ");
				query.append(h2);
				query.append(" on (");
				query.append(h1);
				query.append(".");
				query.append(jd.getFromColumn());
				query.append("=");
				query.append(h2);
				query.append(".");
				query.append(jd.getToColumn());
				query.append(")");

				if (!directJoinTables.contains(h1)) {
					directJoinTables.add(h1);
				}
				if (!directJoinTables.contains(h2)) {
					directJoinTables.add(h2);
				}
			}

			if (directJoinTables.size() < tables.keySet().size()) {
				query.append(",");
			} else {
				query.append(" ");
			}
		}

		// From tables
		it = tables.keySet().iterator();
		String alias;
		while (it.hasNext()) {
			s = it.next();
			alias = tables.get(s);
			if (!directJoinTables.contains(alias)) {
				if (debug) {
					log.debug("Added table from " + s + " " + alias);
				}
				query.append(s);
				query.append(" ");
				query.append(tables.get(s));
				query.append(",");

				if (debug) {
					log.debug("query is " + query);
				}
			}
		}

		return new StringBuffer(query.substring(0, query.length() - 1));
	}

	protected StringBuffer appendWhereString(StringBuffer query, ArrayList<Object> paramObjects, Integer position) {
		Object[] ax;
		Integer[] n;

		// Check first to see if we need a where statement
		for (String whereGroup: whereGroups.keySet()) {
			TreeMap<String, Object[]> where = whereGroups.get(whereGroup).get("where");
			TreeMap<String, Object[]> tableIn = whereGroups.get(whereGroup).get("in");

			if (!where.isEmpty() || !tableIn.isEmpty()) {
				query.append(" where ");
				break;
			}
		}

		// Loop Through Where Groups
		int count = 0;
		for (String whereGroup : whereGroups.keySet()) {
			TreeMap<String, Object[]> where = whereGroups.get(whereGroup).get("where");
			TreeMap<String, Object[]> tableIn = whereGroups.get(whereGroup).get("in");
			String op = whereGroupOps.get(whereGroup);

			// put in a bracket if we have a non empty group
			if (!where.isEmpty() || !tableIn.isEmpty()) {
				// Only put in a concatenating and if the group is not empty
				// and this is not the first part
				if (count > 0) {
					query.append(" and ");
				}

				query.append("(");

				// Only increment the counter if we actualy have data
				// in the where group otherwise if we have data
				// in a non-zero where group, we will get a spurious 'and'
				count++;
			}

			// where conditions
			if (!where.isEmpty()) {
				for (String s : where.keySet()) {
					query.append(s);
					query.append(" ");
					query.append(op);
					query.append(" ");
					ax = where.get(s);
					n = parameterPositionMap.get(s);
					concatenate(paramObjects, ax);
					for (int t = 0; t < ax.length; t++) {
						if (debug) {
							log.debug("Putting position " + position + " fot token " + n[t]);
						}
						parameterPositionMapMap.put(n[t], position++);
					}
				}

				query = new StringBuffer(query.substring(0, query.length() - (op.length() + 2)));
			}

			// and ins
			if (!tableIn.isEmpty()) {
				if (!where.isEmpty()) {
					query.append(" ");
					query.append(op);
					query.append(" ");
				}
				for (String s : tableIn.keySet()) {
					query.append(s);
					if (debug) {
						log.debug("Appending in " + s);
					}
					query.append(" ");
					query.append(op);
					query.append(" ");
					if (tableIn.get(s) == null) {
						log.error("Got a null Object[] for in " + s);
					} else {
						ax = tableIn.get(s);
						n = parameterPositionMap.get(s);
						concatenate(paramObjects, ax);
						for (int t = 0; t < ax.length; t++) {
							parameterPositionMapMap.put(n[t], position++);
						}
					}
				}

				query = new StringBuffer(query.substring(0, query.length() - (op.length() + 2)));
			}

			if (!where.isEmpty() || !tableIn.isEmpty()) {
				query.append(")");
			}

		}

		return query;
	}

	protected StringBuffer appendOrderString(StringBuffer query) {
		// order by
		if (!order.isEmpty()) {
			query.append(" order by ");
			for (String s : order) {
				query.append(s);
				query.append(",");
			}
			query = new StringBuffer(query.substring(0, query.length() - 1));
		}

		return query;
	}

	protected StringBuffer appendOffsetLimit(StringBuffer query) {
		if (offset!=-1) {
			query.append(" offset ");
			query.append(offset);
		}
		if (limit!=-1) {
			query.append(" limit ");
			query.append(limit);
		}

		return query;
	}

	/**
	 * The maximum number of rows to return in the result set
	 *
	 * @param i the limit value
	 */
	public void setLimit(int i) {
		limit=i;
	}

	/**
	 * The offset into the result set of the first row to be returned
	 *
	 * @param i the offset value
	 */
	public void setOffset(int i) {
		offset=i;
	}
}
