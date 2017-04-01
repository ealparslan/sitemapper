package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import com.sikayetvar.sitemap.generator.entity.Complaint;
import com.sikayetvar.sitemap.generator.entity.ComplaintHashtag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.Lists;
import java.io.*;
import java.util.stream.Collectors;

import com.sikayetvar.sitemap.generator.entity.URLMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecyrd.speed4j.StopWatch;


/**
 * Created by deniz on 3/14/17.
 */
public class Controller {

    public List<ComplaintHashtag> complaint_hashtags_list = new ArrayList<ComplaintHashtag>();

    private ConcurrentHashMap<String,URLMeta> companyHashtagURLs = new ConcurrentHashMap<String,URLMeta>();
    private ConcurrentHashMap<String,Date> companyURLs = new ConcurrentHashMap<String,Date>();

    public ConcurrentHashMap<String, URLMeta> getCompanyHashtagURLs() {return companyHashtagURLs;}

    private HashMap<String,Date> complaintURLs = new HashMap<String,Date>();
    private int index;
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);


    public static void main(String[] args) {

        StopWatch sw = new StopWatch();
        MemoryWatch mw = new MemoryWatch();
        Controller controller = new Controller();

        logger.info(Configuration.dumpCurrentConfiguration());

        sw.start();

        MySQLDataOperator.getInstance().initDatasources();

        controller.writeComplaintURLs();

        int chunkIndex=0;
        do{
            controller.complaint_hashtags_list = MySQLDataOperator.getInstance().getComplaintHashtags(Configuration.COMPLAINT_HASHTAGS_CHUNK_SIZE, Configuration.COMPLAINT_HASHTAGS_CHUNK_SIZE * chunkIndex);
            logger.info(controller.complaint_hashtags_list.size() + " Complaints Loaded!");
            controller.processChunk();
            logger.info("Processor returned! ");
            chunkIndex++;
        }while (controller.complaint_hashtags_list.size()!=0);

        logger.info("companyHashtagURLs Calculated!");

        controller.writeCompanyHashtagURLs();
        controller.writeCompanyURLs();
        logger.info(sw.toString());
        Utils.getInstance().disposeInstance();
    }

    public void processChunk(){
        logger.info(complaint_hashtags_list.size() + " will be processed in this chunk! ");
        int size = complaint_hashtags_list.size() / Configuration.NUMBER_OF_THREADS + 1;
        List<List<ComplaintHashtag>> partitions = Lists.partition(complaint_hashtags_list, size);

        ExecutorService executor = Executors.newFixedThreadPool(Configuration.NUMBER_OF_THREADS);

        for (List<ComplaintHashtag> partition : partitions) {
            executor.execute(() -> executor(partition));
        }
        executor.shutdown();
        try {
            while (!executor.awaitTermination(24L, TimeUnit.HOURS)) {
                logger.info("Not yet. Still waiting for termination");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void executor(List<ComplaintHashtag> complaint_hashtags_list){
        int i =0;
        System.out.println("I'm starting!");
        for (ComplaintHashtag complaint:complaint_hashtags_list) {

            Date publish_time = complaint.getPublish_time();
            if (complaint.isMalformed()) continue;
            Set<Set<String>> allCombinations = complaint.getCombinations();
            for (Set<String> combination:allCombinations) {
                if(combination.isEmpty()) continue;
                // brand + hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(complaint.getCompany() + "/" + combination.stream().collect(Collectors.joining("/")) , publish_time);
                // hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(combination.stream().collect(Collectors.joining("/")) , publish_time);
                // brand
                enrichCompanyURLs(complaint.getCompany(), publish_time);
            }

            if(i++ > Configuration.NOTIFY_ROW_SIZE) {
                index = index+i;
                System.out.println(index + " complaints processed!");
                i = 0;
            }
        }
        System.out.println("I'm done! " + companyHashtagURLs.size());
    }

    private void enrichCompanyHashtagURLs(String url, Date publish_time){

        int count = 0;
        try {
            if(companyHashtagURLs.containsKey(url)){
                if(companyHashtagURLs.get(url).getMostUpToDate().after(publish_time))    return;
                count = companyHashtagURLs.get(url).getCount();
            }
        } catch (Exception e) {
            logger.error("Problem when getting companyHashtagUrl! URL: "+ url ,e);
        }
        try {
            companyHashtagURLs.put(url, new URLMeta(publish_time,++count));
        } catch (Exception e) {
            logger.error("new node could not be added! URL: "+ url,e);
        }
    }

    private void enrichCompanyURLs(String url, Date publish_time){

        try {
            if(companyURLs.containsKey(url)){
                if(companyURLs.get(url).after(publish_time))    return;
            }
        } catch (Exception e) {
            logger.error("Problem when getting companyUrl! URL: "+ url ,e);
        }
        try {
            companyURLs.put(url,publish_time);
        } catch (Exception e) {
            logger.error("new node could not be added! URL: "+ url,e);
        }
    }

    private void writeComplaintURLs(){
        HashMap<Integer,Complaint> complaints = MySQLDataOperator.getInstance().complaints;
        complaints.forEach((id,complaint) -> complaintURLs.put(complaint.getUrl(),complaint.getUpdate_time()));
        try {

            Utils.getInstance().writeToFile(complaintURLs,Configuration.FILENAME_SITEMAP_COMPLAINTS);

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("complaintURLs written!");
    }

    public void writeCompanyHashtagURLs(){
        try {

            Utils.getInstance().writeToFile(companyHashtagURLs,Configuration.FILENAME_SITEMAP_COMPANIES_HASHTAGS,true);

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("companyHashtagURLs written!");
    }

    public void writeCompanyURLs(){
        try {

            Utils.getInstance().writeToFile(companyURLs,Configuration.FILENAME_SITEMAP_COMPANIES);

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("companyURLs written!");
    }

}