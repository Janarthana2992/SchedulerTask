package com.sp.schedulerplatform.model;

public class User {
    private int id;
    private String email;
    private String passwordHash;
    private Organization organization;
    private Role role;
    private boolean isActive;

    public User() {
    }

    public User(int id, String email, String passwordHash, Organization organization, Role role, boolean isActive) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.organization = organization;
        this.role = role;
        this.isActive = isActive;
    }

    public int getId() {
        return id;
    }

    public User(String email, String passwordHash, Organization organization, Role role, boolean isActive) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.organization = organization;
        this.role = role;
        this.isActive = isActive;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
