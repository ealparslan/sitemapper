package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import com.sikayetvar.sitemap.generator.entity.Complaint;
import com.sikayetvar.sitemap.generator.entity.ComplaintHashtag;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map.Entry;
import com.google.common.collect.Lists;
import java.io.*;
import java.util.stream.Collectors;

import com.sikayetvar.sitemap.generator.entity.URLMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ecyrd.speed4j.StopWatch;

import javax.rmi.CORBA.Util;


/**
 * Created by deniz on 3/14/17.
 */
public class Controller {

    public List<ComplaintHashtag> complaint_hashtags_list = new ArrayList<ComplaintHashtag>();
    public List<ComplaintHashtag> all_complaint_hashtags_list = new ArrayList<ComplaintHashtag>();


    private ConcurrentHashMap<String,URLMeta> companyHashtagURLs = new ConcurrentHashMap<String,URLMeta>();
    private ConcurrentHashMap<String,Date> companyURLs = new ConcurrentHashMap<String,Date>();
    private SortedMap<String,Date> topNCompanyURLs = new TreeMap<>();

    public ConcurrentHashMap<String, URLMeta> getCompanyHashtagURLs() {return companyHashtagURLs;}

    private HashMap<String,Date> complaintURLs = new HashMap<String,Date>();
    private int index;
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private static SlackSender slackSender = new SlackSender(Configuration.SLACK_SERVICE_ERRORS_URL);

    public static void main(String[] args) {

        StopWatch sw = new StopWatch();
        MemoryWatch mw = new MemoryWatch();
        Controller controller = new Controller();

        logger.info(Configuration.dumpCurrentConfiguration());

        sw.start();

        MySQLDataOperator.getInstance().initDatasources();

        controller.writeComplaintURLs();

        controller.all_complaint_hashtags_list = MySQLDataOperator.getInstance().getComplaintHashtags();

        int chunkIndex=0;
        int fromIndex=0;
        int toIndex=Configuration.COMPLAINT_HASHTAGS_CHUNK_SIZE;
        do{
            if (toIndex > controller.all_complaint_hashtags_list.size())
                toIndex = controller.all_complaint_hashtags_list.size();
            controller.complaint_hashtags_list = controller.all_complaint_hashtags_list.subList(fromIndex,toIndex);
            logger.info(controller.complaint_hashtags_list.size() + " Complaints Loaded!");
            controller.processChunk();
            chunkIndex++;
            fromIndex = chunkIndex * Configuration.COMPLAINT_HASHTAGS_CHUNK_SIZE + 1;
            toIndex = (chunkIndex+1) * Configuration.COMPLAINT_HASHTAGS_CHUNK_SIZE;
        }while (fromIndex < controller.all_complaint_hashtags_list.size());

        logger.info("companyHashtagURLs Calculated!");

        controller.writeTopNCompanyURLs();
        controller.writeCompanyHashtagURLs();
        controller.writeCompanyURLs();
        logger.info(sw.toString());

        if(Configuration.NOTIFY_THE_END)
            slackSender.send("Tamam tamam bugunu de hallettik!!! Bikac gun bu uyariyi yollayacam");

        Utils.getInstance().disposeInstance();
    }

    public void processChunk(){
        //logger.info(complaint_hashtags_list.size() + " will be processed in this chunk! ");
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
        //System.out.println("I'm starting!");
        for (ComplaintHashtag complaint:complaint_hashtags_list) {

            Date publish_time = complaint.getPublish_time();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.YEAR, -1);

            Date update_time = complaint.getUpdate_time();

            if (complaint.isMalformed()) continue;

            // brand
            enrichCompanyURLs(complaint.getCompany(), update_time);

            if(publish_time.before(cal.getTime())) continue; // company hastags and hashtags url is not affedcted with prior than 1 year complaints

            // foreach hashtag combination of a complaint
            for (Set<String> combination:complaint.getCombinations()) {
                if(combination.isEmpty()) continue;
                // brand + hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(complaint.getCompany() + "/" + combination.stream().collect(Collectors.joining("/")) , update_time);
                // hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(combination.stream().collect(Collectors.joining("/")) , update_time);
            }

            if(i++ > Configuration.NOTIFY_ROW_SIZE) {
                index = index+i;
                System.out.println(index + " complaints processed!");
                i = 0;
            }
        }
        //System.out.println("I'm done! " + companyHashtagURLs.size());
    }


    private void enrichCompanyHashtagURLs(String url, Date update_time){


        int count = 0;
        Date mostUpToDate = update_time;
        try {
            if(companyHashtagURLs.containsKey(url)){
                if(companyHashtagURLs.get(url).getMostUpToDate().after(update_time))
                    mostUpToDate = companyHashtagURLs.get(url).getMostUpToDate();
                count = companyHashtagURLs.get(url).getCount();
            }

        } catch (Exception e) {
            logger.error("Problem when getting companyHashtagUrl! URL: "+ url ,e);
        }
        try {
            companyHashtagURLs.put(url, new URLMeta(mostUpToDate,++count));
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
        complaints.forEach((id,complaint)
                -> {
            if (complaint.getLen() > 200 && complaint.getSilindi() != 1)
                complaintURLs.put(complaint.getUrl(),complaint.getUpdate_time());
        });
        try {

            Utils.getInstance().writeToFile(complaintURLs,Configuration.FILENAME_SITEMAP_COMPLAINTS, "complaint");

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("complaintURLs written!");
    }

    public void writeCompanyHashtagURLs(){
        try {
            logger.info("Total CompanyHashtagUrl: " + companyHashtagURLs.size());

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

            Utils.getInstance().writeToFile(companyURLs,Configuration.FILENAME_SITEMAP_COMPANIES,"company");

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("companyURLs written!");
    }

    public void writeTopNCompanyURLs(){
        try {
            topNCompanyURLs = Utils.getInstance().putFirstEntries(Configuration.TOP_N_UPTODATE_COMPANIES, Utils.getInstance().sortHashMap(companyURLs));
            Utils.getInstance().writeToFile(topNCompanyURLs,Configuration.FILENAME_SITEMAP_TOPNCOMPANIES,"topN");

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("companyURLs written!");
    }

}
