package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;

import com.sp.schedulerplatform.utils.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@WebServlet("/api/invite/*")
public class InviteServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String inviteToken = req.getParameter("token");

        Map<String, String> data = JsonUtil.parseRequest(req);
        String password = data.get("password");
        String confirmPassword = data.get("confirmpassword");


        if (inviteToken == null || password == null || confirmPassword == null) {
            JsonUtil.sendJsonError(resp, "insufficient data", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (password.equals(confirmPassword)) {
            JsonUtil.sendJsonError(resp, "passwords do not match", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!PasswordUtil.isStrongPassword(password)){
            JsonUtil.sendJsonError(resp,"need Strong password include 8 chars , caps , small sym, no",HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        try (Connection conn = DbPool.getConnection()) {
            conn.setAutoCommit(false);

            int userId = -1;
            String selectUserSql = "SELECT id FROM users WHERE invite_token = ? AND password_hash IS NULL";

            try (PreparedStatement stmt = conn.prepareStatement(selectUserSql)) {
                stmt.setString(1, inviteToken);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("id");
                    } else {
                        JsonUtil.sendJsonError(resp, "Invalid or expired invite token", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
            }

            String hashedPwd = PasswordUtil.hashPassword(password);

            String updatePwdSql = "UPDATE users SET password_hash = ?, invite_token = NULL, is_verified = TRUE WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updatePwdSql)) {
                stmt.setString(1, hashedPwd);
                stmt.setInt(2, userId);
                int updated = stmt.executeUpdate();

                if (updated != 1) {
                    JsonUtil.sendJsonError(resp, "failed to set password", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }
            }

            conn.commit();
            JsonUtil.sendJsonResponse(resp, Map.of("message", "Password set successfully. You can now log in."));

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(resp, "server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
