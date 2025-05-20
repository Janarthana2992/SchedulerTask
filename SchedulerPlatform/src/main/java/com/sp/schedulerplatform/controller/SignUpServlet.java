package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import com.sp.schedulerplatform.utils.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.lang.model.type.NullType;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/signup")
public class SignUpServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseRequest(req);

        String email = data.get("email");
        String password = data.get("password");
        String hashpasswordInvite=PasswordUtil.hashPassword(password);

        if (email == null || password == null) {
            JsonUtil.sendJsonError(resp, "missing fields", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        System.out.println(PasswordUtil.isStrongPassword(password));
        if (!PasswordUtil.isStrongPassword(password)){
            JsonUtil.sendJsonError(resp,"need Strong password include 8 chars , caps , small sym, no",HttpServletResponse.SC_BAD_REQUEST);
            return;
        }



        try (Connection conn = DbPool.getConnection()) {
            String storedHash;
            boolean isVerified;
            String sql = "select id, name, password_hash, user_role, is_verified, organization_id,invite_token FRom users where email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email.toLowerCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        JsonUtil.sendJsonError(resp, "no user exist ", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    int userId = rs.getInt("id");
                    storedHash = rs.getString("password_hash");
                    isVerified = rs.getBoolean("is_verified");
                    if (isVerified == false) {
                        String updatePwdSql = "UPDATE users SET password_hash = ?, invite_token = NULL, is_verified = TRUE WHERE id = ?";
                        try (PreparedStatement stmt1 = conn.prepareStatement(updatePwdSql)) {
                            stmt1.setString(1, hashpasswordInvite);
                            stmt1.setInt(2, userId);
                            int updated = stmt1.executeUpdate();

                            if (updated != 1) {
                                JsonUtil.sendJsonError(resp, "failed to set password", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                return;
                            }
                        }


                        HttpSession session = req.getSession(true);
                        session.setAttribute("userId", rs.getInt("id"));
                        session.setAttribute("userName", rs.getString("name"));
                        session.setAttribute("userRole", rs.getString("user_role"));
                        session.setAttribute("orgId", rs.getInt("organization_id"));

                        JsonUtil.sendJsonResponse(resp, Map.of(
                                "message", "login successful",
                                "name", rs.getString("name"),
                                "role", rs.getString("user_role")
                        ));
                    }
                }
            }

            } catch (Exception e) {
                e.printStackTrace();
                JsonUtil.sendJsonError(resp, "server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
