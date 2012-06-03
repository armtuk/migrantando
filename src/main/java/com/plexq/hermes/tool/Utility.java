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
package com.plexq.hermes.tool;

public class Utility {
  public static String toCamelCase(String s) {
    if (s == null) {
      return null;
    }
    if (s.equals("")) {
      return "";
    }

    StringBuffer in = new StringBuffer(s);
    in.replace(0, 1, in.substring(0, 1).toUpperCase());
    while (in.indexOf("_") != -1) {
      int x = in.indexOf("_");
      in.replace(x, x + 2, in.substring(x + 1, x + 2).toUpperCase());
    }

    return in.toString();
  }

  public static String toCamelCaseVar(String s) {
    if (s == null) {
      return null;
    }

    StringBuffer in = new StringBuffer(s);
    while (in.indexOf("_") != -1) {
      int x = in.indexOf("_");
      in.replace(x, x + 2, in.substring(x + 1, x + 2).toUpperCase());
    }

    return in.toString();
  }

  public static String toUnderscores(String s) {
    if (s == null) {
      return null;
    }
    if (s.equals("")) {
      return "";
    }

    StringBuffer sb = new StringBuffer(s);
    char c;
    char lastChar='a';
    for (int t = 0; t < sb.length(); t++) {
      c = sb.charAt(t);

      if (Character.isUpperCase(c) || Character.isDigit(c)) {
        if (t>0 && Character.isDigit(c) && Character.isDigit(sb.charAt(t-1))) {
          // no-op
        }
        else if (t>0 && Character.isUpperCase(c) && Character.isUpperCase(lastChar)) {
          sb.replace(t, t + 1, "" + Character.toLowerCase(c));
        }
        else {
          sb.replace(t, t + 1, "_" + Character.toLowerCase(c));
          t+=1;
        }
      }
      // Skip over the char so we don't re-replace it
      if (Character.isDigit(c)) {
        t += 1;
      }
      lastChar=c;
    }

    if (sb.substring(0, 1).equals("_")) {
      return sb.substring(1);
    } else {
      return sb.toString();
    }
  }
}
