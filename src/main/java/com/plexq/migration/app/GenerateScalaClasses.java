package com.plexq.migration.app;

import com.plexq.hermes.*;
import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: aturner
 * Date: 3/28/11
 * Time: 5:09 PM
 */
public class GenerateScalaClasses {
    private static Logger log = LoggerFactory.getLogger(GenerateDDL.class);

    private static ApplicationContext context;

    private static String dumpPath="data/scala";

    private static Configuration config;

    private String[] names;

    public static void main(String[] args) throws Exception {
        String x = System.getProperty("dumpPath");

        context = new FileSystemXmlApplicationContext("src/main/resources/META-INF/spring-dump-database.xml");
        config = context.getBean("configuration", Configuration.class);
        dumpPath = config.getDumpPath();
        GenerateScalaClasses dtc = new GenerateScalaClasses(args);
        if (x!=null && !x.equals("")) {
            dumpPath=x;
        }

        DataSource ds = (DataSource)context.getBean("dataSource");
        if (ds instanceof JtdsDataSource) {
            JtdsDataSource jds = (JtdsDataSource)ds;
            log.info("Creating DDL from "+jds.getDatabaseName()+" at server "+jds.getServerName());
        }

        dtc.run();
    }

    public GenerateScalaClasses(String[] names) {
        this.names = names;
    }

    public void run() throws Exception {
        File dp = new File(dumpPath);
        if (!dp.exists() && !dp.mkdirs()) {
            log.error("Failed to make dump path: "+dumpPath);
            return;
        }

        Connection connection = ((DataSource)context.getBean("dataSource")).getConnection();

        ScalaClassTableBuilder jctb = new ScalaClassTableBuilder();
        jctb.setPackageName(config.getPackageName());
        jctb.setDumpPath(dumpPath);

        CreateDDL cddl = new CreateDDL(names);

        // pretty sure at this point, htis is just a side-effect.
        String ddl = cddl.create(connection,"public", jctb);

    }

    public static String getDumpPath() {
        return dumpPath;
    }
}
