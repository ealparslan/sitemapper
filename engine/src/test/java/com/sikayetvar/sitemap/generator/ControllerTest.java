package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.MySQLDataOperator;
import com.sikayetvar.sitemap.generator.entity.ComplaintHashtag;
import com.sikayetvar.sitemap.generator.entity.URLMeta;
import org.junit.Test;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by deniz on 3/18/17.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    List<CH> mockComplaintHashtags = new ArrayList<>();
    Controller controller = new Controller();


    public class CH {
        public Integer getComplaint() {
            return complaint;
        }

        public void setComplaint(Integer complaint) {
            this.complaint = complaint;
        }

        public Integer getHashtag() {
            return hashtag;
        }

        public void setHashtag(Integer hashtag) {
            this.hashtag = hashtag;
        }

        public CH(Integer complaint, Integer hashtag) {
            this.complaint = complaint;
            this.hashtag = hashtag;
        }

        private Integer complaint;
        private Integer hashtag;
    }

    public ControllerTest() {
    }

    @Before
    public void setUp() {

        //region Mock initialization
        mockComplaintHashtags.add(new CH(332337,342831));//aksam
        mockComplaintHashtags.add(new CH(332337,515722));//mesai
        mockComplaintHashtags.add(new CH(332337,516736));//mesgul
        mockComplaintHashtags.add(new CH(332337,522873));//mp3

        mockComplaintHashtags.add(new CH(1591090,342831));//aksam
        mockComplaintHashtags.add(new CH(1591090,548564));//piranha
        mockComplaintHashtags.add(new CH(1591090,574478));//siparis
        mockComplaintHashtags.add(new CH(1591090,600760));//tip

        mockComplaintHashtags.add(new CH(1599934,342831));//aksam
        mockComplaintHashtags.add(new CH(1599934,643203));//cal
        mockComplaintHashtags.add(new CH(1599934,643208));//calar
        mockComplaintHashtags.add(new CH(1599934,515722));//mesai

        mockComplaintHashtags.add(new CH(1646380,515722));//0000-00-00 00:00:00
        mockComplaintHashtags.add(new CH(1646380,342831));//0000-00-00 00:00:00
        mockComplaintHashtags.add(new CH(1646380,548564));//0000-00-00 00:00:00
        mockComplaintHashtags.add(new CH(1617122,548564));//to cycle loop
        //endregion

        MySQLDataOperator.getInstance().initDatasources();
        for (CH ch:mockComplaintHashtags) {
            MySQLDataOperator.getInstance().processComplaintHashtags(ch.getComplaint(),ch.getHashtag());
        }
        controller.complaint_hashtags_list = MySQLDataOperator.getInstance().getComplaint_hashtags_list();
        controller.processChunk();
        logger.info("Processor returned! ");

    }

    @Test
    public void test1CombinationOutput() throws Exception{

        logger.info("Test started! ");
        //region Assertion
        HashMap<String,String> assertion = new HashMap<>();
        assertion.put("siparis","");
        assertion.put("koleston/aksam/mesai","");
        assertion.put("weblepi-ajans/aksam/mesgul","");
        assertion.put("yapi-kredi-bankasi/piranha/tip","");
        assertion.put("yapi-kredi-bankasi/siparis/tip","");
        assertion.put("weblepi-ajans/mesai/mesgul","");
        assertion.put("koleston/cal/calar","");
        assertion.put("cal/mesai","");
        assertion.put("aksam/tip","");
        assertion.put("piranha/siparis","");
        assertion.put("weblepi-ajans/aksam/mesai","");
        assertion.put("yapi-kredi-bankasi/aksam/siparis","");
        assertion.put("koleston/aksam/cal","");
        assertion.put("tip","");
        assertion.put("weblepi-ajans/mesai/mp3","");
        assertion.put("aksam/calar","");
        assertion.put("aksam/cal","");
        assertion.put("piranha","");
        assertion.put("aksam/piranha","");
        assertion.put("koleston/mesai","");
        assertion.put("aksam/siparis","");
        assertion.put("siparis/tip","");
        assertion.put("koleston/aksam","");
        assertion.put("koleston/cal/mesai","");
        assertion.put("cal/calar","");
        assertion.put("aksam/mp3","");
        assertion.put("mesgul","");
        assertion.put("piranha/tip","");
        assertion.put("yapi-kredi-bankasi/siparis","");
        assertion.put("yapi-kredi-bankasi/tip","");
        assertion.put("weblepi-ajans/mp3","");
        assertion.put("cal","");
        assertion.put("koleston/aksam/calar","");
        assertion.put("yapi-kredi-bankasi/aksam/tip","");
        assertion.put("mesgul/mp3","");
        assertion.put("weblepi-ajans","");
        assertion.put("weblepi-ajans/aksam/mp3","");
        assertion.put("koleston/calar","");
        assertion.put("koleston","");
        assertion.put("calar","");
        assertion.put("mesai/mesgul","");
        assertion.put("aksam/mesgul","");
        assertion.put("yapi-kredi-bankasi/piranha","");
        assertion.put("yapi-kredi-bankasi/aksam/piranha","");
        assertion.put("weblepi-ajans/mesgul/mp3","");
        assertion.put("calar/mesai","");
        assertion.put("mesai","");
        assertion.put("mesai/mp3","");
        assertion.put("weblepi-ajans/aksam","");
        assertion.put("mp3","");
        assertion.put("koleston/cal","");
        assertion.put("weblepi-ajans/mesgul","");
        assertion.put("yapi-kredi-bankasi","");
        assertion.put("yapi-kredi-bankasi/piranha/siparis","");
        assertion.put("weblepi-ajans/mesai","");
        assertion.put("koleston/calar/mesai","");
        assertion.put("yapi-kredi-bankasi/aksam","");
        assertion.put("aksam","");
        assertion.put("aksam/mesai","");
        if(Configuration.MAX_NUMBER_OF_HASHTAGS_IN_COMBINATION == 3){
            assertion.put("koleston/aksam/cal/calar","");
            assertion.put("koleston/aksam/calar/mesai","");
            assertion.put("koleston/cal/calar/mesai","");
            assertion.put("koleston/aksam/cal/mesai","");
            assertion.put("weblepi-ajans/aksam/mesai/mesgul","");
            assertion.put("weblepi-ajans/aksam/mesai/mp3","");
            assertion.put("weblepi-ajans/mesai/mesgul/mp3","");
            assertion.put("weblepi-ajans/aksam/mesgul/mp3","");
            assertion.put("yapi-kredi-bankasi/aksam/piranha/siparis","");
            assertion.put("yapi-kredi-bankasi/aksam/siparis/tip","");
            assertion.put("yapi-kredi-bankasi/aksam/piranha/tip","");
            assertion.put("yapi-kredi-bankasi/piranha/siparis/tip","");
            assertion.put("aksam/cal/calar","");
            assertion.put("aksam/cal/mesai","");
            assertion.put("aksam/calar/mesai","");
            assertion.put("cal/calar/mesai","");
            assertion.put("aksam/mesai/mesgul","");
            assertion.put("aksam/mesai/mp3","");
            assertion.put("mesai/mesgul/mp3","");
            assertion.put("aksam/mesgul/mp3","");
            assertion.put("aksam/piranha/siparis","");
            assertion.put("aksam/siparis/tip","");
            assertion.put("piranha/siparis/tip","");
            assertion.put("aksam/piranha/tip","");
        }
        //endregion
        assert  controller.getCompanyHashtagURLs().keySet().equals(assertion.keySet());
    }

//    @Test
//    public void test2ThresholdMinComplaintOutput() throws Exception{
//
//        assert  controller.getCompanyHashtagURLs().size() == 59;
//    }

}
