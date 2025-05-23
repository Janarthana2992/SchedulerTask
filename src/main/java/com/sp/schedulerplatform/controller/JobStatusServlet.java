package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet("/api/job/status")
public class JobStatusServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        List<Map<String, Object>> jobStatuses = new ArrayList<>();

        String sql = """
                select j.id as job_id,j.name,
                       coalesce(e.status, 'not_Started') as latest_status,
                       e.started_at,
                       e.ended_at
                from jobs j
                left join lateral (
                    select status, started_at, ended_at
                    from job_executions
                    where job_id = j.id
                    order by id desc
                    limit 1
                ) e on true
                order by j.id;
                
                """;

        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> job = new LinkedHashMap<>();
                job.put("jobId", rs.getInt("job_id"));
                job.put("name", rs.getString("name"));
                job.put("latestStatus", rs.getString("latest_status"));
                job.put("startedAt", rs.getTimestamp("started_at"));
                job.put("endedAt", rs.getTimestamp("ended_at"));
                jobStatuses.add(job);
            }

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.write("{\"error\": \"Failed to fetch job statuses.\"}");
            }
            e.printStackTrace();
            return;
        }

        try (PrintWriter out = resp.getWriter()) {
            out.write(toJson(jobStatuses));
        }
    }

    private String toJson(List<Map<String, Object>> list) {
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            json.append("{");
            int j = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                json.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value == null) {
                    json.append("null");
                } else if (value instanceof Number) {
                    json.append(value);
                } else {
                    json.append("\"").append(value.toString()).append("\"");
                }
                if (++j < map.size()) json.append(",");
            }
            json.append("}");
            if (i < list.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }
}
