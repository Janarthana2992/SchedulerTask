package com.sp.schedulerplatform.dao;
import com.sp.schedulerplatform.model.Role;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RoleDAO {
    public Role findByName(String name){
        String sql =" SELECT id,name from roles WHERE name=?";
        try(Connection conn= DbPool.getConnection();
            PreparedStatement ps= conn.prepareStatement(sql)){
            ps.setString(1,name);
            ResultSet rs=ps.executeQuery();
            if (rs.next()){
                return new Role (rs.getInt("id"),rs.getString("name"));
            }
        } catch (SQLException|InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;


    }
    public Role findById (int id ){
        String sql =" SELECT id,name from roles WHERE id=?";
        try(Connection conn= DbPool.getConnection();
            PreparedStatement ps= conn.prepareStatement(sql)){
            ps.setInt(1,id);
            ResultSet rs=ps.executeQuery();
            if (rs.next()){
                return new Role (rs.getInt("id"),rs.getString("name"));
            }
        } catch (SQLException|InterruptedException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}
