package com.sikayetvar.sitemap.generator.dao;

import com.sikayetvar.sitemap.generator.Configuration;
import com.sikayetvar.sitemap.generator.entity.Company;
import com.sikayetvar.sitemap.generator.entity.Complaint;
import com.sikayetvar.sitemap.generator.entity.ComplaintHashtag;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;

/**
 * Created by deniz on 3/14/17.
 */
public class MySQLDataOperator {

    private static MySQLDataOperator instance = null;
    private Connection connect = null;
    private PreparedStatement statement = null;
    private ResultSet resultSet = null;
    public HashMap<Integer,String> corpus = null;
    public HashMap<Integer,Company> companies = null;
    public HashMap<Integer,Complaint> complaints = null;
    private static final Logger logger = LoggerFactory.getLogger(MySQLDataOperator.class);
    private int focused_complaint;
    private ComplaintHashtag  complaintHashtag = new ComplaintHashtag();
    private List<ComplaintHashtag> complaint_hashtags_list = new ArrayList<>();


    protected MySQLDataOperator() {
    }

    public static MySQLDataOperator getInstance() {
        if (instance == null) {
            instance = new MySQLDataOperator();
            instance.connect();
        }
        return instance;
    }

    public void connect(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(Configuration.DATABASE_CONNECTION_URL+ "&user=" + Configuration.DATABASE_USERNAME + "&password=" + Configuration.DATABASE_PASSWORD);
        }
        catch (Exception e ){
            logger.error("Could not connect MySQL server",e);
        }

    }

    public void initDatasources(){
        try {
            instance.getCorpus();
            logger.info("Corpus Loaded!");
            instance.getCompanies();
            logger.info("Companies Loaded!");
            instance.getComplaints();
            logger.info("Complaints Loaded!");
        } catch (Exception e) {
            logger.error("Datasource initiailization error! ",e);
        }
    }

    public HashMap<Integer,String> getCorpus(){
        HashMap<Integer,String> corpus = new HashMap<Integer, String>();

        try {
            statement = connect.prepareStatement(Configuration.SQL_GET_CORPUS);
            resultSet = statement.executeQuery();
            while (resultSet.next()){
                corpus.put(resultSet.getInt(1),resultSet.getString(2));
            }
        }
        catch (Exception e){
            logger.error("Exception in MySQLDAO.getCorpus",e);
        }
        this.corpus = corpus;
        return corpus;
    }

    public HashMap<Integer,Complaint> getComplaints(){
        HashMap<Integer,Complaint> complaints = new HashMap<Integer, Complaint>();
        int complaintId = 0;

        try {
            statement = connect.prepareStatement(Configuration.SQL_GET_COMPLAINTS);
            resultSet = statement.executeQuery();
            while (resultSet.next()){
                complaintId = resultSet.getInt(1);
                try {
                    complaints.put(complaintId,new Complaint(complaintId,resultSet.getTimestamp(2),resultSet.getTimestamp(3),resultSet.getInt(4),
                                                                resultSet.getString(5),resultSet.getInt(6),resultSet.getInt(7)));
                } catch (SQLException e) {
                    logger.error("Exception in MySQLDAO.getComplaints! The most possible reason is Update_Time = 0000-00-00 00:00:00  / ComplaintId: " + complaintId,e);
                }
            }
        }
        catch (Exception e){
            logger.error("Exception in MySQLDAO.getComplaints! ComplaintId: " + complaintId,e);
        }
        this.complaints = complaints;
        return complaints;
    }

    public HashMap<Integer,Company> getCompanies(){
        HashMap<Integer,Company> companies = new HashMap<Integer, Company>();

        try {
            statement = connect.prepareStatement(Configuration.SQL_GET_COMPANIES);
            resultSet = statement.executeQuery();
            while (resultSet.next()){
                companies.put(resultSet.getInt(1),new Company(resultSet.getInt(1),resultSet.getString(2),resultSet.getInt(3)));
            }
        }
        catch (Exception e){
            logger.error("Exception in MySQLDAO.getCompanies",e);
        }
        this.companies = companies;
        return companies;
    }



    public List<ComplaintHashtag> getComplaintHashtags(){

        try {

            complaint_hashtags_list = new ArrayList<>();
            logger.info("Getting hashtag complaints!");
            if(Configuration.DEBUG)
                statement = connect.prepareStatement(Configuration.SQL_GET_COMPLAINTHASHTAGS + " limit " + Configuration.DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE);
            else
                statement = connect.prepareStatement(Configuration.SQL_GET_COMPLAINTHASHTAGS );

            resultSet = statement.executeQuery();

            focused_complaint = 0;

            while (resultSet.next()){
                int on_complaint = resultSet.getInt(1);
                if(!complaints.containsKey(on_complaint)) continue;
                int hashtag = resultSet.getInt(2);
                processComplaintHashtags(on_complaint,hashtag);
            }
            logger.info("Done!");
        }
        catch (Exception e){
            logger.error("Exception in MySQLDAO.getComplaintHashtags",e);
        }
        return complaint_hashtags_list;
    }

    public void processComplaintHashtags(int on_complaint, int hashtag){

        if (!complaints.containsKey(on_complaint)){
            logger.warn("Hashtag-Complaint is not processed due to the lack of complaint Id: " +on_complaint + "in our dataset!");
            return; // if we don't recognize the complaint id from our dataset we don't process it
        }
        if( on_complaint!=focused_complaint ){
            if(complaintHashtag.getComplaint_id() != 0)
                complaint_hashtags_list.add(complaintHashtag);
            complaintHashtag = new ComplaintHashtag();
            complaintHashtag.setComplaint_id(on_complaint);
            try {
                complaintHashtag.setCompany(companies.get(complaints.get(on_complaint).getComplaint_company_id()).getName());
            } catch (Exception e) {
                logger.error("Exception in MySQLDAO.getComplaintHashtags:  Complaint or Company not found. on_complaint " + on_complaint ,e);
            }
            try {
                complaintHashtag.setPublish_time(complaints.get(on_complaint).getSikayet_time());
                complaintHashtag.setUpdate_time(complaints.get(on_complaint).getUpdate_time());
            } catch (Exception e) {
                logger.error("Exception in MySQLDAO.getComplaintHashtags:  Complaint or Update Time not found. on_complaint " + on_complaint,e);
            }
        }
        complaintHashtag.insertHashtag(corpus.get(hashtag));
        focused_complaint = on_complaint;
    }

    public List<ComplaintHashtag> getComplaint_hashtags_list() {
        return complaint_hashtags_list;
    }

}
