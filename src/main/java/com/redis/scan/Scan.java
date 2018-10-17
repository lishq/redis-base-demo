package com.redis.scan;

import com.redis.util.JedisUtil;
import redis.clients.jedis.Jedis;

public class Scan {

    private Jedis jedis;

    public Scan(Jedis jedis) {
        this.jedis = jedis;
    }

    public static void main(String[] args) {
        Jedis jedis = JedisUtil.getJedis();
        for(int i=0; i<10000; i++) {
            jedis.setex("key"+i, 60 * 60, i+"");
        }
    }

}
