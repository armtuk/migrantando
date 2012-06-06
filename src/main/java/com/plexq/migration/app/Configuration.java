package com.plexq.migration.app;

/**
 * Created with IntelliJ IDEA.
 * User: aturner
 * Date: 6/6/12
 * Time: 8:02 AM
 */
public class Configuration {
    private String dumpPath;
    private String packageName;

    public String getDumpPath() {
        return dumpPath;
    }

    public void setDumpPath(String dumpPath) {
        this.dumpPath = dumpPath;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
