package com.sikayetvar.sitemap.generator.entity;

import java.util.Date;

/**
 * Created by deniz on 3/17/17.
 */
public class Complaint {

    private int complaint_id;
    private Date update_time;
    private int complaint_company_id;
    private String url;

    public Complaint(int complaint_id, Date update_time, int complaint_company_id, String url) {
        this.complaint_id = complaint_id;
        this.update_time = update_time;
        this.complaint_company_id = complaint_company_id;
        this.url = url;
    }

    public int getComplaint_id() {
        return complaint_id;
    }

    public void setComplaint_id(int complaint_id) {
        this.complaint_id = complaint_id;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }

    public int getComplaint_company_id() {
        return complaint_company_id;
    }

    public void setComplaint_company_id(int complaint_company_id) {
        this.complaint_company_id = complaint_company_id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }



}