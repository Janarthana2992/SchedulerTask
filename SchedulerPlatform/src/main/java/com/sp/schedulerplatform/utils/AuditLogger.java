package com.sp.schedulerplatform.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class AuditLogger {

    public static void logJobFieldChange(int jobId, int userId, String fieldName, String oldValue, String newValue) {
        if (oldValue == null && newValue == null) return;
        if (oldValue != null && oldValue.equals(newValue)) return;

        String sql = "insert into job_audit_log (job_id, modified_by, field_name, old_value, new_value, modified_at) " +
                "values (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, jobId);
            stmt.setInt(2, userId);
            stmt.setString(3, fieldName);
            stmt.setString(4, oldValue);
            stmt.setString(5, newValue);
            stmt.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to log audit for job " + jobId + " field: " + fieldName);
            e.printStackTrace();
        }
    }
}
