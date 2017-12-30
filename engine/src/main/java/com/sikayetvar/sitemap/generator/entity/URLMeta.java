package com.sikayetvar.sitemap.generator.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by deniz on 3/18/17.
 */
public class URLMeta implements Comparable<URLMeta> {



    private Date mostUpToDate;
    private int count;
    private int priority;
    private Set<String> content;
    private int cardinality;


    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }


    public Set<String> getContent() {
        return content;
    }

    public void setContent(Set<String> content) {
        this.content = content;
    }

    public int getCardinality() {
        return cardinality;
    }

    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    public Date getMostUpToDate() {
        return mostUpToDate;
    }
    public void setMostUpToDate(Date mostUpToDate) {
        this.mostUpToDate = mostUpToDate;
    }

    public int getCount() {
        return count;
    }

    public URLMeta(Date mostUpToDate, int count, Set<String> content, int cardinality, int priority) {
        this.mostUpToDate = mostUpToDate;
        this.count = count;
        this.content = content;
        this.cardinality = cardinality;
        this.priority = priority;
    }

    public URLMeta(Date mostUpToDate, int count, String uniqueContent, int cardinality, int priority) {
        Set<String> content = new HashSet<>();
        content.add(uniqueContent);
        this.mostUpToDate = mostUpToDate;
        this.count = count;
        this.content = content;
        this.cardinality = cardinality;
        this.priority = priority;
    }


    @Override
    public int compareTo(URLMeta other) {
        int comparison = other.getMostUpToDate().compareTo(this.mostUpToDate);
        if(comparison == 0){
            Iterator iterOther = other.getContent().iterator();
            Iterator iterThis = this.getContent().iterator();
            comparison = ((String)iterOther.next()).compareTo((String)iterThis.next());
        }
        return comparison;
    }
}
