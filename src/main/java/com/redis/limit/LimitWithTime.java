package com.redis.limit;

import com.redis.util.JedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * 简单限流，利用时间窗口，zset数据结构实现
 */
public class LimitWithTime {

    private Jedis jedis;

    public LimitWithTime(Jedis jedis) {
        this.jedis = jedis;
    }

    public boolean isActionAllowed(String userId, String actionKey,
                                   int period, int maxCount) throws Exception{

        String key = String.format("cls:limit:time:%s:%s", userId, actionKey);
        long nowTime = System.currentTimeMillis();

        Pipeline pipeline = jedis.pipelined();
        //事务，能保证一系列指令的原子顺序执行
        pipeline.multi();
        //zadd第一个参数是key，第二个参数是score，第三个参数是value，value要保证唯一性
        pipeline.zadd(key, System.currentTimeMillis(), ""+System.currentTimeMillis());
        pipeline.zremrangeByScore(key, 0, nowTime - period * 1000L);
        Response<Long> resps = pipeline.zcard(key);
        pipeline.expire(key, period+1);
        pipeline.exec();
        pipeline.close();
        return resps.get() <= maxCount;
    }

    public static void main(String[] args) throws Exception{
        LimitWithTime limit = new LimitWithTime(JedisUtil.getJedis());
        for(int i=0; i<20; i++) {
            System.out.println(limit.isActionAllowed("l-test", "sign", 10, 5));
        }
    }
}
