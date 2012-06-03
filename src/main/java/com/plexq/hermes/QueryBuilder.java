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

import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * The QueryBuilder class is designed to build a SQL query programatically.  It can be used to either build string based statement or a PreparedStatement object pre-filled with objects ready for execution.
 */
public interface QueryBuilder {
  /**
   * Add a table to the query.
   *
   * @param inTableName a String that is the name of the table in the database
   * @return A String ojbect that is the table's handle
   */
  String addTable(String inTableName);

  /**
   * Add a column to the left hand side of the expression, i.e. this column
   * Will be in the ResultSet of the query
   *
   * @param inTableHandle A table handle as return by addTable()
   * @param inColumnName  The name of the column in the database
   */
  void addColumn(String inTableHandle, String inColumnName);

  void addColumn(String inTableHandle, String[] inColumnNames);

  void addColumn(String inTableHandle, String[] inColumnNames, String[] inAliasNames);

  void addColumn(String inTableHandle, String inColumnName, String inAliasName);

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
  void addDateColumn(String inTableHandle, String inColumnName, String inAliasName);

  /**
   * Add a column formatted nicely as a timestamp - see addDateColumn for format
   *
   * @param inTableHandle the table handle for the table
   * @param inColumnName  the column name to represent
   * @param inAliasName   a return alias for this functionated column
   */
  void addTimestampColumn(String inTableHandle, String inColumnName, String inAliasName);


  /*
    * Add a column formatted as a number.  Format is as follows:
    * <pre>
    * 9	value with the specified number of digits
    * 0	value with leading zeros
    * . (period)	decimal point
    * , (comma)	group (thousand) separator
    * </pre>
    * @param inTableHandle the table handle of the table this column comes from
    * @param inColumnName the name of the column
    * @param inFormat The numeric format to use
    * @param inAliasName The alias name to alias this functionated column to.
    */
  void addNumericColumn(String inTableHandle, String inColumnName, String inFormat, String inAliasName);

  /**
   * Add a column wrapped by a function so it will become like lower(&lt;column&gt;)
   *
   * @param inTableHandle a table handle as returned by addTable();
   * @param inColumnName  the name of a column in that table in the database
   * @param prefix        what to put before the column
   * @param suffix        what to put after the column
   * @param alias         The alias name of the column
   *                      <blockquote>
   *                      qb.addColumnWithFunc(handle, "first_name","lower(",")");
   *                      </blockquote>
   */
  void addColumnWithFunc(String inTableHandle, String inColumnName, String prefix, String suffix, String alias);

  /**
   * Add a where clause to the statement
   *
   * @param inTableHandle a table handle as returned by addTable()
   * @param inColumnName  a column name in the table from the database
   * @param inCondition   a condition statement
   * @param inValue       an Object of containing the parameter for the prepared statement
   *                      <blockquote>
   *                      String name=request.getParameter("name");
   *                      qb.addWhere(handle, "first_name", "=?", name);
   *                      </blockquote>
   */
  Integer addWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue);

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
  Integer addWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue, String inWhereGroup);

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
  Integer[] addIn(String inTableHandle, String inColumnName, List<Object> inValues);

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
  Integer[] addIn(String inTableHandle, String inColumnName, Object[] inValues);

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
  Integer[] addIn(String inTableHandle, String inColumnName, Object[] inValues, String inWhereGroup);

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
  Integer[] addFuncIn(String inTableHandle, String inColumnName, List<Object> inValues, String inPrefix, String inSuffix);

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
  Integer[] addFuncIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix);

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
  Integer[] addFuncIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix, String inWhereGroup);

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
  Integer[] addNotIn(String inTableHandle, String inColumnName, List<Object> inValues);

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
  Integer[] addNotIn(String inTableHandle, String inColumnName, Object[] inValues);

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
  Integer[] addNotIn(String inTableHandle, String inColumnName, Object[] inValues, String inWhereGroup);

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
  Integer[] addFuncNotIn(String inTableHandle, String inColumnName, List<Object> inValues, String inPrefix, String inSuffix);

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
  Integer[] addFuncNotIn(String inTableHandle, String inColumnName, Object[] inValues, String inPrefix, String inSuffix, String inWhereGroup);

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
  void addSubQueryIn(String inTableHandle, String inColumnName, String inSubQuery);

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
  void addSubQueryIn(String inTableHandle, String inColumnName, String subQuery, String inWhereGroup);

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
  void addSubQueryIn(String inTableHandle, String inColumnName, String prefix, String suffix, String subQuery);

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
  void addSubQueryIn(String inTableHandle, String inColumnName, String prefix, String suffix, String subQuery, String inWhereGroup);

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
  void addWhereSubQuery(QueryBuilder inSubQuery, String inCondition);

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
  void addWhereSubQuery(QueryBuilder inSubQuery, String inCondition, String inWhereGroup);

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
  Integer[] addWhereSubQuery(String inSubQuery, String inCondition, Object[] inParams);

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
  Integer[] addWhereSubQuery(String inSubQuery, String inCondition, Object[] inParams, String inWhereGroup);

  Integer[] addSubQueryIn(String inTableHandle, String inColumnName, QueryBuilder inQB);

  Integer[] addSubQueryIn(String inTableHandle, String inColumnName, QueryBuilder inQB, String inWhereGroup);

  Integer addLowerWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue);

  Integer addLowerWhere(String inTableHandle, String inColumnName, String inCondition, Object inValue, String inWhereGroup);

  void addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition);

  Integer[] addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition, Object[] params);

  Integer[] addFuncWhere(String inTableHandle, String inColumnName, String inPrefix, String inSuffix, String inCondition, Object[] inValues, String inWhereGroup);

  void addTableWhere(String inTableHandle1, String inColumnName1, String inTableHandle2, String inColumnName2);

  void addTableWhere(String inTableHandle1, String inColumnName1, String inTableHandle2, String inColumnName2, String inWhereGroup);

  void addOrder(String inTableHandle, String inColumn, String inOrder);

  void addDirectJoin(String inTableHandle1, String inColumn1, String inTableHandle2, String inColumn2, String type);

  PreparedStatement getPreparedStatement(Connection db) throws java.sql.SQLException;

  String getQueryString();

  ArrayList<Object> getQueryStringBare();

  String toString();

  String addWhereGroup(String name, String op);

  void setDebug(boolean b);

  void resetTableIndex();

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
	Integer getParameterPosition(Integer key);

	/**
   * Start at row offset in the results set as return by the database
   *
   * @param offset
   */
	void setOffset(int offset);

	/**
   * Return at most limit rows
   *
   * @param limit
   */
	void setLimit(int limit);
}
