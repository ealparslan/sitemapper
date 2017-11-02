package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.entity.URLMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.Key;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.sql.Timestamp;


/**
 * Created by deniz on 3/16/17.
 */
public class Utils {
    private static Utils instance = null;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    Writer companyMapIndexWriter;
    Writer topNCompanyMapIndexWriter;
    Writer complaintMapIndexWriter;
    Writer hashtagMapIndexWriter;

    SlackSender slackSender = new SlackSender(Configuration.SLACK_SERVICE_ERRORS_URL);

    protected Utils() {
        try {

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

        } catch (IOException e) {
            logger.error("Could not close sitemapindex file!",e);
        }
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
            String preprocessed = normalized.replace('ı','i');

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

    public SortedMap<String,Date> sortHashMap(Map<String,Date> unsorted){
        TreeMap sorted = new TreeMap();
        sorted.putAll(unsorted);
        return sorted;
    }

    public SortedMap<String,Date>  putFirstEntries(int max, SortedMap<String,Date> source) {
        int count = 0;
        TreeMap<String,Date> target = new TreeMap<>();
        for (Map.Entry<String,Date> entry:source.entrySet()) {
            if (count >= max) break;

            target.put(entry.getKey(), entry.getValue());
            count++;
        }
        return target;
    }

    public Set<Set<String>> powerSet(Set<String> originalSet) {

        Set<Set<String>> sets = new HashSet<Set<String>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<String>());
            return sets;
        }
        List<String> list = new ArrayList<String>(originalSet);
        String head = list.get(0);
        Set<String> rest = new HashSet<String>(list.subList(1, list.size()));
        for (Set<String> set : powerSet(rest)) {
            if(set.size() == Configuration.MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION){
                sets.add(set);
                continue;
            }
            Set<String> newSet = new TreeSet<String>();
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
                    hashtagMapIndexWriter.write("<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc><lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis())) + "\n"+ "</lastmod></sitemap>");
                } catch (IOException e) {
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
                writer.append("<url><loc>https://www.sikayetvar.com/" + entry.getKey() + "</loc><lastmod>"+ fromIstDate(entry.getValue().getMostUpToDate()) + "</lastmod></url>");
            } catch (IOException e) {
                logger.error("Could not write to file! " + entry.getKey() + " " + entry.getValue(),e);
            }
        }
        try {
            writer.append("</urlset>");
            writer.close();
            hashtagMapIndexWriter.write("<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc><lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis()))+ "\n"+ "</lastmod></sitemap>");
        } catch (IOException e) {
            logger.error("Could not close file!",e);
        }
        logger.info("Total :" + sum);
    }
    public void writeToFile(Map<String,Date> keyUrls, String fileName, String whichSitemap) throws FileNotFoundException, UnsupportedEncodingException{

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
        for(Map.Entry<String,Date> entry : keyUrls.entrySet()) {
            index++;
            if(index > Configuration.WRITE_TO_SITEMAP_SIZE) {
                try {
                    writer.append("</urlset>");
                    writer.close();
                    String writeMainSitemapIndex = "<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc><lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis()))+ "\n"+ "</lastmod></sitemap>";
                    if (whichSitemap.equals("company"))
                        companyMapIndexWriter.write(writeMainSitemapIndex);
                    else if (whichSitemap.equals("complaint"))
                        complaintMapIndexWriter.write(writeMainSitemapIndex);
                    else if (whichSitemap.equals("topN"))
                        topNCompanyMapIndexWriter.write(writeMainSitemapIndex);
                    else
                        logger.error("Sitemap type is not selected! ");
                } catch (IOException e) {
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
                writer.append("<url><loc>https://www.sikayetvar.com/" + entry.getKey() + "</loc><lastmod>"+ fromIstDate(entry.getValue()) + "</lastmod></url>");
            } catch (IOException e) {
                logger.error("Could not write to file! " + entry.getKey() + " " + entry.getValue(),e);
            }
        }
        try {
            writer.append("</urlset>");
            writer.close();
            String writeMainSitemapIndex ="<sitemap><loc>" + Configuration.SITEMAP_URL + fileName +  Configuration.SITEMAP_VERSION  + "-" + fileIndex + ".xml.gz" + "</loc><lastmod>" +  dateFormat.format(new Timestamp(System.currentTimeMillis()))+ "\n"+ "</lastmod></sitemap>";
            if (whichSitemap.equals("company"))
                companyMapIndexWriter.write(writeMainSitemapIndex);
            else if (whichSitemap.equals("complaint"))
                complaintMapIndexWriter.write(writeMainSitemapIndex);
            else if (whichSitemap.equals("topN"))
                topNCompanyMapIndexWriter.write(writeMainSitemapIndex);
            else
                logger.error("Sitemap type is not selected! ");
        } catch (IOException e) {
            logger.error("Could not close file!",e);
        }
    }

}
