package com.plexq.migration.app;

import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: plexq
 * Date: Aug 15, 2010
 * Time: 12:47:34 PM
 */
public class DumpToORASQLLoadable {
    private static Logger log = Logger.getLogger(DumpToORASQLLoadable.class);

    private static ApplicationContext context;

    private String dumpPath="data";

    public static void main(String[] args) throws SQLException, IOException {
        String x = System.getProperty("dumpPath");

        context = new FileSystemXmlApplicationContext("src/main/resources/META-INF/spring-dump-database.xml");
        DumpToORASQLLoadable dtc = new DumpToORASQLLoadable();
        if (x!=null && !x.equals("")) {
            dtc.setDumpPath(x);
        }

        DataSource ds = (DataSource)context.getBean("dataSource");
        if (ds instanceof JtdsDataSource) {
            JtdsDataSource jds = (JtdsDataSource)ds;
            log.info("Dumping data from "+jds.getDatabaseName()+" at server "+jds.getServerName());
        }

        dtc.run();
    }

    public DumpToORASQLLoadable() throws IOException {

    }

    public void run() throws SQLException, IOException {
        File f = new File(dumpPath);
        if (!f.exists() && !f.mkdirs()) {
            log.error("Failed to make dump path: "+dumpPath);
            return;
        }

        Connection connection = ((DataSource)context.getBean("dataSource")).getConnection();

        ArrayList<Table> tables = new ArrayList<Table>();

        System.out.println("Fetching Tables...");

        try {
            DatabaseMetaData dmd = connection.getMetaData();
            ResultSet rs = dmd.getTables(null,null,null,null);
            String schemaName;
            String tableName;
            while (rs.next()) {
                schemaName = rs.getString(2);
                tableName = rs.getString(3);
                if (System.getProperty("table")==null || System.getProperty("table").equals(tableName)) {
                    tables.add(new Table(schemaName,tableName));
                }
            }
        }
        catch (SQLException se) {
            log.error(se);
            log.error("Failed to dump schema - program aborting - badness here");
        }

        for (Table t : tables) {
            if (!t.schema.equals("INFORMATION_SCHEMA") && !t.schema.equals("sys")) {
                System.out.println("Dumping "+t);
                try {
                    List<Column> cl = new ArrayList<Column>();

                    Statement mtst = connection.createStatement();
                    DatabaseMetaData dmd = connection.getMetaData();
                    ResultSet mtrs = dmd.getColumns(null,"dbo",t.name,null);
                    while(mtrs.next()) {
                        Column c = new Column();
                        c.setName(mtrs.getString(4));
                        c.setType(mtrs.getInt(5));
                        cl.add(c);
                    }

                    Statement st = connection.createStatement();
                    StringBuilder sb = new StringBuilder();
                    for (Column c : cl) {
                        if (sb.length()!=0) {
                            sb.append(",");
                        }
                        if (c.getType()==Types.VARCHAR || c.getType()==Types.NVARCHAR || c.getType()==Types.LONGVARCHAR || c.getType()==Types.LONGNVARCHAR) {
                            sb.append("unicode("+c.getName()+")");
                        }
                        else {
                            sb.append(c.getName());
                        }
                    }
                    sb.append(" from ["+t.schema+"].["+t.name+"]");
                    ResultSet rs = st.executeQuery("select "+sb.toString());
                    ResultSetMetaData rsmd = rs.getMetaData();


                    FileWriter ctrlFile = new FileWriter("data/"+t.schema+"."+t.name+".ctl");
                    ctrlFile.write("load data\n" +
                            "infile "+t.schema+"."+t.name+".dump \"||\\n\"\n" +
                            "into table "+t+"\n" +
                            "fields terminated by '\t'\n");
                    ctrlFile.close();

                    FileWriter fw = new FileWriter("data/"+t.schema+"."+t.name+".dump");
                    while (rs.next()) {
                        for (int x=0;x<rsmd.getColumnCount();x++) {
                            if (x>0) {
                                fw.write("\t");
                            }
                            Object o = rs.getObject(x+1);
                            if (o!=null) {
                                if (cl.get(x).getType()==Types.CLOB) {
                                    Clob clob = (Clob)o;
                                    BufferedReader br = new BufferedReader(clob.getCharacterStream());
                                    String s;
                                    StringBuilder lsb = new StringBuilder();
                                    while ((s=br.readLine())!=null) {
                                        /*
                                        if (s.contains("\\")) {
                                            s = s.replaceAll("\\\\","\\\\\\\\");
                                        }
                                        */
                                        /*
                                        if (s.contains("\t") || s.contains("\"")) {
                                            s = "\""+s.replaceAll("\\\t","\\\\t").replaceAll("\\\"", "\\\"\\\"")+"\"";
                                        }
                                        */
                                        if (s.contains("\t")) {
                                            s = "\""+s.replaceAll("\\\t","\\\\t");
                                        }
                                        lsb.append(s);
                                        lsb.append("\t");
                                    }
                                    fw.write(lsb.toString());
                                }
                                else {
                                    String s = o.toString();
                                    if (s.contains("\n")) {
                                        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(s.getBytes())));
                                        sb = new StringBuilder();
                                        while ((s=br.readLine())!=null) {
                                            sb.append(s);
                                            sb.append("\n");
                                        }
                                        s = sb.toString();
                                    }
                                    if (s.contains("\t")) {
                                        s = "\""+s.replaceAll("\\\t","\\\\t");
                                    }
                                    fw.write(s);
                                }
                            }
                            else {
                                //fw.write("");
                            }
                        }

                        fw.write("\n");
                    }
                    fw.close();
                }
                catch (SQLException se) {
                    log.info(se);
                    log.warn("Failed to dump table " + t);
                }
            }
        }

        connection.close();
    }

    private class Table {
        public String schema;
        public String name;

        public Table(String schema, String name) {
            this.schema=schema;
            this.name=name;
        }

        public String toString() {
            return schema+"."+name;
        }
    }

    public String getDumpPath() {
        return dumpPath;
    }

    public void setDumpPath(String dumpPath) {
        this.dumpPath = dumpPath;
    }

    private class Column {
        private String name;
        private int type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }
    }
}
