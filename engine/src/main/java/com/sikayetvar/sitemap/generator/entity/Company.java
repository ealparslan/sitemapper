package com.sikayetvar.sitemap.generator.entity;

public class Company {

    private int id;
    private String name;
    private int complaintCount;
    private boolean sikayetAlmis;

    public boolean isSikayetAlmis() {
        return sikayetAlmis;
    }

    public void setSikayetAlmis(boolean sikayetAlmis) {
        this.sikayetAlmis = sikayetAlmis;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getComplaintCount() {
        return complaintCount;
    }

    public void setComplaintCount(int complaintCount) {
        this.complaintCount = complaintCount;
    }

    public Company( int id,String name, int complaintCount, boolean sikayetAlmis) {
        this.name = name;
        this.complaintCount = complaintCount;
        this.id = id;
        this.sikayetAlmis = sikayetAlmis;
    }

}
