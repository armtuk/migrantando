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

/**
 * A class that represents TableBuilers.  These kinds of object can create a table over a database connection.  There is a subclass for each database type.
 * These can be acquired from TableBuilderFactory which reads the product string from the database connection
 *
 * Copyright (c) 2008 Alex R.M. Turner
 * Licensed Under the LGPL v3.0
 */
public abstract class TableBuilder {
    /**
     * Our database connection object
     */
    protected Connection db;

    /**
     * Set the database connection for this TableBuilder
     *
     * @param c A Database Connection object
     */
    public void setConnection(Connection c) {
        db = c;
    }

    /**
     * Construct the table from the table representation passed
     *
     * @param tr a table representation object which contains types and columns
     */
    public abstract String buildTable(TableRepresentation tr) throws SQLException, TableBuildException;

    public String convertToUnderscoreFormat(String s) {
        StringBuilder sb = new StringBuilder();
        for (Character a : s.toCharArray()) {
            if ("ABCDEFGHIJKLMNOPQRSTUVWXYZ".contains(""+a)) {
                sb.append("_");
            }
            sb.append(a);
        }
        return sb.toString().toLowerCase();
    }
}
