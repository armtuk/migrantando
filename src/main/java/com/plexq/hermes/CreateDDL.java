package com.plexq.hermes;

import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: plexq
 * Date: 3/3/11
 * Time: 3:03 PM
 */
public class CreateDDL {
    private String[] names;

    public CreateDDL() {
    }

    public CreateDDL(String[] names) {
        this.names = names;
    }

    public String create(Connection db, String schema, TableBuilder tb) throws SQLException,TableBuildException {
        StringBuilder sb = new StringBuilder();

        ResultSet rs;

        DatabaseMetaData dmd = db.getMetaData();
        if (names==null || names.length==0) {
            System.out.println("Getting tables...");
            rs = dmd.getTables(null,schema,null,null);
            processResultSet(db, tb, rs);
        }
        else {
            System.out.println("Select tables only...");
            for (String s: names) {
                String[] pieces = s.split("\\.");
                String lSchema = schema;
                String tableName = null;
                if (pieces.length==1) {
                    tableName = pieces[0];
                }
                if (pieces.length==2) {
                    lSchema = pieces[0];
                    tableName = pieces[1];
                }

                processResultSet(db, tb, dmd.getTables(null, lSchema, tableName, null));
            }
        }


        return sb.toString();
    }

    public String processResultSet(Connection db, TableBuilder tb, ResultSet rs) throws SQLException, TableBuildException {
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            String tableName = rs.getString(3);
            System.out.println("Table : " + tableName);

            TableRepresentation tr = new TableRepresentation(db, tableName);

            if (tr.getPrimaryKeys().length>0) {
                System.out.println("Building for table " + tr.getTableName());
                sb.append(tb.buildTable(tr));
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
