package com.plexq.hermes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: aturner
 * Date: 6/6/12
 * Time: 11:13 AM
 */
public class ScalaTypeNameFactory extends TypeNameFactory {
    public String getTypeName(Class c) {
        if (c.equals(Integer.class)) {
            return "Int";
        }
        else if (c.equals(Date.class) || c.equals(java.sql.Date.class)) {
            return "java.util.Date";
        }
        else if (c.equals(Timestamp.class)) {
            return "java.util.Date";
        }
        else if (c.equals(BigDecimal.class)) {
            return "java.math.BigDecimal";
        }
        else {
            return c.getSimpleName();
        }
    }
}
