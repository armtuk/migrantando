package com.plexq.hermes;

/**
 * Created with IntelliJ IDEA.
 * User: aturner
 * Date: 6/6/12
 * Time: 11:12 AM
 */
public class JavaTypeNameFactory extends TypeNameFactory {
    public String getTypeName(Class c) {
        return c.getSimpleName();
    }
}
