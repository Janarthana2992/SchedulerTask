package com.sp.schedulerplatform.dao;

import com.sp.schedulerplatform.model.Organization;
import com.sp.schedulerplatform.model.Role;
import com.sp.schedulerplatform.model.User;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {
    public User findByEmail(String email) throws SQLException, InterruptedException {
        String sql = "select u.id,u.email,u.password_hash,u.is_active," +

                "r.id as role_id,r.name as role_name, " +
                " o.id as org_id , o.domain  " +
                "From users u" +
                "Join roles r on u.role_id=r.id" +
                "join Organization o on u.org_id=o.id" +
                "where u.email=?";


        // role -id, name
        //user - id, email, password_hash, is_active
        //organization -  id, domain
        //join user and roles
        //ID


        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Role role = new Role(rs.getInt("role_id"), rs.getString("role_name"));
                Organization org = new Organization(rs.getInt("id"), rs.getString("domain"));
                return new User(rs.getInt("id"), rs.getString("email"), rs.getString("password_hash"), org, role, rs.getBoolean("is_active"));

            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean save(User user) {

        String sql = "Insert into users (email, password_hash,org_id,role_id,is_active) values (?,?,?,?,?)";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setInt(3, user.getOrganization().getId());
            ps.setInt(4, user.getRole().getId());
            ps.setBoolean(5, user.isActive());
            return ps.executeUpdate() > 0;


        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }


    }


    public void activateUser(String email){
        String sql =" Update users set is_active = TRUE where email=?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,email);
            ps.executeUpdate();
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
