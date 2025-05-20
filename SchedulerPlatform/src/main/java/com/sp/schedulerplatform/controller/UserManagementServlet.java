package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import com.sp.schedulerplatform.utils.TokenUtil;
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

@WebServlet("/api/usermanage")


public class UserManagementServlet extends HttpServlet {


    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws  IOException {

        Map<String, String> data = JsonUtil.parseRequest(req);

        String email = data.get("email");
        String role = data.get("role");
        String name= data.get("name");

        if (email == null || role == null || (!role.equals("Viewer") && !role.equals("Operator"))) {
            JsonUtil.sendJsonError(resp, "insufficient", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String loggedInUserRole = (String) req.getSession().getAttribute("userRole");
        System.out.println(loggedInUserRole);
        Integer loggedInOrgId = 2;

        if (loggedInUserRole == null || !loggedInUserRole.equals("Admin") ) {
            JsonUtil.sendJsonError(resp, "unauthrized only admin ", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = DbPool.getConnection()) {
            conn.setAutoCommit(false);


            String checkUserSql = "select id from users where email = ? and  organization_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkUserSql)) {
                stmt.setString(1, email.toLowerCase());
                stmt.setInt(2, loggedInOrgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        JsonUtil.sendJsonError(resp, " already exists", HttpServletResponse.SC_CONFLICT);
                        return;
                    }
                }
            }

            String inviteToken = TokenUtil.generateToken();


            String insertUserSql = "insert into users (email, name, password_hash, user_role, is_verified, invite_token, organization_id) " +
                    "values (?, ?, NULL, ?, false, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertUserSql)) {
                stmt.setString(1, email.toLowerCase());

                stmt.setString(2,name);
                stmt.setString(3, role);
                stmt.setString(4, inviteToken);
                stmt.setInt(5, loggedInOrgId);
                stmt.executeUpdate();
            }

            conn.commit();

            String inviteUrl = req.getRequestURL().toString().replace("/api/usermanage", "/api/invite/") + "?token=" + inviteToken;

            JsonUtil.sendJsonResponse(resp, Map.of("inviteUrl", inviteUrl));

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(resp, "server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String id = req.getParameter("id");
        if (id == null) {
            JsonUtil.sendJsonError(resp, " id required", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(id);
            System.out.println(userId);
        } catch (NumberFormatException e) {
            JsonUtil.sendJsonError(resp, "invalid user id", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String loggedInUserRole = (String) req.getSession().getAttribute("userRole");
        Integer loggedInOrgId = 2;
        System.out.println(loggedInUserRole);

        if (loggedInUserRole == null || !loggedInUserRole.equals("Admin") ) {
            JsonUtil.sendJsonError(resp, "unauthrized only admin ", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try (Connection conn = DbPool.getConnection()) {
            conn.setAutoCommit(false);


            String checkSql = "select id from users where id = ? and organization_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, loggedInOrgId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        JsonUtil.sendJsonError(resp, "no user found", HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }

            String deleteSql = "delete from users where id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, userId);
                int deleted = stmt.executeUpdate();
                if (deleted == 0) {
                    JsonUtil.sendJsonError(resp, "User not found", HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }

            conn.commit();

            JsonUtil.sendJsonResponse(resp, Map.of("message", "user deleted "));

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(resp, " server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
