package com.sp.schedulerplatform.utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.ScatteringByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class DbPool {
    private static BlockingDeque<Connection> pool=new LinkedBlockingDeque<Connection>();

    static
    {
        try {

            String url = "jdbc:postgresql://localhost:5432/schedule";
            System.out.println(url);
            String user = "postgres";
            String pass = "postgres";

            for (int i = 0; i < 20; i++) {
                Connection conn = DriverManager.getConnection(url, user, pass);
                pool.add(conn);
            }
        }
        catch (SQLException e){
            e.printStackTrace();
        }


    }
 public static Connection getConnection( ) throws  InterruptedException {
        Connection conn = pool.take();
        try {
            if (conn == null || conn.isClosed()) {
                System.out.println("DB - (null or closed)");

            }
        }
            catch(SQLException e){
                System.out.println("DB - " + e);
            }
        return conn;


 }
 public static void close(Connection conn) {
        int closeCount = 0;
        for (int i = 0; i < 20; i++) {
            try{
                Connection conns =pool.poll();
                if (conns != null && conns.isClosed()) {
                    conns.close();
                    closeCount++;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
 }

}
