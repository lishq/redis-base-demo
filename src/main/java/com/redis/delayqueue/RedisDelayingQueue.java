package com.redis.delayqueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.redis.util.JedisUtil;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 延迟队列的实现
 */
public class RedisDelayingQueue<T> {

    private String queueKey  ;

    private Jedis jedis;

    // fastjson 序列化对象中存在 generic 类型时，需要使用 TypeReference
    private Type TaskType = new TypeReference<TaskItem<T>>() {
    }.getType();

    public RedisDelayingQueue(Jedis jedis, String queueKey) {
        this.queueKey   = queueKey;
        this.jedis = jedis;
    }

    public void delay(T msg, int seconds) {
        TaskItem<T> taskItem = new TaskItem<T>();
        taskItem.id = UUID.randomUUID().toString();// 分配唯一的 uuid
        taskItem.msg = msg;
        String value = JSON.toJSONString(taskItem);// fastjson 序列化
        jedis.zadd(queueKey, System.currentTimeMillis()+(seconds * 1000L), value);// 塞入延时队列 ,5s 后再处理
        System.out.println(String.format("produce msg: %s, delay time: %d", value, seconds));
    }

    public void loop() {
        while (!Thread.interrupted()) {
            try{
                // 只取一条
                Set<String> values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(), 0, 1);
                if(values.isEmpty()) {
                    Thread.sleep(500L);// 歇会继续
                    continue;
                }

                String value = values.iterator().next();
                if(jedis.zrem(queueKey, value) > 0) {// 抢到了
                    TaskItem<T> taskItem = JSON.parseObject(value, TaskType);
                    this.handleMsg(taskItem.msg);
                }
            }catch (InterruptedException  e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void handleMsg(T msg) {
        System.out.println(msg);
    }

    public static void main(String[] args) throws Exception{
        Jedis jedis = JedisUtil.getJedis();

        RedisDelayingQueue<String> RedisDelayingQueue = new RedisDelayingQueue(jedis, "l-demo");
        new Thread(() -> {
            Random random = new Random();
            for(int i=0; i<10; i++) {
                RedisDelayingQueue.delay("codehole:"+i, random.nextInt(5));
            }
        }).start();

        new Thread(() -> {
            RedisDelayingQueue.loop();
        }).start();

        while(true) {
            Thread.sleep(1000L);
        }
    }

    static class TaskItem<T> {

        public String id;

        public T msg;
    }

}
