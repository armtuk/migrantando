package com.plexq.hermes;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: plexq
 * Date: 3/3/11
 * Time: 3:03 PM
 */
public class CreateDDL {
    public CreateDDL() {

    }

    public String create(Connection db, String schema, TableBuilder tb) throws SQLException,TableBuildException {
        StringBuilder sb = new StringBuilder();

        ResultSet rs;

        DatabaseMetaData dmd = db.getMetaData();
        rs = dmd.getTables(null,schema,null,null);

        while (rs.next()) {
            String tableName = rs.getString(3);

            TableRepresentation tr = new TableRepresentation(db, tableName);

            sb.append(tb.buildTable(tr));
            sb.append("\n");
        }

        return sb.toString();
    }
}
