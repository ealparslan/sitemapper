package com.sikayetvar.sitemap.generator;

import com.google.common.base.CaseFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    public static boolean FORCOMPLAINTLINE = true;

    /* PARAMETERS AND THEIR DEFAULT VALUES. ALL VALUES ARE OVERRIDDEN BY PROPERTIES FILE */
    /**
     * DEBUG flag, for debugging purposes only.
     * It must be false in production environments.
     */
    public static boolean DEBUG = true;

    /**
     * When DEBUG flag is set to true, this many records are read from database
     * This is the number of records from SQL_GET_COMPLAINTHASHTAGS
     */
    public static int DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE = 1000;

    /**
     * In production due to the ram considerations we are processing in batches
     */
    public static int COMPLAINT_HASHTAGS_CHUNK_SIZE = 100000;

    /**
     * When to insert log record while saving entities to database
     */
    public static int NOTIFY_ROW_SIZE = 100;

    /**
     * When to insert log record while saving entities to database
     */
    public static int WRITE_TO_SITEMAP_SIZE = 40000;

    /**
     * Will generate autocomplete index?
     */
    public static boolean STASH_IN_ELASTIC = true;

    /**
     * will serialize computed ConcurrentHashMaps into .ser files
     */
    public static boolean SERIALIZE_DATA = true;

    /**
     * When to insert batch of documents into the elasticsearch
     */
    public static int WRITE_TO_ELASTIC_SIZE = 1000;

    /**
     * For parallel processing. Default 1
     */
    public static int NUMBER_OF_THREADS = 8;

    /**
     * Combination powerset max size. Default 4
     */
    public static int MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION = 2;

    /**
     * URL min complaint threshold
     */
    public static int MIN_NUMBER_OF_COMPLAINTS_ON_URL = 4;

    /**
     * N Top Uptodate Companies
     */
    public static int TOP_N_UPTODATE_COMPANIES = 1000;


    /**
     * WRITE Which
     */
    public static boolean WRITE_TOP_N_UPTODATE_COMPANIES = true;
    public static boolean WRITE_ALL_COMPANIES = false;
    public static boolean WRITE_COMPANIES_HAVING_COMPLAINT = true;
    public static boolean WRITE_COMPLAINTS = true;
    public static boolean WRITE_COMPANIES_HASHTAGS = true;


    public static boolean WRITE_LAST_UPDATE_TIME = false;


    /**
     * Database to be used for data access. Default to "MYSQL"
     */
    public static String DATABASE = "MYSQL";

    /**
     * Database connection URL to connect to database
     */
    //public static String DATABASE_CONNECTION_URL = "jdbc:mysql://curiosity.sikayetvar.com:3307/textmining?characterEncoding=UTF-8&amp;useSSL=false&amp;rewriteBatchedStatements=true";
    public static String DATABASE_CONNECTION_URL = "jdbc:mysql://db1:3306/textmining?characterEncoding=UTF-8&amp;useSSL=false&amp;rewriteBatchedStatements=true";
