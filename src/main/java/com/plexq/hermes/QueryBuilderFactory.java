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

public class QueryBuilderFactory {
	public static QueryBuilder getQueryBuilder(Connection db) throws SQLException {

		assert(db != null);

		DatabaseMetaData dmd = db.getMetaData();
		String product = dmd.getDatabaseProductName();

		if (product.equals("PostgreSQL")) {
			return new PostgresqlQueryBuilder();
		}

		throw new QueryBuilderException("No Query Builder available for database product " + product);
	}
}
