package com.sp.schedulerplatform.dao;

import com.sp.schedulerplatform.model.Organization;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.*;
import java.util.Optional;

public class OrganizationDAO {

    public Organization findByDomain(String domain) {
        String sql = "SELECT id, domain FROM organization where domain = ?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Organization(rs.getInt("id"), rs.getString("domain"));
            }
        } catch (SQLException | InterruptedException e) {

            throw new RuntimeException(e);
        }
        return null;

    }

//    public Organization create(String domain) {
//        String sql = " insert into organization (domain) values(?) returning id ";
//        try (Connection conn = DbPool.getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setString(1, domain);
//            ResultSet rs = ps.executeQuery();
//            if (rs.next()) {
//                return new Organization(rs.getInt("id"), domain);
//            }
//        } catch (SQLException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        return null;
//
//
//    }
}
