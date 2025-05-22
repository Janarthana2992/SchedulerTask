package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.scheduler.JobScheduler;
import com.sp.schedulerplatform.utils.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

@WebServlet("/api/job/cancel")
public class CancelJobServlet extends HttpServlet {

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

        int userId = (Integer) userIdObj;

        Map<String, String> data = JsonUtil.parseRequest(req);
        String jobIdStr = data.get("jobId");

        if (jobIdStr == null || jobIdStr.trim().isEmpty()) {
            JsonUtil.sendJsonError(resp, "Missing jobId parameter", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int jobId = Integer.parseInt(jobIdStr.trim());

            boolean cancelled = JobScheduler.cancelJobBeforeExecution(jobId, userId);

            if (cancelled) {
                JsonUtil.sendSuccess(resp, "Job cancelled and marked as skipped.");
            } else {
                JsonUtil.sendJsonError(resp, "Job could not be cancelled (already running or completed)", HttpServletResponse.SC_CONFLICT);
            }
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonError(resp, "Invalid jobId format", HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            JsonUtil.sendJsonError(resp, "Internal server error: " + e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
