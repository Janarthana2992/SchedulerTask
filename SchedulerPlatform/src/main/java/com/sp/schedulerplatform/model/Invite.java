package com.sp.schedulerplatform.model;

public class Invite {
    private int id ;
    private Organization organization;
    private String email;
    private String inviteToken;
    private boolean used;


    public Invite() {
    }

    public Invite(int id, Organization organization, String email, String inviteToken, boolean used) {
        this.id = id;
        this.organization = organization;
        this.email = email;
        this.inviteToken = inviteToken;        this.used = used;
    }

    public Invite(int id, String email, String inviteToken, boolean used) {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInviteToken() {
        return inviteToken;
    }

    public void setInviteToken(String inviteToken) {
        this.inviteToken = inviteToken;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
