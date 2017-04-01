package com.sikayetvar.sitemap.generator.entity;

import java.util.Date;

/**
 * Created by deniz on 3/18/17.
 */
public class URLMeta {

    private Date mostUpToDate;
    private int count;

    public Date getMostUpToDate() {
        return mostUpToDate;
    }

    public int getCount() {
        return count;
    }

    public URLMeta(Date mostUpToDate, int count) {
        this.mostUpToDate = mostUpToDate;
        this.count = count;
    }

}
