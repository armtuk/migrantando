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

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;

/**
 *
 * @author plexq
 * @date Jun 29, 200:
 */
public class ResultsData extends LinkedList<Map<String, Object>> {
	public ResultsData(Connection db, QueryBuilder qb) throws SQLException {
		super();

		PreparedStatement ps=qb.getPreparedStatement(db);

		System.out.println("ResultsData comming from "+qb.getQueryStringBare());

		ResultSet rs=ps.executeQuery();
		ResultSetMetaData rsmd=rs.getMetaData();

		while (rs.next()) {
			HashMap<String,Object> m=new HashMap<String,Object>();
			for (int t=1;t<=rsmd.getColumnCount();t++) {
				String columnName=rsmd.getColumnName(t);
				m.put(columnName, rs.getObject(columnName));
			}
			this.add(m);
		}
	}
}
