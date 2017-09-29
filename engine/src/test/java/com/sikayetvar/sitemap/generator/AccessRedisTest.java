package com.sikayetvar.sitemap.generator;

import com.sikayetvar.sitemap.generator.dao.RedisDataOperator;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;

public class AccessRedisTest {

    @Test
    public void test1AccessRedis() throws Exception{
        RedisDataOperator redisDataOperator = RedisDataOperator.getInstance();
        HashMap<Integer,Date> list = redisDataOperator.getComplaintLastUpdates();
        //assert (redisDataOperator.getPublishTimes(9535671).toString().equals("Mon Sep 25 04:44:01 PDT 2017"));
    }
}
