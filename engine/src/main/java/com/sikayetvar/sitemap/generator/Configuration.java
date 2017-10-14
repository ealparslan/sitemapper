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
    public static int DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE = 10000000;

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
     * Database to be used for data access. Default to "MYSQL"
     */
    public static String DATABASE = "MYSQL";

    /**
     * Database connection URL to connect to database
     */
    //public static String DATABASE_CONNECTION_URL = "jdbc:mysql://curiosity.sikayetvar.com:3307/textmining?characterEncoding=UTF-8&amp;useSSL=false&amp;rewriteBatchedStatements=true";
    public static String DATABASE_CONNECTION_URL = "jdbc:mysql://localhost/textmining?characterEncoding=UTF-8&amp;useSSL=false&amp;rewriteBatchedStatements=true";

    /**
     * Database username to connect to database
     */
    public static String DATABASE_USERNAME = "textminer";

    /**
     * Database password to connect to database
     */
    public static String DATABASE_PASSWORD = "3BY98DJnmtfpZgzmusT3";

    /**
     * Redis host URL for complaint detail set
     */
    public static String REDIS_COMPLAINT_DETAIL_HOST = "luna.sikayetvar.com";

    /**
     * Redis port for complaint detail set
     */
    public static int REDIS_COMPLAINT_DETAIL_PORT = 6382;

    /**
     * Database username to connect to database
     */
    public static String REDIS_COMPLAINT_DETAIL_HOST_USERNAME = "";

    /**
     * Database password to connect to database
     */
    public static String REDIS_COMPLAINT_DETAIL_HOST_PASSWORD = "";

    /**
     * GetCorpus SQL
     */
    public static String SQL_GET_CORPUS = "select id,term from textmining.corpus";

    /**
     * GetCompanies SQL
     */
    public static String SQL_GET_COMPANIES = "select id,name from textmining.v_sitemap_companies";

    /**
     * GetComplaints SQL
     */
    public static String SQL_GET_COMPLAINTS = "select complaint_id,update_time,complaint_company_id,url,len,silindi from textmining.v_sitemap_complaints";

    /**
     * GetComplainthashtags SQL
     */
    public static String SQL_GET_COMPLAINTHASHTAGS = "select complaint_id,hashtag_id from textmining.v_sitemap_complaint_hashtags";

    /**
     * Filename sitemapindex-home
     */
    public static String FILENAME_SITEMAP_INDEX_HOME = "sitemapindex-home";

    /**
     * Filename sitemapindex-complaints
     */
    public static String FILENAME_SITEMAP_COMPLAINTS = "sitemapindex-complaints";

    /**
     * Filename sitemapindex-companies
     */
    public static String FILENAME_SITEMAP_COMPANIES = "sitemapindex-companies";

    /**
     * Filename sitemapindex-companies-hashtags
     */
    public static String FILENAME_SITEMAP_COMPANIES_HASHTAGS = "sitemapindex-companies-hashtags";

    /**
     * Sitemap version
     */
    public static String SITEMAP_VERSION = "v2";

    /**
     * Slack sv chanel
     */
    public static String SLACK_SERVICE_ERRORS_URL = "https://hooks.slack.com/services/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";

    public static boolean NOTIFY_THE_END = true;

    static {
        Configurations configs = new Configurations();
        try {
            File configFile = new File("engine.properties");

            if (configFile.exists()) {
                org.apache.commons.configuration2.Configuration config = configs.properties(configFile);

                DEBUG = config.getBoolean("engine.debug", DEBUG);
                DATABASE = config.getString("engine.database", DATABASE);
                DATABASE_CONNECTION_URL = config.getString("engine.databaseConnectionURL", DATABASE_CONNECTION_URL);
                DATABASE_USERNAME = config.getString("engine.databaseUsername", DATABASE_USERNAME);
                DATABASE_PASSWORD = config.getString("engine.databasePassword", DATABASE_PASSWORD);
                DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE = config.getInt("engine.debugComplaintHashtagsSampleSize", DEBUG_COMPLAINT_HASHTAGS_SAMPLE_SIZE);
                COMPLAINT_HASHTAGS_CHUNK_SIZE = config.getInt("engine.complaintHashtagsChunkSize", COMPLAINT_HASHTAGS_CHUNK_SIZE);
                NOTIFY_ROW_SIZE = config.getInt("engine.notifyRowSize", NOTIFY_ROW_SIZE);
                WRITE_TO_SITEMAP_SIZE = config.getInt("engine.writeToSitemapSize", WRITE_TO_SITEMAP_SIZE);
                NUMBER_OF_THREADS = config.getInt("engine.numberOfThreads", NUMBER_OF_THREADS);
                MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION = config.getInt("engine.maxNumberOfHashtagsInCombination", MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION);
                MIN_NUMBER_OF_COMPLAINTS_ON_URL = config.getInt("engine.minNumberOfComplaintsOnURL", MIN_NUMBER_OF_COMPLAINTS_ON_URL);
                SQL_GET_CORPUS = config.getString("engine.getCorpusSQL", SQL_GET_CORPUS);
                SQL_GET_COMPANIES = config.getString("engine.getCompaniesSQL", SQL_GET_COMPANIES);
                SQL_GET_COMPLAINTS= config.getString("engine.getComplaintsSQL", SQL_GET_COMPLAINTS);
                SQL_GET_COMPLAINTHASHTAGS = config.getString("engine.getComplaintHashtagsSQL", SQL_GET_COMPLAINTHASHTAGS);
                FILENAME_SITEMAP_INDEX_HOME = config.getString("engine.fileNameSitemapIndexHome", FILENAME_SITEMAP_INDEX_HOME);
                FILENAME_SITEMAP_COMPLAINTS = config.getString("engine.fileNameSitemapComplaints", FILENAME_SITEMAP_COMPLAINTS);
                FILENAME_SITEMAP_COMPANIES = config.getString("engine.fileNameSitemapCompanies", FILENAME_SITEMAP_COMPANIES);
                FILENAME_SITEMAP_COMPANIES_HASHTAGS = config.getString("engine.fileNameSitemapCompaniesHashtags", FILENAME_SITEMAP_COMPANIES_HASHTAGS);
                SITEMAP_VERSION = config.getString("engine.sitemapVersion", SITEMAP_VERSION);
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
