package com.sp.schedulerplatform.dao;

import com.sp.schedulerplatform.model.Invite;
import com.sp.schedulerplatform.model.Organization;
import com.sp.schedulerplatform.model.Role;
import com.sp.schedulerplatform.utils.DbPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InviteDAO {
    public boolean save(Invite invite) {
        String sql = " Insert into invites(ord_id,email, invite_token, uses) values (?,?,?,?)";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invite.getOrganization().getId());
            ps.setString(2, invite.getEmail());
            ps.setString(3, invite.getInviteToken());
            ps.setBoolean(4, invite.isUsed());

            return ps.executeUpdate() > 0;

        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public Invite findByToken(String token) {
        String sql = " Select i.id,i.email,i.invite_token,i.used,o.id as org.id,org.domain" +
                "from invites i join organization o on i.org =o.id where invite_token=?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Organization org = new Organization(rs.getInt("org_id"), rs.getString("domain"));
                return new Invite(rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("invite_token"),
                        rs.getBoolean("used"));

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;

    }


    public void markASUsed(String token) {
        String sql = " update invites set used=TRUE where invite_token=?";
        try (Connection conn = DbPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.executeUpdate();

        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
