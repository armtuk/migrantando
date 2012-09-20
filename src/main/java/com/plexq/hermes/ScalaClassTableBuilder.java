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

import com.plexq.hermes.util.Util;

import java.io.*;
import java.sql.SQLException;
import java.util.*;

// TODO make this smart enough to figure out inheritance chain somehow, at least for our case, cope with mapping entity if appropriate
// TODO fuck inheritance chains - they suck.
public class ScalaClassTableBuilder extends TableBuilder {
    private String packageName;

    private String dumpPath;

    public ScalaClassTableBuilder() {
    }

    @Override
    public String buildTable(TableRepresentation tr) throws SQLException, TableBuildException {
        File f = new File(dumpPath + "/" + tr.getTableNameCamel() + ".scala");
        tr.setTypeNameFactory(new ScalaTypeNameFactory());
        try {
            FileWriter fw = new FileWriter(f);
            System.out.println("Writing to " + f.getPath());

            StringBuilder sql=new StringBuilder();

            sql.append("package ");
            sql.append(packageName);
            sql.append("\n\n");

            sql.append("\n" +
                    "import play.api.db._\n" +
                    "import play.api.Play.current\n" +
                    "\n" +
                    "import anorm._\n" +
                    "import anorm.SqlParser._\n" +
                    "import models.util.DBHelper\n" +
                    "import models.IdDriven\n" +
                    "import models.IdDriveCompanion\n");
            sql.append("\n");

            sql.append("/**\n * Autogenerated by ScalaClassTableBuilder\n */\n");

            sql.append(generateCaseClass(tr));
            sql.append(generateObject(tr));

            fw.write(sql.toString());
            fw.close();

            return sql.toString();
        }
        catch (IOException ioe) {
            throw new RuntimeException("FAILED", ioe);
        }
    }

    public String generateCaseClass(TableRepresentation tr) throws TableBuildException {
        StringBuilder sql = new StringBuilder();
        sql.append("case class ");
        sql.append(tr.getTableNameCamel());
        sql.append("(");

        Map<String, Guidance> guidance = tr.getMetaData().getTableGuidance();

        TreeMap<Integer, String> fieldOrderedColumnNames = new TreeMap<Integer, String>();


        for (Map.Entry<String, Guidance> e: guidance.entrySet()) {
            fieldOrderedColumnNames.put(e.getValue().getFieldPosition(), e.getKey());
        }

        if (fieldOrderedColumnNames.size()>22) {
            throw new TableBuildException("Failed to build class for table " + tr.getTableName() + " table has more than 22 colums, it has " + fieldOrderedColumnNames.size());
        }

        boolean useIdDriven = tr.hasLongOrIntId();
        if  (useIdDriven) {
            sql.append("override val id");
            sql.append(": Option[Long], ");

            fieldOrderedColumnNames.remove(guidance.get(tr.getPrimaryKeys()[0]).getFieldPosition());
        }

        for (String a: fieldOrderedColumnNames.values()) {
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append(": ");
            sql.append(tr.getTypeName(a));
            sql.append(", ");
        }
        sql = trimRight(sql, 2);
        sql.append(")");
        if (useIdDriven) {
            sql.append(" extends IdDriven");
        }
        sql.append("\n\n");
        return sql.toString();
    }


    public String generateObject(TableRepresentation tr) throws TableBuildException {
        StringBuilder sql = new StringBuilder();
        sql.append("object ");
        sql.append(tr.getTableNameCamel());
        sql.append(" extends IdDriveCompanion[").append(tr.getTableNameCamel()).append("]{\n");
        sql.append("\tval tableName =\"").append(tr.getTableName()).append("\"\n");
        sql.append(generateSimpleParser(tr, "simple", new AliasNameBuilder(tr){
            public String getAlias(String column) throws TableBuildException {
                return column;
            }
        }));
        sql.append(generateSimpleParser(tr, "result", new AliasNameBuilder(tr) {
            @Override
            public String getAlias(String column) throws TableBuildException {
                return tr.getTableName()+"."+column;
            }
        }));


        sql.append(buildFindByIdFunction(tr));
        sql.append(buildPersistFunction(tr));
        sql.append(buildUpdateFunction(tr));
        sql.append(buildSaveOrUpdateFunction(tr));

        sql.append("}");

        return sql.toString();
    }


    public String generateSimpleParser(TableRepresentation tr, String parserName, AliasNameBuilder anb) throws TableBuildException {
        StringBuilder sql = new StringBuilder();
        sql.append("\tval ").append(parserName).append(" = {\n");

        for (String column : tr.getMetaData().getColumnsInOrder()) {

            sql.append("\t\tget[");
            if (tr.hasLongOrIntId() && column.equals(tr.getPrimaryKeys()[0])) {
                sql.append("Option[Long]");
            }
            else {
                sql.append(tr.getTypeName(column));
            }
            sql.append("](\"");
            sql.append(anb.getAlias(column));
            sql.append("\") ~\n");
        }

        sql = trimRight(sql, 2);
        sql.append(" map {\n\t\t\tcase ");

        for (String column : tr.getMetaData().getColumnsInOrder()) {
            sql.append(Util.convertToCamelCaseFormat(column, false));
            sql.append(" ~ ");
        }

        sql = trimRight(sql, 2);

        sql.append("=> {\n\t\t\t\t");
        sql.append(tr.getTableNameCamel());
        sql.append("(");

        for (String column : tr.getMetaData().getColumnsInOrder()) {
            sql.append(Util.convertToCamelCaseFormat(column, false));
            sql.append(", ");
        }


        sql = trimRight(sql, 2);

        sql.append(")\n");

        // End Case
        sql.append("\t\t\t}\n");
        // End Map
        sql.append("\t\t}\n");
        // End val
        sql.append("\t}\n\n");

        return sql.toString();
    }

