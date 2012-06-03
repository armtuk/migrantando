package com.plexq.migration.app;

import com.plexq.hermes.CreateDDL;
import com.plexq.hermes.PostgresqlTableBuilder;
import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: plexq
 * Date: Aug 15, 2010
 * Time: 12:47:34 PM
 */
public class GenerateDDL {
    private static Logger log = Logger.getLogger(GenerateDDL.class);

    private static ApplicationContext context;

    private String dumpPath="ddl/";

    public static void main(String[] args) throws Exception {
        String x = System.getProperty("dumpPath");

        context = new FileSystemXmlApplicationContext("src/main/resources/META-INF/spring-dump-database.xml");
        GenerateDDL dtc = new GenerateDDL();
        if (x!=null && !x.equals("")) {
            dtc.setDumpPath(x);
        }

        DataSource ds = (DataSource)context.getBean("dataSource");
        if (ds instanceof JtdsDataSource) {
            JtdsDataSource jds = (JtdsDataSource)ds;
            log.info("Creating DDL from "+jds.getDatabaseName()+" at server "+jds.getServerName());
        }

        dtc.run();
    }

    public GenerateDDL() {

    }

    public void run() throws Exception {
        File dp = new File(dumpPath);
        if (!dp.exists() && !dp.mkdirs()) {
            log.error("Failed to make dump path: "+dumpPath);
            return;
        }

        File f = new File(dp.getPath()+"/"+"ddl.sql");

        Connection connection = (context.getBean("dataSource",DataSource.class)).getConnection();

        CreateDDL cddl = new CreateDDL();

        String ddl = cddl.create(connection,"dbo", new PostgresqlTableBuilder());

        FileWriter fw = new FileWriter(f);
        fw.write(ddl);
        fw.close();
    }

    public String getDumpPath() {
        return dumpPath;
    }

    public void setDumpPath(String dumpPath) {
        this.dumpPath = dumpPath;
    }
}
