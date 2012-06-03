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

public class JoinData {
  protected String fromTableHandle = null;
  protected String fromColumn = null;
  protected String toTableHandle = null;
  protected String toColumn = null;
  protected String joinType = "inner";


  public void setFromTableHandle(String s) {
    fromTableHandle = s;
  }

  public String getFromTableHandle() {
    return fromTableHandle;
  }

  public void setFromColumn(String s) {
    fromColumn = s;
  }

  public String getFromColumn() {
    return fromColumn;
  }

  public void setToTableHandle(String s) {
    toTableHandle = s;
  }

  public String getToTableHandle() {
    return toTableHandle;
  }

  public void setToColumn(String s) {
    toColumn = s;
  }

  public String getToColumn() {
    return toColumn;
  }

  public void setJoinType(String s) {
    joinType = s;
  }

  public String getJoinType() {
    return joinType;
  }

  public boolean equals(JoinData jd) {
    return (jd.getFromTableHandle() == fromTableHandle &&
      jd.getFromColumn() == fromColumn &&
      jd.getToTableHandle() == toTableHandle &&
      jd.getToColumn() == toColumn &&
      jd.getJoinType() == joinType);
  }

  public String toString() {
    return getFromTableHandle() + " " + getJoinType() + " join " + getToTableHandle() + " on (" + getFromTableHandle() + "." + getFromColumn() + " " + getJoinType() + "=" + getToTableHandle() + "." + getToColumn() + ")";
  }
}