//    public static String DATABASE_CONNECTION_URL = "jdbc:mysql://complaintlineweb.eastus.cloudapp.azure.com:3306/textmining_en?characterEncoding=UTF-8&amp;useSSL=false&amp;rewriteBatchedStatements=true";


    /**
     * Database username to connect to database
     */
    public static String DATABASE_USERNAME = "dbuser";

    /**
     * Database password to connect to database
     */
    public static String DATABASE_PASSWORD = "muqBX2nKup5S";

    /**
     * Elasticsearch host URL
     */
    public static String ELASTICSEARCH_HOST = "elktomcat";

    /**
     * Elasticsearch port
     */
    public static int ELASTICSEARCH_PORT = 9300;

    /**
     * Elasticsearch index
     */
    public static String ELASTICSEARCH_INDEX = "autocomplete5";

    /**
     * Elasticsearch type
     */
    public static String ELASTICSEARCH_TYPE = "entite";


    /**
     * GetCorpus SQL
     */
    public static String SQL_GET_CORPUS = "select id,term from corpus";

    /**
     * GetCompanies SQL
     */
    public static String SQL_GET_COMPANIES = "select id,name,guncel_sikayet_sayisi,sikayet_almis from v_sitemap_companies";

    /**
     * GetComplaints SQL
     */
    public static String SQL_GET_COMPLAINTS = "select complaint_id,update_time,sikayet_tarihi,complaint_company_id,url,len,silindi from textmining.v_sitemap_complaints";

    /**
     * GetComplainthashtags SQL
     */
    public static String SQL_GET_COMPLAINTHASHTAGS = "select complaint_id,hashtag_id from v_sitemap_complaint_hashtags";

    /**
     * Filename -home
     */
    public static String FILENAME_SITEMAP_INDEX_HOME = "home";

    /**
     * Filename -complaints
     */
    public static String FILENAME_SITEMAP_COMPLAINTS = "complaints";

    /**
     * Filename -companies
     */
    public static String FILENAME_SITEMAP_COMPANIES = "companies";

    /**
     * Filename -companies
     */
    public static String FILENAME_SITEMAP_TOPNCOMPANIES = "topNcompanies";


    /**
     * Filename -companies-hashtags
     */
    public static String FILENAME_SITEMAP_COMPANIES_HASHTAGS = "companies-hashtags";

    /**
     * Sitemap version
     */
    public static String SITEMAP_VERSION = "v2";

    /**
     * Sitemap URL
     */
    public static String SITEMAP_URL = "https://www.sikayetvar.com/sitemap/";

    /**
     * BASE URL
     */
    public static String BASE_URL = "https://www.sikayetvar.com/";



    /**
     * Slack sv chanel
     */
    public static String SLACK_SERVICE_ERRORS_URL = "https://hooks.slack.com/services/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";

    public static boolean NOTIFY_THE_END = true;

    static {
        Configurations configs = new Configurations();
        try {
            File configFile = new File("/Users/da/Documents/IdeaProjects/sitemapper/sitemapper/engine/src/main/resources/engine.properties");

            if (configFile.exists()) {
                org.apache.commons.configuration2.Configuration config = configs.properties(configFile);

                FORCOMPLAINTLINE = config.getBoolean("engine.forcomplaintline", FORCOMPLAINTLINE);
                DEBUG = config.getBoolean("engine.debug", DEBUG);
                DATABASE = config.getString("engine.database", DATABASE);
                DATABASE_CONNECTION_URL = config.getString("engine.databaseConnectionURL", DATABASE_CONNECTION_URL);
                DATABASE_USERNAME = config.getString("engine.databaseUsername", DATABASE_USERNAME);
                DATABASE_PASSWORD = config.getString("engine.databasePassword", DATABASE_PASSWORD);
                ELASTICSEARCH_HOST = config.getString("engine.elasticHost", ELASTICSEARCH_HOST);
                ELASTICSEARCH_PORT = config.getInt("engine.elasticPort", ELASTICSEARCH_PORT);
                ELASTICSEARCH_INDEX = config.getString("engine.elasticIndex", ELASTICSEARCH_INDEX);
                ELASTICSEARCH_TYPE = config.getString("engine.elasticType", ELASTICSEARCH_TYPE);
                DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE = config.getInt("engine.debugComplaintHashtagsSampleSize", DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE);
                COMPLAINT_HASHTAGS_CHUNK_SIZE = config.getInt("engine.complaintHashtagsChunkSize", COMPLAINT_HASHTAGS_CHUNK_SIZE);
                NOTIFY_ROW_SIZE = config.getInt("engine.notifyRowSize", NOTIFY_ROW_SIZE);
                WRITE_TO_SITEMAP_SIZE = config.getInt("engine.writeToSitemapSize", WRITE_TO_SITEMAP_SIZE);
                STASH_IN_ELASTIC = config.getBoolean("engine.stashInElastic", STASH_IN_ELASTIC);
                SERIALIZE_DATA = config.getBoolean("engine.serializeData", SERIALIZE_DATA);
                WRITE_TO_ELASTIC_SIZE = config.getInt("engine.writeToElasticSize", WRITE_TO_ELASTIC_SIZE);
                NUMBER_OF_THREADS = config.getInt("engine.numberOfThreads", NUMBER_OF_THREADS);
                MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION = config.getInt("engine.maxNumberOfHashtagsInCombination", MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION);
                MIN_NUMBER_OF_COMPLAINTS_ON_URL = config.getInt("engine.minNumberOfComplaintsOnURL", MIN_NUMBER_OF_COMPLAINTS_ON_URL);
                TOP_N_UPTODATE_COMPANIES = config.getInt("engine.topNUptodateCompanies", TOP_N_UPTODATE_COMPANIES);
                WRITE_TOP_N_UPTODATE_COMPANIES = config.getBoolean("engine.writeTopNCompanies", WRITE_TOP_N_UPTODATE_COMPANIES);
                WRITE_ALL_COMPANIES = config.getBoolean("engine.writeAllCompanies", WRITE_ALL_COMPANIES);
                WRITE_COMPANIES_HAVING_COMPLAINT = config.getBoolean("engine.writeCompaniesHavingComplaint", WRITE_COMPANIES_HAVING_COMPLAINT);
                WRITE_COMPLAINTS = config.getBoolean("engine.writeComplaints", WRITE_COMPLAINTS);
                WRITE_COMPANIES_HASHTAGS = config.getBoolean("engine.writeCompaniesHashtags", WRITE_COMPANIES_HASHTAGS);
                WRITE_LAST_UPDATE_TIME = config.getBoolean("engine.writeLastUpdateTime", WRITE_LAST_UPDATE_TIME);
                SQL_GET_CORPUS = config.getString("engine.getCorpusSQL", SQL_GET_CORPUS);
                SQL_GET_COMPANIES = config.getString("engine.getCompaniesSQL", SQL_GET_COMPANIES);
                SQL_GET_COMPLAINTS= config.getString("engine.getComplaintsSQL", SQL_GET_COMPLAINTS);
                SQL_GET_COMPLAINTHASHTAGS = config.getString("engine.getComplaintHashtagsSQL", SQL_GET_COMPLAINTHASHTAGS);
                FILENAME_SITEMAP_INDEX_HOME = config.getString("engine.fileNameSitemapIndexHome", FILENAME_SITEMAP_INDEX_HOME);
                FILENAME_SITEMAP_COMPLAINTS = config.getString("engine.fileNameSitemapComplaints", FILENAME_SITEMAP_COMPLAINTS);
                FILENAME_SITEMAP_COMPANIES = config.getString("engine.fileNameSitemapCompanies", FILENAME_SITEMAP_COMPANIES);
                FILENAME_SITEMAP_TOPNCOMPANIES = config.getString("engine.fileNameSitemapTopNCompanies", FILENAME_SITEMAP_TOPNCOMPANIES);
                FILENAME_SITEMAP_COMPANIES_HASHTAGS = config.getString("engine.fileNameSitemapCompaniesHashtags", FILENAME_SITEMAP_COMPANIES_HASHTAGS);
                SITEMAP_VERSION = config.getString("engine.sitemapVersion", SITEMAP_VERSION);
                SITEMAP_URL = config.getString("engine.sitemapURL", SITEMAP_URL);
                BASE_URL = config.getString("engine.baseURL", BASE_URL);
                SLACK_SERVICE_ERRORS_URL = config.getString("engine.slackServiceURL", SLACK_SERVICE_ERRORS_URL);
                NOTIFY_THE_END = config.getBoolean("engine.notifyTheEnd", NOTIFY_THE_END);

            } else {
                logger.info("Config file <" + configFile.getName() + "> not found");
                logger.info("Falling back to default hardcoded values.");
            }

        } catch (ConfigurationException cex) {
            logger.info("Error loading configuration", cex);
            logger.info("Falling back to default hardcoded values.");
        }
    }

    public static String dumpCurrentConfiguration() {
        return "Active configuration\r\n" + Arrays.stream(Configuration.class.getFields()).map(field -> {
            String fieldName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, field.getName());
            try {
                return fieldName + " = " + (fieldName.toLowerCase().contains("password") ? "***********" : field.get(null));
            } catch (IllegalAccessException e) {
                return fieldName + " = N/A";
            }
        }).collect(Collectors.joining("\r\n"));
    }
}
