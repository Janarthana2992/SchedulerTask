package com.sp.schedulerplatform.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class DbPool {
    private static final BlockingDeque<Connection> pool = new LinkedBlockingDeque<>();
    private static final int POOL_SIZE = 30;

    static {
        try {
            String url = "jdbc:postgresql://localhost:5432/schedule";
            String user = "postgres";
            String pass = "postgres";

            for (int i = 0; i < POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(url, user, pass);
                pool.add(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws InterruptedException {
        Connection conn = pool.take();
        try {
            if (conn == null || conn.isClosed()) {
                System.out.println("DB - (null or closed)");
                String url = "jdbc:postgresql://localhost:5432/schedule";
                String user = "postgres";
                String pass = "postgres";
                conn = DriverManager.getConnection(url, user, pass);
            }
        } catch (SQLException e) {
            System.out.println("DB - " + e);
        }
        return conn;
    }

    public static void release(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    pool.put(conn);
                } else {

                    String url = "jdbc:postgresql://localhost:5432/schedule";
                    String user = "postgres";
                    String pass = "postgres";
                    Connection newConn = DriverManager.getConnection(url, user, pass);
                    pool.put(newConn);
                }
            } catch (SQLException | InterruptedException e) {
                throw new RuntimeException("Failed to release DB connection", e);
            }
        }
    }


    public static void shutdown() {
        for (Connection conn : pool) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
