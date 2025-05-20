package com.sp.schedulerplatform.model;

public class Organization {
    private int id;
    private String domain;

    public Organization (){

    }

    public Organization(int id, String domain) {
        this.id = id;
        this.domain = domain;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
