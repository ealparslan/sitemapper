package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.entity.ComplaintHashtag;
import com.sikayetvar.sitemap.generator.entity.URLMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Try {

    public static void main(String[] args){





//        Utils.getInstance().permute(Arrays.asList("ali","veli","deli"), 0);
//
//        Utils.getInstance().permutations.stream().forEach(entry -> System.out.print(entry));
//
//        Utils.getInstance().reset();
//
//        Utils.getInstance().permute(Arrays.asList("ddd","sss","ggg"), 0);
//
//        Utils.getInstance().permutations.stream().forEach(entry -> System.out.print(entry));




//        ConcurrentHashMap<String, URLMeta> denemekaydi = new ConcurrentHashMap<>();
//
//        Set<String> words = new HashSet<>();
//        words.add("vestel");
//        words.add("buzdolabi");
//        words.add("kapi");
//
//
//        denemekaydi.put("/vestel/buzdolabi/kapi", new URLMeta(new Date(),5,words,1,1));
//
//        words = new HashSet<>();
//        words.add("vestel");
//        words.add("sogutma");
//        words.add("kapi");
//
//
//        denemekaydi.put("/vestel/sogutma/kapi", new URLMeta(new Date(),5,words,1,1));
//
//
//        Utils.getInstance().stashInElastic(denemekaydi);




        List<String> hashtags = new ArrayList<>();
        hashtags.add("telefon");
        hashtags.add("hat");

        for (int i = 0; i < 100 ; i++) {
            hashtags.add(String.valueOf(i));
        }

        Controller controller = new Controller();


        ComplaintHashtag complaintHashtag = new ComplaintHashtag();
        complaintHashtag.setCompany("vodafone");
        complaintHashtag.setPublish_time(new Date());
        complaintHashtag.setUpdate_time(new Date());
        complaintHashtag.setComplaint_id(111);
        complaintHashtag.setHashtags(hashtags);


        Date update_time = complaintHashtag.getUpdate_time();


        // we have companyURLs from previous fillCompanies step. But we have to adjust mostUptoDate variable due to the last complaint of the company

        // foreach hashtag combination of a complaint
        for (SortedSet<String> combination:complaintHashtag.getCombinations()) {
            if(combination.isEmpty()) continue;
            // hashtag1 + hashtag2 .....
            controller.enrichCompanyHashtagURLs(combination, null , update_time);
            // brand + hashtag1 + hashtag2 .....
            controller.enrichCompanyHashtagURLs(combination,complaintHashtag.getCompany(), update_time);

        }

        Map<String, URLMeta> hashMap = new HashMap<>(controller.companyHashtagURLs);


        controller.serializeObjects("./company_hashtag_urls.ser",controller.companyHashtagURLs);


    }

}
