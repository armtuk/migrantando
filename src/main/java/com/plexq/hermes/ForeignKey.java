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

/** Foreign Key Objects are normaly tied to a specific column which is why there is no source column attribute.
 * Foreign key objects are used primarily in guidance to create a table
 * Author: plexq
 * Date: Jun 6, 2008
 */
public class ForeignKey {
  /** The table that this foreign key points to */
  protected String tableName;
  /** The column in the table that this foreign key points to */
  protected String ColumnName;
  /** The delete action */
  protected String deleteAction;

  public static final String CASCADE="cascade";
  public static final String NO_ACTION="no action";

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getColumnName() {
    return ColumnName;
  }

  public void setColumnName(String columnName) {
    ColumnName = columnName;
  }

  public String getDeleteAction() {
    return deleteAction;
  }

  public void setDeleteAction(String deleteAction) {
    this.deleteAction = deleteAction;
  }

}
