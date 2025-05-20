package com.sp.schedulerplatform.filters;

import com.sp.schedulerplatform.utils.JsonUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

@WebFilter("/api/uregister")
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        if (session == null) {
            JsonUtil.sendJsonError(resp, "unauthorized: login required", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof Map)) {
            JsonUtil.sendJsonError(resp, "unauthorized: user not found in session", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Map<String, Object> user = (Map<String, Object>) userObj;
        String role = (String) user.get("user_role");

        if (role == null || !role.equals("Admin")) {
            JsonUtil.sendJsonError(resp, "forbidden: admin access required", HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        chain.doFilter(request, response);
    }
}
