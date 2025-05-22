package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/executions")
public class AuditViewServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        List<Map<String, Object>> executions = new ArrayList<>();

        String sql = "SELECT e.id, e.job_id, j.name AS job_name, e.status, e.started_at, " +
                "e.ended_at, e.error_message, e.job_duration " +
                "FROM job_executions e " +
                "JOIN jobs j ON e.job_id = j.id " +
                "ORDER BY e.started_at DESC " +
                "LIMIT 100";

        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                Timestamp startedAt = rs.getTimestamp("started_at");
                Timestamp endedAt = rs.getTimestamp("ended_at");
                String errorMessage = rs.getString("error_message");

                row.put("id", rs.getLong("id"));
                row.put("jobId", rs.getInt("job_id"));
                row.put("jobName", rs.getString("job_name"));
                row.put("status", rs.getString("status"));
                row.put("startedAt", startedAt);
                row.put("endedAt", endedAt);
                row.put("jobDuration", rs.getString("job_duration"));

                row.put("errorMessage", errorMessage);
                executions.add(row);
            }

            JsonUtil.sendSuccess(response, executions);

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(response, "Failed to retrieve job execution logs", 500);
        }
    }
}
