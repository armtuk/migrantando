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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

/**
 * Created by IntelliJ IDEA.
 * User: plexq
 * Date: Jun 5, 2008
 * Time: 11:48:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableBuilderFactory {
  public static TableBuilder getTableBuilder(Connection db) throws SQLException, TableBuildException {
    DatabaseMetaData dmd = db.getMetaData();
    String product = dmd.getDatabaseProductName();

    if (product.equals("PostgreSQL")) {
      return new PostgresqlTableBuilder();
    }

    throw new TableBuildException("No Query Builder available for database product " + product);
  }
}
