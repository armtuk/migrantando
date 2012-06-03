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

import org.junit.*;
import static org.junit.Assert.*;

public class UtilityTest {

  public static void main(String[] args) {
    org.junit.runner.JUnitCore.runClasses(UtilityTest.class);
  }

  @Test
  public void testToCamelCase() {
    String initial = "my_database_name";
    String expected = "MyDatabaseName";
    String result = Utility.toCamelCase(initial);
    assertTrue(expected.equals(result));
  }

  @Test
  public void testToCamelCaseNull() {
    String initial = null;
    String result = Utility.toCamelCase(initial);
    assertTrue(result == null);
  }

  @Test
  public void testToCamelCaseEmpty() {
    String initial = "";
    String expected = "";
    String result = Utility.toCamelCase(initial);
    assertTrue(expected.equals(result));
  }

  @Test
  public void testToCamelCaseVar() {
    String initial = "my_database_name";
    String expected = "myDatabaseName";
    String result = Utility.toCamelCaseVar(initial);
    assertTrue(expected.equals(result));
  }

  @Test
  public void testToCamelCaseVarNull() {
    String initial = null;
    String result = Utility.toCamelCaseVar(initial);
    assertTrue(result == null);
  }

  @Test
  public void testToCamelCaseVarEmpty() {
    String initial = "";
    String expected = "";
    String result = Utility.toCamelCaseVar(initial);
    assertTrue(expected.equals(result));
  }

  @Test
  public void testToUnderscores() {
    String initial = "MyCamelCaseName";
    String expected = "my_camel_case_name";
    String result = Utility.toUnderscores(initial);
    assertEquals(expected, result);
  }

  @Test
  public void testToUnderscoresLowerFirst() {
    String initial = "myCamelCaseName";
    String expected = "my_camel_case_name";
    String result = Utility.toUnderscores(initial);
    assertEquals(expected, result);
  }

  @Test
  public void testToUnderscoresNull() {
    String initial = null;
    String result = Utility.toUnderscores(initial);
    assertTrue(result == null);
  }

  @Test
  public void testToUnderscoresEmpty() {
    String initial = "";
    String expected = "";
    String result = Utility.toUnderscores(initial);
    assertEquals(expected, result);
  }

  @Test
  public void testToUnderscoresNumeric() {
    String initial = "address1";
    String expected = "address_1";
    String result = Utility.toUnderscores(initial);
    assertEquals(expected, result);
  }
}
