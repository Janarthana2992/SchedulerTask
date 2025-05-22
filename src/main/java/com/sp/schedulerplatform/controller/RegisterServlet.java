package com.sp.schedulerplatform.controller;

import com.sp.schedulerplatform.utils.PasswordUtil;

import com.sp.schedulerplatform.utils.DbPool;
import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sp.schedulerplatform.utils.PasswordUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> data = JsonUtil.parseRequest(req);

        String email = data.get("email");
        String username = data.get("username");
        String password = data.get("password");
        String confirmPassword = data.get("confirmPassword");
        String hashPassAdmin= PasswordUtil.hashPassword(password);




        if (email == null || username == null || password == null || confirmPassword == null) {
            JsonUtil.sendJsonError(resp, "insufficient", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JsonUtil.sendJsonError(resp, "passwords do not match", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        System.out.println(PasswordUtil.isStrongPassword(password));
        if (!PasswordUtil.isStrongPassword(password)){
            JsonUtil.sendJsonError(resp,"need Strong password include 8 chars , caps , small sym, no",HttpServletResponse.SC_BAD_REQUEST);
            return;
        }


        String domain = email.replaceAll(".*@(.+)", "$1");

        try (Connection conn = DbPool.getConnection()) {

            String checkOrgSql = "select domain from organizations LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(checkOrgSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String existingDomain= rs.getString("domain");
                    if (!existingDomain.equalsIgnoreCase(domain)) {
                        JsonUtil.sendJsonError(resp, "organization  iwth diff domain already exist", HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    else{
                    JsonUtil.sendJsonError(resp, "organization already exist", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
                }


            int orgId =0;
            String orgInsertSql = "insert into  organizations (domain, name) values (?, ?) RETURNING id";
            try (PreparedStatement stmt = conn.prepareStatement(orgInsertSql)) {
                stmt.setString(1, domain);
                stmt.setString(2, domain.split("\\.")[0]);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        orgId = rs.getInt("id");
                    }
                }
            }
            String userInsertSql = "insert into  users (email, name, password_hash, user_role, is_verified, organization_id) " +
                    "values (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(userInsertSql)) {
                stmt.setString(1, email.toLowerCase());
                stmt.setString(2, username);
                stmt.setString(3, hashPassAdmin);
                stmt.setString(4, "Admin");
                stmt.setBoolean(5, true);
                stmt.setInt(6, orgId);
                stmt.executeUpdate();
            }


            JsonUtil.sendJsonResponse(resp, Map.of("message", "admin registered success.  login now."));

        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonError(resp, "server error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}