package com.sikayetvar.sitemap.generator.dao;

import com.sikayetvar.sitemap.generator.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Date;

/**
 * Created by deniz on 3/16/17.
 */
public class RedisDataOperator {

    private static RedisDataOperator instance = null;
    private static final Logger logger = LoggerFactory.getLogger(RedisDataOperator.class);
    private static JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "luna.sikayetvar.com");
    private static Jedis jedis = jedisPool.getResource();

    protected RedisDataOperator() {
    }

    public static RedisDataOperator getInstance() {
        if (instance == null) {
            instance = new RedisDataOperator();
            jedis.select(2);
        }
        return instance;
    }

    public Date getPublishTime(Integer complaintId) {
        logger.info("going to Redis!");
        Date retval =  Utils.getInstance().toIstDate(jedis.hget("svComplaintDetail:" + complaintId, "redis_update_time"));
        logger.info("coming from Redis!");
        return retval;
    }
}
