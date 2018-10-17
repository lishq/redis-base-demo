package com.redis.util;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class JedisUtil implements MethodInterceptor{

    private static final JedisPool jedisPool;

    private static final List<String> skipMethods = Arrays.asList("isConnected", "close");

    private static final Object[] sysConfig = {"ip", 6379,"auth"};

    final private static int TIMEOUT = 10000;

    static {

        JedisPoolConfig config = new JedisPoolConfig();//redis连接池配置对象
        config.setMaxTotal(32);//最大连接数
        config.setMaxIdle(6);//闲置最大连接数
        config.setMinIdle(0);//闲置最小连接数
        config.setMaxWaitMillis(15000);//到达最大连接数后，调用者阻塞时间
        config.setMinEvictableIdleTimeMillis(300000);//连接空闲的最小时间，可能被移除
        config.setSoftMinEvictableIdleTimeMillis(-1);//连接空闲的最小时间，多余最小闲置连接的将被移除
        config.setNumTestsPerEvictionRun(3);//设置每次检查闲置的个数
        config.setTestOnBorrow(false);//申请连接时，是否检查连接有效
        config.setTestOnReturn(false);//返回连接时，是否检查连接有效
        config.setTestWhileIdle(false);//空闲超时,是否执行检查有效
        config.setTimeBetweenEvictionRunsMillis(60000);//空闲检查时间
        config.setBlockWhenExhausted(true);//当连接数耗尽，是否阻塞

        jedisPool = new JedisPool(config, sysConfig[0].toString(), (int)sysConfig[1], TIMEOUT, sysConfig[2].toString());
    }

    public static Jedis ceateProxtJedis() {
        // 声明增加类实例
        Enhancer enhancer = new Enhancer();
        // 设置被代理类字节码，CGLIB根据字节码生成被代理类的子类
        enhancer.setSuperclass(Jedis.class);
        // 回调方法
        enhancer.setCallback(new JedisUtil());
        // 创建代理对象
        return (Jedis) enhancer.create();
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object o = null;
        Jedis jedis = null;
        // 注意参数object,仍然为外部声明的源对象，且Method为JDK的Method反射
        try {
            jedis = jedisPool.getResource();
            o = method.invoke(jedis, args);
        }finally {
            if(!skipMethods.contains(method.getName())) {
                jedis.close();
            }
        }
        return o;
    }

    public static Jedis getJedis() {
        return getJedis(false);
    }

    public static Jedis getJedis(boolean isSelfClose) {
        if(isSelfClose) {
            return jedisPool.getResource();
        }

        return ceateProxtJedis();
    }

    public static void main(String[] args) throws Exception{
        Jedis jedis = JedisUtil.getJedis();

        jedis.set("test", "test1");
        jedis.set("test", "test2");
        jedis.set("test", "test3");
        System.out.println(jedis.get("test"));

        System.out.println(jedis.expire("test", 3));
        System.out.println(jedis.expire("test123", 3));
        //jedis.set("test", "test3");
        //jedis.close();
        while(true) {
            Thread.sleep(1000L);
        }
    }

}