    public String buildPersistFunction(TableRepresentation tr) throws TableBuildException {
        StringBuilder sql = new StringBuilder();
        // Persist the Class to the database
        sql.append("\tdef persist(obj: ");
        sql.append(Util.convertToCamelCaseFormat(tr.getTableName(), true));
        sql.append(") = {\n");
        sql.append("\t\tDB.withTransaction { implicit connection =>\n");

        if (tr.hasLongOrIntId()) {
            sql.append("\t\t\tvar c = obj.copy(");

            sql.append("id = Some(DBHelper.nextId)).asInstanceOf[");
            sql.append(Util.convertToCamelCaseFormat(tr.getTableName(), true));
            sql.append("]\n\n");
        }
        else {
            sql.append("\t\t\tvar c = obj\n\n");
        }

        sql.append("\t\t\tSQL(\"insert into ");
        sql.append(tr.getTableName());
        sql.append(" (");

        for (String a: tr.getMetaData().getColumnsInOrder()) {
            sql.append(a);
            sql.append(", ");
        }

        sql = trimRight(sql, 2);

        sql.append(") \" + \n\t\t\t\t\"values (");

        for (String x: tr.getMetaData().getColumnsInOrder()) {
            String a = tr.hasLongOrIntId()&&tr.getPrimaryKeys()[0].equals(x) ? "id" : x;

            sql.append("{");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append("}, ");
        }
        sql = trimRight(sql, 2);

        sql.append(")\")\n");
        sql.append("\t\t\t.on(");


        for (String x: tr.getMetaData().getColumnsInOrder()) {
            String a = tr.hasLongOrIntId()&&tr.getPrimaryKeys()[0].equals(x) ? "id" : x;
            sql.append("'");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append(" -> c.");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append(", ");
        }

        sql = trimRight(sql, 2);

        sql.append(").executeInsert()\n");
        // End DB.withTransaction
        sql.append("\t\t}\n\tobj\n");
        // End def
        sql.append("\t}\n\n");

        return sql.toString();
    }

    public String buildFindByIdFunction(TableRepresentation tr) throws TableBuildException {
        StringBuilder sql = new StringBuilder();

        sql.append("\tdef read(id: ");
        if (tr.hasLongOrIntId()) {
            sql.append("Long");
        }
        else {
            sql.append(tr.getTypeName(tr.getPrimaryKeys()[0]));
        }
        sql.append(") = {\n\t\tDB.withConnection { implicit connection =>\n");
        sql.append("\t\t\tSQL(\"select * from ");
        sql.append(tr.getTableName());
        sql.append(" where ");
        sql.append(tr.getPrimaryKeys()[0]);
        sql.append(" = {id}\")\n\t\t\t\t.on('id -> id).as(");
        sql.append(tr.getTableNameCamel());
        sql.append(".simple.singleOpt)\n");
        // End Db Connection
        sql.append("\n\t\t}\n");
        // End def
        sql.append("\n\t}\n");

        return sql.toString();
    }

    public String buildUpdateFunction(TableRepresentation tr) {
        StringBuilder sql = new StringBuilder();

        sql.append("\tdef update(obj: ");
        sql.append(tr.getTableNameCamel());
        sql.append(") = {\n");
        sql.append("\t\tDB.withTransaction { implicit connection =>\n");
        sql.append("\t\t\tSQL(\"update ");
        sql.append(tr.getTableName());
        sql.append(" set ");

        for (String x : tr.getMetaData().getColumnsInOrder()) {
            String a = tr.hasLongOrIntId()&&tr.getPrimaryKeys()[0].equals(x) ? "id" : x;
            if (!Arrays.asList(tr.getPrimaryKeys()).contains(a)) {
                sql.append(x);
                sql.append(" = {");
                sql.append(Util.convertToCamelCaseFormat(a, false));
                sql.append("}, ");
            }
        }

        sql = trimRight(sql, 2);

        sql.append(" where ");

        for (String x: tr.getPrimaryKeys()) {
            String a = tr.hasLongOrIntId()&&tr.getPrimaryKeys()[0].equals(x) ? "id" : x;
            sql.append(x);
            sql.append(" = {");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append("} and ");
        }

        sql = trimRight(sql, 5);

        sql.append("\")\n");
        sql.append("\t\t\t.on(");

        for (String x: tr.getMetaData().getColumnsInOrder()) {
            String a = tr.hasLongOrIntId()&&tr.getPrimaryKeys()[0].equals(x) ? "id" : x;
            sql.append("'");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append(" -> obj.");
            sql.append(Util.convertToCamelCaseFormat(a, false));
            sql.append(", ");
        }

        sql = trimRight(sql, 2);
        sql.append(").executeUpdate()\n");
        //End DB with Transaction
        sql.append("\t\t}\n\tobj\n");
        // End def
        sql.append("\t}\n");

        return sql.toString();
    }

    public String buildSaveOrUpdateFunction(TableRepresentation tr) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\toverride def saveOrUpdate(t: ").append(tr.getTableNameCamel()).append(") = super.saveOrUpdate");
        sb.append("(t: ").append(tr.getTableNameCamel()).append(")\n");

        return sb.toString();
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setDumpPath(String dumpPath) {
        this.dumpPath = dumpPath;
    }

    public StringBuilder trimRight(StringBuilder s, Integer count) {
        return new StringBuilder(s.substring(0,s.length()-count));
    }

    public abstract class AliasNameBuilder {
        TableRepresentation tr;
        public AliasNameBuilder(TableRepresentation tr) {
            this.tr = tr;
        }

        public abstract String getAlias(String column) throws TableBuildException;
    }
}
