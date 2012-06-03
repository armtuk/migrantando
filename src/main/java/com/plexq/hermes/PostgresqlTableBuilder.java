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

import java.sql.Types;
import java.util.*;
import java.sql.SQLException;
import java.sql.Statement;

/**
 */
public class PostgresqlTableBuilder extends TableBuilder {
    public String buildTable(TableRepresentation tr) throws SQLException, TableBuildException {
        Map<String, Class> types = tr.getTypeMap();
        final Map<String, Guidance> guidance = tr.getMetaData().getTableGuidance();

        StringBuffer sql=new StringBuffer();

        sql.append("create table ");
        sql.append(convertToUnderscoreFormat(tr.getTableName()));
        sql.append(" (\n");

        ArrayList<String> sortedKeys = new ArrayList<String>();
        sortedKeys.addAll(types.keySet());
        Collections.sort(sortedKeys, new Comparator<String>() {
            @Override
            public int compare(String s, String s1) {
                return guidance.get(s).getFieldPosition() - guidance.get(s1).getFieldPosition();
            }
        });

        for (String a: sortedKeys) {
            Guidance g=guidance.get(a);
            sql.append(a);
            sql.append(" ");
            sql.append(getDatabaseTypeForJavaClass(types.get(a), g));
            if (g!=null) {
                if (!g.isNullable()) {
                    sql.append(" not null");
                }
                if (g.getDefaultValue()!=null) {
                    sql.append(" default "+g.getDefaultValue());
                }
            }

            sql.append(",\n");
        }

        sql=new StringBuffer(sql.substring(0,sql.length()-2));

        for (String a: guidance.keySet()) {

            Guidance g=guidance.get(a);
            if (g!=null && g.getForeignKey()!=null) {
                sql.append(",\n");
                ForeignKey fk=g.getForeignKey();
                sql.append("constraint ");
                sql.append(convertToUnderscoreFormat(tr.getTableName()));
                sql.append("_");
                sql.append(a);
                sql.append("_fk");
                sql.append(" foreign key (");
                sql.append(a);
                sql.append(") references ");
                sql.append(fk.getTableName());
                sql.append(" on delete ");
                String deleteAction=fk.getDeleteAction();
                if (deleteAction==ForeignKey.CASCADE) {
                    sql.append(" cascade");
                }
                else if (deleteAction==ForeignKey.NO_ACTION) {
                    sql.append(" no action");
                }
            }
        }

        String pkeys[]=tr.getPrimaryKeys();
        if (pkeys.length>0) {
            sql.append(",\nprimary key (");

            for (int t=0;t<pkeys.length;t++) {
                sql.append(pkeys[t]);
                sql.append(",");
            }
            sql=new StringBuffer(sql.substring(0,sql.length()-1));

            sql.append(")");
        }

        sql.append(");");

        sql.append("\n");

        for (IndexRepresentation ir : tr.getIndices()) {
            sql.append("create ");
            if (ir.isUnique()) {
                sql.append("unique ");
            }
            sql.append("index ");
            sql.append(ir.getName());
            sql.append(" on ");
            sql.append(ir.getTableName());
            sql.append(" (");
            StringBuilder lsb = new StringBuilder();
            for (String s : ir.getColumns()) {
                if (lsb.length()!=0) {
                    lsb.append(",");
                }
                lsb.append(s);
            }
            sql.append(lsb.toString());
            sql.append(");\n");
            ir.getColumns();
        }

        return sql.toString();


        /*
        Statement st=db.createStatement();

        System.out.println(sql);

        st.executeUpdate(sql.toString());
        */
    }

    public String getDatabaseTypeForJavaClass(Class c, Guidance g) throws TableBuildException {

        if (c==String.class) {
            if (g==null || g.getNativeTypeName().equals("text")) {
                return "text";
            }
            else {
                if (g.getJavaSQLType()== Types.SQLXML || g.getNativeTypeName().equals("xml")) {
                    return "xml";
                }
                else {
                    return "text";
                }
            }

        }
        else if (c==Float.class || c==Double.class) {
            if (g==null) {
                return "float8";
            }
            else {
                if (g.getPrecision()!=-1 && g.getPrecisionB()!=-1) {
                    return "numeric("+g.getPrecision()+","+g.getPrecisionB()+")";
                }
                else {
                    return "float8";
                }
            }
        }
        else if (c==Integer.class) {
            return "int";
        }
        else if (c==Long.class) {
            return "int8";
        }
        else if (c==java.sql.Date.class) {
            return "date";
        }
        else if (c==java.sql.Timestamp.class) {
            if (g==null) {
                return "timestamp";
            }
            else {
                if (g.hasTimeZone()) {
                    return "timestamp with timezone";
                }
                else {
                    return "timestamp";
                }
            }
        }
        else if (c==Boolean.class) {
            return "boolean";
        }
        else if (c==byte[].class) {
            return "bytea";
        }

        throw new TableBuildException("Unable to map type "+c+" to a type for a database column");
    }
}
