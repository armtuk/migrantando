package com.plexq.hermes.util;

public class Util {
    public static String convertToCamelCaseFormat(String s, boolean initialCap) {
        StringBuilder sb =  new StringBuilder();

        // Start this as true so that the first char will be forced to capital for the table name
        boolean makeNextCap = initialCap;

        for (Character a : s.toCharArray()) {
            if (a == '_') {
                makeNextCap = true;
            }
            else {
                if (makeNextCap) {
                    String ls = "" + a;
                    sb.append(ls.toUpperCase());

                    makeNextCap=false;
                }
                else {
                    sb.append(a);
                }
            }
        }

        return sb.toString();
    }
}
