package com.sp.schedulerplatform.controller;


import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet("/api/jobs")
public class JobManageServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            JsonUtil.sendJsonError(response, "unauthorized only admin and operator", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = (String) session.getAttribute("userRole");
        if (!role.equals("Admin") && !role.equals("Operator")) {
            JsonUtil.sendJsonError(response, "no access", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Map<String, String> data = JsonUtil.parseRequest(request);
        String name = data.get("name");
        String description = data.getOrDefault("description", "");
        String executionMode = data.get("executionMode");
        String concurrencyPolicy = data.get("concurrencyPolicy");
        String scheduledTime = data.get("scheduledTime");

        int maxRetries = Integer.parseInt(data.getOrDefault("maxRetries", "0"));
        long retryDelay = Long.parseLong(data.getOrDefault("retryDelay", "0"));

        try (Connection conn = DbPool.getConnection()) {
            String sql = "INSERT INTO jobs(name, description, scheduled_time, execution_mode, retry_max, retry_delay_ms, concurrency_policy, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setTimestamp(3, Timestamp.valueOf(scheduledTime));
                stmt.setString(4, executionMode);
                stmt.setInt(5, maxRetries);
                stmt.setLong(6, retryDelay);
                stmt.setString(7, concurrencyPolicy);
                stmt.setInt(8, (int) session.getAttribute("userId"));
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    JsonUtil.sendJsonResponse(response, Map.of("message", "job created", "id", rs.getInt("id")));
                } else {
                    JsonUtil.sendJsonError(response, "job creation failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> data = JsonUtil.parseRequest(request);

        String jobId= request.getParameter("id");
        System.out.println(jobId);
        if (jobId == null) {
            JsonUtil.sendJsonError(response, "missing job id", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String name = data.get("name");
        String description = data.getOrDefault("description", "");
        String executionMode = data.get("executionMode");
        String concurrencyPolicy = data.get("concurrencyPolicy");
        String scheduledTime = data.get("scheduledTime");

        int maxRetries = Integer.parseInt(data.getOrDefault("maxRetries", "0"));
        long retryDelay = Long.parseLong(data.getOrDefault("retryDelay", "0"));

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            JsonUtil.sendJsonError(response, "unauthorized only admin", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = (String) session.getAttribute("userRole");
        if (!role.equals("Admin") && !role.equals("Operator")) {
            JsonUtil.sendJsonError(response, "no access", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try (Connection conn = DbPool.getConnection()) {
            String sql = "UPDATE jobs SET name=?, description=?, scheduled_time=?, execution_mode=?, retry_max=?, retry_delay_ms=?, concurrency_policy=?, created_by=? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, description);
                stmt.setTimestamp(3, Timestamp.valueOf(scheduledTime));
                stmt.setString(4, executionMode);
                stmt.setInt(5, maxRetries);
                stmt.setLong(6, retryDelay);
                stmt.setString(7, concurrencyPolicy);
                stmt.setInt(8, (int) session.getAttribute("userId"));
                stmt.setInt(9, Integer.parseInt(jobId));

                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    JsonUtil.sendJsonResponse(response, Map.of("message", "updated job", "id", jobId));
                } else {
                    JsonUtil.sendJsonError(response, "job update failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            JsonUtil.sendJsonError(response, "unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = DbPool.getConnection()) {
            String sql = "SELECT id, name, description, scheduled_time, execution_mode, retry_max, retry_delay_ms, concurrency_policy, updated_at FROM jobs";

            try (PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> jobs = new ArrayList<>();
                while (rs.next()) {
                    jobs.add(Map.of(
                            "id", rs.getInt("id"),
                            "name", rs.getString("name"),
                            "description", rs.getString("description"),
                            "scheduledTime", rs.getTimestamp("scheduled_time").toString(),
                            "executionMode", rs.getString("execution_mode"),
                            "retryPolicy", Map.of(
                                    "maxRetries", rs.getInt("retry_max"),
                                    "retryDelay", rs.getLong("retry_delay_ms")
                            ),
                            "concurrencyPolicy", rs.getString("concurrency_policy"),
                            "updatedAt", rs.getTimestamp("updated_at").toString()
                    ));
                }
                JsonUtil.sendJsonResponse(response, Map.of("jobs", jobs));
            }
        } catch (SQLException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
