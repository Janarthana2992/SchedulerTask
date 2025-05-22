package com.sp.schedulerplatform.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String,String> parseRequest(HttpServletRequest request) throws IOException {
        return mapper.readValue(request.getInputStream(), Map.class);
    }



    public static void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), data);
    }

    public static void sendJsonError(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        sendJsonResponse(response, Map.of("error", message));
    }


    public static void sendSuccess(HttpServletResponse response, Object data) throws IOException {
        sendJsonResponse(response, Map.of("success", true, "data", data));
    }
}
