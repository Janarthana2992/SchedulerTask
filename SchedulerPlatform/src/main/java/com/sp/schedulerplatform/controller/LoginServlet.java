package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import com.sp.schedulerplatform.utils.PasswordUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseRequest(req);

        String email = data.get("email");
        String password = data.get("password");


        if (email == null || password == null) {
            JsonUtil.sendJsonError(resp, "missing fields", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }





        try (Connection conn = DbPool.getConnection()) {
            String sql = "select id, name, password_hash, user_role, is_verified, organization_id FRom users where email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email.toLowerCase());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        JsonUtil.sendJsonError(resp, "no user exist ", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    boolean isVerified = rs.getBoolean("is_verified");

                    if (!PasswordUtil.checkPassword(password,storedHash)) {
                        JsonUtil.sendJsonError(resp, "invalid credentials", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!isVerified) {
                        JsonUtil.sendJsonError(resp, "user not verified", HttpServletResponse.SC_FORBIDDEN);
                        return;
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
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(resp, "server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
