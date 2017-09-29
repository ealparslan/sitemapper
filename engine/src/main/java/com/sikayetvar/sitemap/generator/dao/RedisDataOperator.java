package com.sikayetvar.sitemap.generator.dao;

import com.sikayetvar.sitemap.generator.Configuration;
import com.sikayetvar.sitemap.generator.Utils;
import com.sun.corba.se.spi.ior.ObjectKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;

import java.util.*;

/**
 * Created by deniz on 3/16/17.
 */
public class RedisDataOperator {

    private static RedisDataOperator instance = null;
    private static final Logger logger = LoggerFactory.getLogger(RedisDataOperator.class);
    private static JedisPool complaintDetailPool = new JedisPool(new JedisPoolConfig(), Configuration.REDIS_COMPLAINT_DETAIL_HOST, Configuration.REDIS_COMPLAINT_DETAIL_PORT);
    private static Jedis complaintDetailRedis = complaintDetailPool.getResource();

    private static Set<String> keys = new HashSet<>();
    private HashMap<Integer,Date> complaintLastUpdates = new HashMap<>();


    protected RedisDataOperator() {
    }

    public static RedisDataOperator getInstance() {
        if (instance == null) {
            instance = new RedisDataOperator();
            complaintDetailRedis.select(0);
            keys = complaintDetailRedis.keys("svComplaintDetail:921*");
        }
        return instance;
    }



    public HashMap<Integer,Date> getComplaintLastUpdates() {
        for (String key:keys) {
            int complaintId = Integer.valueOf(key.split(":")[1]);
            complaintLastUpdates.put(complaintId,Utils.getInstance().toIstDate(complaintDetailRedis.hget("svComplaintDetail:" + complaintId, "redis_update_time")));
        }
        return complaintLastUpdates;
    }

}
