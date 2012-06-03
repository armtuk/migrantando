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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * Author: plexq
 * Date: Jun 6, 2008
 */
public class Guidance {
    private int precisionA=-1;
    private int precisionB=-1;
    private ForeignKey foreignKey=null;
    private boolean index=false;
    private boolean typeChar=false;
    private boolean timeZone=false;
    private boolean nullable=true;
    private String nativeTypeName="";
    private int javaSQLType;
    private int fieldPosition=1;

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }

    public int getFieldPosition() {
        return fieldPosition;
    }

    public void setFieldPosition(int fieldPosition) {
        this.fieldPosition = fieldPosition;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    String defaultValue;

    public boolean hasTimeZone() {
        return timeZone;
    }

    public void setTimeZone(boolean b) {
        timeZone=b;
    }

    public boolean isTypeChar() {
        return typeChar;
    }

    public void setTypeChar(boolean typeChar) {
        this.typeChar = typeChar;
    }

    public void setPrecision(int x) {
        precisionA = x;
    }

    public void setPrecision(int x, int y) {
        precisionA = x;
        precisionB = y;
    }

    public int getPrecision() {
        return precisionA;
    }

    public int getPrecisionB() {
        return precisionB;
    }

    public void setForeignKey(ForeignKey foreignKey) {
        this.foreignKey=foreignKey;
    }

    public ForeignKey getForeignKey() {
        return foreignKey;
    }

    public void setIndexDesired(boolean b) {
        index=b;
    }

    public boolean isIndexDesired() {
        return index;
    }

    public static Guidance fetchGuidance(Connection db, String schemaName, String tableName, String columnName) throws SQLException {
        Guidance g = new Guidance();

        //System.out.println("Dynamically initializing Table Representation");
        DatabaseMetaData dmd = db.getMetaData();
        ResultSet rs = null;

        rs = dmd.getColumns(null, null, tableName, columnName);
        while (rs.next()) {
            g.setNullable(rs.getString(18).equals("YES"));
            if (rs.getInt(8)!=0) {
                g.setPrecision(rs.getInt(7),rs.getInt(9));
            }
            else {
                g.setPrecision(rs.getInt(9));
            }
            g.setDefaultValue(rs.getString(12));
            g.setJavaSQLType(rs.getInt(5));
            g.setNativeTypeName(rs.getString(6));
        }

        rs = dmd.getImportedKeys(null,null,tableName);
        while (rs.next()) {
            if (rs.getString(7).equalsIgnoreCase(columnName)) {
                ForeignKey fk = new ForeignKey();
                fk.setColumnName(columnName);
                fk.setTableName(rs.getString(3));
                g.setForeignKey(fk);
            }
        }

        return g;
    }

    public int getJavaSQLType() {
        return javaSQLType;
    }

    public void setJavaSQLType(int javaSQLType) {
        this.javaSQLType = javaSQLType;
    }

    public String getNativeTypeName() {
        return nativeTypeName;
    }

    public void setNativeTypeName(String nativeTypeName) {
        this.nativeTypeName = nativeTypeName;
    }
}
