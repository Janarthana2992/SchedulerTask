package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.scheduler.JobScheduler;
import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
@WebServlet("/api/job/trigger")
public class TriggerJobServlet extends HttpServlet {

    private final JobScheduler jobScheduler = new JobScheduler();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            JsonUtil.sendJsonError(resp, "Session not found", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String role = (String) session.getAttribute("userRole");
        Object userIdObj = session.getAttribute("userId");

        if (role == null || userIdObj == null || !(userIdObj instanceof Integer)) {
            JsonUtil.sendJsonError(resp, "Unauthorized: Invalid session attributes", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!"Admin".equalsIgnoreCase(role)) {
            JsonUtil.sendJsonError(resp, "Access denied: Only Admin can perform this action", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Map<String, String> data = JsonUtil.parseRequest(req);
        String jobIdStr = data.get("jobId");

        if (jobIdStr == null) {
            JsonUtil.sendJsonError(resp, "missing jobId parameter", 400);
            return;
        }

        try {
            int jobId = Integer.parseInt(jobIdStr);
            boolean triggered = jobScheduler.triggerJob(jobId);

            if (triggered) {
                JsonUtil.sendSuccess(resp, "Job triggered successfully");
            } else {
                JsonUtil.sendJsonError(resp, "Job could not be triggered (check concurrency or job existence)", 409);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonError(resp, "Invalid jobId parameter", 400);
        }
    }
}
