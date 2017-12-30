package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import com.sikayetvar.sitemap.generator.entity.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.collect.Lists;
import java.io.*;
import java.util.stream.Collectors;

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
    public ConcurrentHashMap<String,URLMeta> companyHashtagURLs = new ConcurrentHashMap<String,URLMeta>();
    public ConcurrentHashMap<String,URLMeta> companyURLs = new ConcurrentHashMap<>();
    private HashMap<String,URLMeta> topNCompanyURLs = new HashMap<>();
    public ConcurrentHashMap<String, URLMeta> getCompanyHashtagURLs() {return companyHashtagURLs;}
    private HashMap<String,URLMeta> complaintURLs = new HashMap<String,URLMeta>();
    private int index;
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);
    private static SlackSender slackSender = new SlackSender(Configuration.SLACK_SERVICE_ERRORS_URL);


    public static void main(String[] args) {

        StopWatch sw = new StopWatch();
        MemoryWatch mw = new MemoryWatch();
        Controller controller = new Controller();

        logger.info(Configuration.dumpCurrentConfiguration());

        sw.start();

        // fetch Corpus , Companies , Hashtags from database
        MySQLDataOperator.getInstance().initDatasources();

        if(Configuration.WRITE_COMPLAINTS) controller.writeComplaintURLs();

        // fetch ComplaintHashtags from database and save into List<ComplaintHashtag> all_complaint_hashtags_list
        controller.all_complaint_hashtags_list = MySQLDataOperator.getInstance().getComplaintHashtags();

        // fill ConcurrentHashMap<String,URLMeta> companyURLs with default date:TODAY and the other fields correct from database
        controller.fillCompanyURLs();

        // begin parallel processing on List<ComplaintHashtag> all_complaint_hashtags_list
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

        if(Configuration.SERIALIZE_DATA){
            controller.serializeObjects("./company_urls.ser",controller.companyURLs);
            controller.serializeObjects("./company_hashtag_urls.ser",controller.companyHashtagURLs);
            logger.info("serialization completed!");
        }

        if(Configuration.STASH_IN_ELASTIC){
            Utils.getInstance().elasticPrepare();
            Utils.getInstance().stashInElastic(controller.companyHashtagURLs,false);
            Utils.getInstance().stashInElastic(controller.companyURLs,true);
            logger.info("elasticsearch stashed!");
        }

        if(Configuration.WRITE_TOP_N_UPTODATE_COMPANIES)
            controller.writeTopNCompanyURLs();
        if(Configuration.WRITE_COMPANIES_HASHTAGS)
            controller.writeCompanyHashtagURLs();
        if(Configuration.WRITE_COMPANIES_HAVING_COMPLAINT)
            controller.writeCompanyURLs();
        if(Configuration.WRITE_ALL_COMPANIES)
            controller.writeAllCompanyURLs();
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

            // we have companyURLs from previous fillCompanies step. But we have to adjust mostUptoDate variable due to the last complaint of the company
            enrichCompanyURLs(complaint.getCompany(), update_time);

            if(publish_time.before(cal.getTime())) continue; // company hastags and hashtags url is not affected with prior than 1 year complaints

            // foreach hashtag combination of a complaint
            for (SortedSet<String> combination:complaint.getCombinations()) {
                if(combination.isEmpty()) continue;
                // hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(combination, null , update_time);
                // brand + hashtag1 + hashtag2 .....
                enrichCompanyHashtagURLs(combination,complaint.getCompany(), update_time);

            }

            if(i++ > Configuration.NOTIFY_ROW_SIZE) {
                index = index+i;
                System.out.println(index + " complaints processed!");
                i = 0;
            }
        }
        //System.out.println("I'm done! " + companyHashtagURLs.size());
    }


    public void enrichCompanyHashtagURLs(SortedSet<String> combination,String company, Date update_time){

        StringBuilder sb;
        int count = 0;
        int priority = 3;
        Date mostUpToDate = update_time;
        String url;
        String hashtagPart = combination.stream().map(h -> h = Utils.getInstance().toSlug(h)).collect(Collectors.joining("/"));

        // we use combinationPersist because combination may be overrided in different steps of the application espacially in function enrichCompanyHashtagURLs(combination,complaint.getCompany(), update_time);
        SortedSet<String> combinationPersist = new TreeSet<>();
        combination.stream().forEach(comb -> combinationPersist.add(comb));

        if (null != company){
            sb= new StringBuilder(Utils.getInstance().toSlug(company));
            url = (sb.append("/")).append(hashtagPart).toString();
            combinationPersist.add(company); // this is only for autocomplete business
            priority = 2;
        }
        else{
            url = hashtagPart;
        }

        try {
            if(companyHashtagURLs.containsKey(url)){
                if(companyHashtagURLs.get(url).getMostUpToDate().after(update_time))
                    mostUpToDate = companyHashtagURLs.get(url).getMostUpToDate();
                count = companyHashtagURLs.get(url).getCount();
                priority = companyHashtagURLs.get(url).getPriority();
            }

        } catch (Exception e) {
            logger.error("Problem when getting companyHashtagUrl! URL: "+ url ,e);
        }
        try {
            companyHashtagURLs.put(url, new URLMeta(mostUpToDate,++count,combinationPersist,combinationPersist.size(),priority));
        } catch (Exception e) {
            logger.error("new node could not be added! URL: "+ url,e);
        }
    }

    private void enrichCompanyURLs(String company, Date publish_time){
        String url = Utils.getInstance().toSlug(company);

        try {
            if(companyURLs.containsKey(url)){
                if(companyURLs.get(url).getMostUpToDate().after(publish_time))
                    return;
                else
                    companyURLs.get(url).setMostUpToDate(publish_time);
            }
            else
                logger.error("No CompanyURL found with key: "+ url);
        } catch (Exception e) {
            logger.error("Problem when getting companyUrl! URL: "+ url ,e);
        }
    }

    private void fillCompanyURLs(){
        HashMap<Integer,Company> companies = MySQLDataOperator.getInstance().companies;

        Date milat = new Date();
        try {
            milat = new SimpleDateFormat("dd-MM-yyyy").parse("01-01-2010");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date tempMilat = milat;

        companies.forEach((id, company)
                -> {
            if(null != company) {
                Set<String> content = new HashSet<>();
                content.add(company.getName());
                try {
                    companyURLs.put(Utils.getInstance().toSlug(company.getName()),new URLMeta(tempMilat,company.getComplaintCount(),content,1,1));
                    // date field must be updated at enrichCompanyURLs function, in order to fetch most up-to-date 1000 companies
                } catch (Exception e) {
                    logger.error("complaint count: " + company.getComplaintCount() + " name: "+ company.getName(),e);
                }
            }
        });
    }

    private void writeAllCompanyURLs(){
        try {

            Utils.getInstance().writeToFile(companyURLs,Configuration.FILENAME_SITEMAP_COMPANIES, "company");

        } catch (FileNotFoundException e) {
            logger.error("Controller.Main FileNotFoundException during writing into file",e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Controller.Main UnsupportedEncodingException during writing into file",e);
        }
        logger.info("cumpanyURLs written!");
    }

    private void writeComplaintURLs(){
        HashMap<Integer,Complaint> complaints = MySQLDataOperator.getInstance().complaints;
        complaints.forEach((id,complaint)
                -> {
            if (complaint.getLen() > 200 && complaint.getSilindi() != 1)
                complaintURLs.put(complaint.getUrl(),new URLMeta(complaint.getUpdate_time(),1,"",1,4));
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
        HashMap<String,URLMeta> companyURLsHavingComplaint = new HashMap<>();
        try {
            // establish a new HashMap<String,URLMeta> for the companies only having at least 1 complaint
            for(Map.Entry<String,URLMeta> entry : companyURLs.entrySet()) {
                if(entry.getValue().getCount() > 0)
                    companyURLsHavingComplaint.put(entry.getKey(),entry.getValue());
            }

            Utils.getInstance().writeToFile(companyURLsHavingComplaint,Configuration.FILENAME_SITEMAP_COMPANIES,"company");

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

    public void serializeObjects(String path, Object source){

        try {
            FileOutputStream fout = new FileOutputStream(path);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(source);
            oos.close();
            fout.close();
        } catch (FileNotFoundException e) {
            logger.error("serialize file not found!");
        } catch (IOException e) {
            logger.error("serialize object problem!");
        }
        logger.info("serialization completed for : " + source.getClass().getSimpleName());
    }

}
