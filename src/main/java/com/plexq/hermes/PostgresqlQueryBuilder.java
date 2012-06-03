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

import org.apache.log4j.Logger;

/*
 * See notes on SQL99QueryBuilder - PostgreSQL is a perfectly compliant SQL 99 System.
 */
public class PostgresqlQueryBuilder extends SQL99QueryBuilder {
	/**
	 * The Logger for log4j
	 */
	private static Logger log = Logger.getLogger(SQL99QueryBuilder.class);

	/**
	 * Default constructor
	 */
	public PostgresqlQueryBuilder() {
		super();
	}

	public void addDateColumn(String inTableHandle, String inColumnName, String inAliasName) {
		String ref = "to_char(" + inTableHandle + "." + inColumnName + ",'" + dateFormat + "') as " + inAliasName;
		columns.put(ref, "1");
		if (debug) {
			log.debug("Adding column " + ref);
		}
	}

	public void addTimestampColumn(String inTableHandle, String inColumnName, String inAliasName) {
		String ref = "to_char(" + inTableHandle + "." + inColumnName + ",'" + timestampFormat + "') as " + inAliasName;
		columns.put(ref, "1");
		if (debug) {
			log.debug("Adding column " + ref);
		}
	}

	public void addNumericColumn(String inTableHandle, String inColumnName, String inFormat, String inAliasName) {
		String ref = "to_char(" + inTableHandle + "." + inColumnName + ",'" + inFormat + "') as " + inAliasName;
		columns.put(ref, "1");
		if (debug) {
			log.debug("Adding column " + ref);
		}
	}
}
