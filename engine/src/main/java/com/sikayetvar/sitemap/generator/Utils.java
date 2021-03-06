package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import com.sikayetvar.sitemap.generator.entity.Company;
import com.sikayetvar.sitemap.generator.entity.URLMeta;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.Config;

import java.io.*;
import java.net.InetAddress;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.sql.Timestamp;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


/**
 * Created by deniz on 3/16/17.
 */
public class Utils {
    private static Utils instance = null;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    HashMap<String,Integer> companyNameById = new HashMap<>();

    Writer companyMapIndexWriter;
    Writer topNCompanyMapIndexWriter;
    Writer complaintMapIndexWriter;
    Writer hashtagMapIndexWriter;
    TransportClient elasticClient;

    List<String> permutations = new ArrayList<>();

    SlackSender slackSender = new SlackSender(Configuration.SLACK_SERVICE_ERRORS_URL);

    protected Utils() {
        try {

            if(Configuration.STASH_IN_ELASTIC)
                elasticClient = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(new TransportAddress(InetAddress.getByName(Configuration.ELASTICSEARCH_HOST), Configuration.ELASTICSEARCH_PORT));

            if(Configuration.WRITE_TOP_N_UPTODATE_COMPANIES)
            {
                topNCompanyMapIndexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("sitemap/topNcompany"+ Configuration.SITEMAP_VERSION + ".xml"), "utf-8"));
                topNCompanyMapIndexWriter.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            }
            if(Configuration.WRITE_COMPANIES_HASHTAGS)
            {
                hashtagMapIndexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("sitemap/hashtag"+ Configuration.SITEMAP_VERSION + ".xml"), "utf-8"));
                hashtagMapIndexWriter.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            }
            if(Configuration.WRITE_COMPANIES_HAVING_COMPLAINT || Configuration.WRITE_ALL_COMPANIES)
            {
                companyMapIndexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("sitemap/company"+ Configuration.SITEMAP_VERSION + ".xml"), "utf-8"));
                companyMapIndexWriter.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            }
            if(Configuration.WRITE_COMPLAINTS)
            {
                complaintMapIndexWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("sitemap/complaint"+ Configuration.SITEMAP_VERSION + ".xml"), "utf-8"));
                complaintMapIndexWriter.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            logger.error("Could not open sitemapindex file!",e);
        }
        catch (IOException e) {
            logger.error("Could not append sitemapindex file!",e);
        }
    }

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }


    public void  disposeInstance() {
        try {
            if(Configuration.WRITE_TOP_N_UPTODATE_COMPANIES)
            {
                topNCompanyMapIndexWriter.write("</sitemapindex>");
                topNCompanyMapIndexWriter.close();
            }
            if(Configuration.WRITE_COMPANIES_HASHTAGS)
            {
                hashtagMapIndexWriter.write("</sitemapindex>");
                hashtagMapIndexWriter.close();
            }
            if(Configuration.WRITE_COMPANIES_HAVING_COMPLAINT || Configuration.WRITE_ALL_COMPANIES)
            {
                companyMapIndexWriter.write("</sitemapindex>");
                companyMapIndexWriter.close();
            }
            if(Configuration.WRITE_COMPLAINTS)
            {
                complaintMapIndexWriter.write("</sitemapindex>");
                complaintMapIndexWriter.close();
            }

        } catch (Exception e) {
            logger.error("Could not close sitemapindex file!",e);
        }

        if(Configuration.STASH_IN_ELASTIC)
            elasticClient.close();

        instance = null;
    }

    // no call
    public Date toIstDate(String date){
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));
        Date retval = null;
        try {
            retval = dateFormat.parse(date);
        } catch (ParseException e) {
            logger.error("Exception in Utils.toIstDate! String date was: " + date,e);
        }
        return retval;
    }

    public String fromIstDate(Date date){
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));
        return dateFormat.format(date);
    }

    public String toSlug(String input) {
        try {
            String nowhitespace = WHITESPACE.matcher(input.trim().toLowerCase(Locale.ENGLISH)).replaceAll("-");
            String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
            String preprocessed = normalized.replace('??','i');

            String replaced = preprocessed
                                            .replace('_','-')
                                            .replace("--","-")
                                            .replaceAll("[^a-z0-9\\-<>&]","")
                                            .replaceAll("[\\-&<>]+","-");                                            //.replaceAll("<[^>]*>","-");

            String slug = NONLATIN.matcher(replaced).replaceAll("");
            //String slug = replaced.replaceAll("[^\\w-]","");
            return slug;
        } catch (Exception e) {
            logger.error("Error in toSlug. input = /" + input + "/",e);
            return input;
        }
    }

    public SortedMap<URLMeta,String> sortHashMap(Map<String,URLMeta> unsorted){
        TreeMap sorted = new TreeMap();

        for (Map.Entry<String,URLMeta> entry:unsorted.entrySet()) {
            sorted.put(entry.getValue(),entry.getKey());
        }

        return sorted;
    }

    public HashMap<String,URLMeta>  putFirstEntries(int max, SortedMap<URLMeta,String> source) {
        int count = 0;
        HashMap<String,URLMeta> target = new HashMap<>();
        for (Map.Entry<URLMeta,String> entry:source.entrySet()) {
            if (count >= max) break;

            target.put(entry.getValue(),entry.getKey());
            count++;
        }
        return target;
    }

    public Set<SortedSet<String>> powerSet(Set<String> originalSet) {

        Set<SortedSet<String>> sets = new HashSet<>();
        if (originalSet.isEmpty()) {
            sets.add(new TreeSet<>());
            return sets;
        }
        List<String> list = new ArrayList<>(originalSet);
        String head = list.get(0);
        Set<String> rest = new HashSet<>(list.subList(1, list.size()));
        for (SortedSet<String> set : powerSet(rest)) {
            if(set.size() == Configuration.MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION){
                sets.add(set);
                continue;
            }
            SortedSet<String> newSet = new TreeSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    public void writeToFile(ConcurrentHashMap<String,URLMeta> keyUrls, String fileName, boolean checkComplaintCountThreshold) throws FileNotFoundException, UnsupportedEncodingException{

        if(null == keyUrls || keyUrls.size() == 0){
            slackSender.send("Sitemap e yazacak URL yok elimde!!!");
        }

        Writer writer = null;
        try {
            writer =  new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("sitemap/" + fileName +  Configuration.SITEMAP_VERSION  + "-1.xml.gz")), "UTF-8"));
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        } catch (IOException e) {
            logger.error("Could not open first file!",e);
        }
        int sum = 0;
        int index = 1;
        int fileIndex = 1;
        for(Map.Entry<String,URLMeta> entry : keyUrls.entrySet()) {
            sum = sum + entry.getValue().getCount();
            if (checkComplaintCountThreshold && entry.getValue().getCount() < Configuration.MIN_NUMBER_OF_COMPLAINTS_ON_URL) continue;
            index++;
            if(index > Configuration.WRITE_TO_SITEMAP_SIZE) {
                try {
                    writer.append("</urlset>");
                    writer.close();
                    logger.info("closed it!!!" + fileIndex);
                    hashtagMapIndexWriter.write("<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc>"+ (Configuration.WRITE_LAST_UPDATE_TIME ? "<lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis())) + "\n"+ "</lastmod>" : "" ) +"</sitemap>");
                } catch (Exception e) {
                    logger.error("Could not close file!",e);
                }
                fileIndex++;
                index = 1;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("sitemap/" + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz")), "UTF-8"));
                    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
                } catch (IOException e) {
                    logger.error("Could not open next file!",e);
                }            }
            try {
                writer.append("<url><loc>" +  Configuration.BASE_URL  + entry.getKey() + "</loc>" + ((Configuration.WRITE_LAST_UPDATE_TIME && (!Configuration.FORCOMPLAINTLINE || !fromIstDate(entry.getValue().getMostUpToDate()).startsWith("2010-01-01")))? "<lastmod>" +  fromIstDate(entry.getValue().getMostUpToDate()) + "\n" + "</lastmod>" : "" )+ "</url>");

            } catch (IOException e) {
                logger.error("Could not write to file! " + entry.getKey() + " " + entry.getValue(),e);
            }
        }
        try {
            writer.append("</urlset>");
            writer.close();
            logger.info("closed it!!!" + fileIndex);
            hashtagMapIndexWriter.write("<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc>"+ (Configuration.WRITE_LAST_UPDATE_TIME ? "<lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis())) + "\n"+ "</lastmod>" : "" ) +"</sitemap>");
        } catch (IOException e) {
            logger.error("Could not close file!",e);
        }
        logger.info("Total :" + sum);
    }
    public void writeToFile(Map<String,URLMeta> keyUrls, String fileName, String whichSitemap) throws FileNotFoundException, UnsupportedEncodingException{

        if(null == keyUrls || keyUrls.size() == 0){
            slackSender.send("Sitemap e yazacak URL yok elimde!!!");
        }

        Writer writer = null;
        try {
            writer =  new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("sitemap/" + fileName +  Configuration.SITEMAP_VERSION  + "-1.xml.gz")), "UTF-8"));
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        } catch (IOException e) {
            logger.error("Could not open first file!",e);
        }
        int index = 1;
        int fileIndex = 1;
        for(Map.Entry<String,URLMeta> entry : keyUrls.entrySet()) {
            index++;
            if(index > Configuration.WRITE_TO_SITEMAP_SIZE) {
                try {
                    writer.append("</urlset>");
                    writer.close();
                    logger.info("closed it!!!" + fileIndex);
                    String writeMainSitemapIndex = "<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc>"+ (Configuration.WRITE_LAST_UPDATE_TIME ? "<lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis())) + "\n"+ "</lastmod>" : "" ) +"</sitemap>";
                    if (whichSitemap.equals("company"))
                        companyMapIndexWriter.write(writeMainSitemapIndex);
                    else if (whichSitemap.equals("complaint"))
                        complaintMapIndexWriter.write(writeMainSitemapIndex);
                    else if (whichSitemap.equals("topN"))
                        topNCompanyMapIndexWriter.write(writeMainSitemapIndex);
                    else
                        logger.error("Sitemap type is not selected! ");
                } catch (Exception e) {
                    logger.error("Could not close file!",e);
                }
                fileIndex++;
                index = 1;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("sitemap/" + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz")), "UTF-8"));
                    writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
                } catch (IOException e) {
                    logger.error("Could not open next file!",e);
                }
            }
            try {
                writer.append("<url><loc>" +  Configuration.BASE_URL  + entry.getKey() + "</loc>" + ((Configuration.WRITE_LAST_UPDATE_TIME && (!Configuration.FORCOMPLAINTLINE || !fromIstDate(entry.getValue().getMostUpToDate()).startsWith("2010-01-01")))? "<lastmod>" +  fromIstDate(entry.getValue().getMostUpToDate()) + "\n" + "</lastmod>" : "" )+ "</url>");
            } catch (IOException e) {
                logger.error("Could not write to file! " + entry.getKey() + " " + entry.getValue(),e);
            }
        }
        try {
            writer.append("</urlset>");
            writer.close();
            logger.info("closed it!!!" + fileIndex);
            String writeMainSitemapIndex = "<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc>"+ (Configuration.WRITE_LAST_UPDATE_TIME ? "<lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis())) + "\n"+ "</lastmod>" : "" ) +"</sitemap>";
            if (whichSitemap.equals("company"))
                companyMapIndexWriter.write(writeMainSitemapIndex);
            else if (whichSitemap.equals("complaint"))
                complaintMapIndexWriter.write(writeMainSitemapIndex);
            else if (whichSitemap.equals("topN"))
                topNCompanyMapIndexWriter.write(writeMainSitemapIndex);
            else
                logger.error("Sitemap type is not selected! ");
        }
        catch (Exception ex){
            logger.error(ex.getMessage(),ex );
        }
    }

    public void elasticPrepare(){

        for(Map.Entry<Integer,Company> entry : MySQLDataOperator.getInstance().companies.entrySet()) {
            companyNameById.put(entry.getValue().getName(),entry.getKey());
        }
    }

    public void stashInElastic(ConcurrentHashMap<String,URLMeta> keyUrls, boolean forCompanies){

        BulkRequestBuilder bulkRequest = elasticClient.prepareBulk();

        int count = 0;

        for(Map.Entry<String,URLMeta> entry : keyUrls.entrySet()) {
            count++;
            List<String> words = new ArrayList<>();
            words.addAll(entry.getValue().getContent());

//            String documentId = entry.getKey();
//            // we need to seperate brand URLs with hashtag URLs. Forexample we have both Garanti Bankasi brand and garanti bankasi hashtag.
//            if(entry.getValue().getCardinality() == 1 && entry.getValue().getPriority() == 1)
//                documentId =  documentId.concat("-b");

            try {
                bulkRequest.add(elasticClient.prepareIndex(Configuration.ELASTICSEARCH_INDEX,Configuration.ELASTICSEARCH_TYPE, entry.getKey()).setSource(jsonify(entry.getValue(),forCompanies)));
            } catch (Exception e) {
                logger.error("Problem in preparing bulk request");
            }
            permutations.clear();
            if (count == Configuration.WRITE_TO_ELASTIC_SIZE){
                BulkResponse bulkResponse = bulkRequest.get();

                if (bulkResponse.hasFailures()) {
                    logger.error("Failure in Elastic Stash: " + bulkResponse.toString());
                }
                count = 0;
                bulkRequest = elasticClient.prepareBulk();
            }

        }

    }



    private XContentBuilder jsonify(URLMeta meta, boolean forCompanies){

        int companyId = 0;
        int boost = 0;
        if(meta.getPriority() == 1)
            boost = 10000000;
        else{
            if(meta.getCardinality()==2) boost = 100000;
            else if(meta.getCardinality()==1) boost = 1000000;
        }


        List<String> words = new ArrayList<>();
        words.addAll(meta.getContent());

        if(forCompanies){
            if (null != words.get(0))
                companyId = companyNameById.get(words.get(0));
        }


        permute(words,0);
        XContentBuilder xContentBuilder = null;
        try {
            xContentBuilder =  jsonBuilder()
                    .startObject()
                    .field("priority", meta.getPriority())
                    .field("cardinality", meta.getCardinality())
                    .field("company",companyId)
                    .startObject("words")
                    .field("input",permutations)
                    .field("weight", meta.getCount() + boost)
                    .endObject()
                    .endObject();
        } catch (IOException e) {
            logger.error("Error occured in jsonification for elastic stash!",e);
        }
        return xContentBuilder;
    }


    private void permute(List<String> arr, int k){

        for(int i = k; i < arr.size(); i++){
            Collections.swap(arr, i, k);
            permute(arr, k+1);
            Collections.swap(arr, k, i);
        }
        if (k == arr.size() -1){
            permutations.add(arr.stream().collect(Collectors.joining(" ")));
        }
    }



}
