package com.sikayetvar.sitemap.generator.entity;

import com.sikayetvar.sitemap.generator.Utils;
import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by deniz on 3/14/17.
 */
public class ComplaintHashtag {

    private int complaint_id;
    private List<String> hashtags;
    private Date publish_time;
    private Date update_time;
    private String company;
    private static final Logger logger = LoggerFactory.getLogger(ComplaintHashtag.class);


    public ComplaintHashtag() {
        hashtags = new ArrayList<String>();

    }

    public String getCompany() {
        return company;
    }

    public String getCompanySlugged() {
        return Utils.getInstance().toSlug(company);
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public int getComplaint_id() {
        return complaint_id;
    }

    public void setComplaint_id(int complaint_id) {
        this.complaint_id = complaint_id;
    }

    public void setHashtags(List<String> hashtag_ids) {
        this.hashtags = hashtag_ids;
    }

    public List<String> getHashtags() {
        return hashtags;
    }


    public void insertHashtag(String tag){
        this.hashtags.add(tag);
    }

    public Date getPublish_time() {
        return publish_time;
    }

    public Date getUpdate_time() {
        return update_time;
    }

    public void setPublish_time(Date publish_time) {
        this.publish_time = publish_time;
    }

    public void setUpdate_time(Date update_time) {
        this.update_time = update_time;
    }


    public Set<SortedSet<String>> getCombinations(){
        Set<SortedSet<String>> allCombinations = new HashSet<>();

        // define a sorted set of hashtags for that complaint
        SortedSet<String> biggestSet = new TreeSet<String>();

        for (String hashtag:hashtags) {
            try {
                if(null != hashtag && hashtag != "" & hashtag != "null")
                    biggestSet.add(hashtag);
            } catch (Exception e) {
                logger.error("Error in biggest set generation. hashtag = /" + hashtag + "/",e);
            }
        }

        allCombinations.addAll(Utils.getInstance().powerSet(biggestSet));

        return allCombinations;
    }

    public boolean isMalformed(){
        if (complaint_id == 0 || hashtags.size() == 0 || null == publish_time  || publish_time.equals("0000-00-00 00:00:00") || null == company || company.equals("")){
            logger.warn("Malformed Complaint-Hashtag. id= " + complaint_id);
            return true;
        }
        else
            return false;
    }

}
