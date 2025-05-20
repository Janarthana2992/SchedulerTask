package com.sp.schedulerplatform.service;

import com.sp.schedulerplatform.utils.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class SchedulerThread extends Thread {
    private volatile boolean running = true;

    public void shutdown() {
        running = false;
    }


    public void run() {
        while (running) {
            try (Connection conn = DbPool.getConnection()) {

                String sql = "SELECT id, name, description, scheduled_time, execution_mode, retry_max, retry_delay_ms, concurrency_policy FROM jobs where scheduled_time<= now() " +
                        " and (status is null or status ='pending')  from job_executions";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        String executionMode = rs.getString("execution_mode");
                        String concurrencyPolicy = rs.getString("concurrency_policy");

                        boolean dispatch = dispatch(id, concurrencyPolicy, conn);
                        if (dispatch) {
                            dispatchJobs(id, name, conn);
                            markJobStatus(id, executionMode, conn);

                        }


                    }
                    conn.commit();
                }

            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

    }

    private boolean dispatch(int id, String policy, Connection conn) throws SQLException {
        switch (policy) {
            case "ALLOW":
                return true;
            case "REPLACE":
                terminateRunning(id, conn);
            case "FORBID":
                return !isAlreadyrunning(id, conn);
            default:
                return false;

        }


    }
private boolean isAlreadyrunning(int id ,Connection conn) throws SQLException{
        String sql="select count(*) from job_executions where job_id=? and status ='running'";
        try(PreparedStatement stmt =conn.prepareStatement(sql)){
            stmt.setInt(1,id);
            ResultSet rs=stmt.executeQuery();
            rs.next();
            return rs.getInt(1)>0;
        }
    }
    private void terminateRunning(int id, Connection conn ) throws SQLException{
        String sql="update job_executions set status='terminated' , ended_at=now() where job_id=? and status ='running'  ";
        try(PreparedStatement stmt =conn.prepareStatement(sql)){
            stmt.setInt(1,id);
            stmt.executeUpdate();


    }
}
private void dispatchJobs(int id , String name, Connection conn) throws SQLException{
        String sql =" insert into job_executions (job_id,status,started_at) values (?,'running',now())  ";
    try(PreparedStatement stmt =conn.prepareStatement(sql)){
        stmt.setInt(1,id);
        stmt.executeUpdate();

    }
    System.out.println("dispatched job"+name);
}

private void markJobStatus(int id, String mode , Connection conn) throws SQLException {
        if ("ONCE".equals(mode)) {
            String sql = "update job_executions set status='completed' where id=?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();

            }
        }
        else{
            String sql ="update jobs set scheduled_time= "+
                    "  case when execution_mode ='HOURLY' then scheduled_time+ INTERVAL '1 hour'" +
                    "  case when execution_mode ='DAILY' then scheduled_time+ INTERVAL '1 day'" +
                    "  case when execution_mode ='MONTHLY' then scheduled_time+ INTERVAL '1 month'" +
                    "  case when execution_mode ='WEEKLY' then scheduled_time+ INTERVAL '1 week'" +
                    "else schedule_time end " +
                    "where id=?" ;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();

            }

        }
}
}