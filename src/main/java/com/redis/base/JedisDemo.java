package com.redis.base;

import com.redis.util.JedisUtil;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import java.util.*;

public class JedisDemo {
	
	private Jedis jedis;

    @Before
    public void getJedis() {
    	jedis = JedisUtil.getJedis();
    }

    /**
     * Redis操作字符串
     */
    @Test
    public void testString() {
        //添加数据
        jedis.set("name", "lishq"); 
        System.out.println("拼接前:" + jedis.get("name"));

        //向key为name的值后面加上数据 ---拼接
        jedis.append("name", " is my name;");
        System.out.println("拼接后:" + jedis.get("name"));

        //删除某个键值对
        jedis.del("name");
        System.out.println("删除后:" + jedis.get("name"));

        //设置多个键值对
        jedis.mset("name", "lishq", "age", "22", "email", "isqmail@163.com");
        jedis.incr("age");//用于将键的整数值递增1。如果键不存在，则在执行操作之前将其设置为0。 如果键包含错误类型的值或包含无法表示为整数的字符串，则会返回错误。此操作限于64位有符号整数。
        System.out.println(jedis.get("name") + " " + jedis.get("age") + " " + jedis.get("email"));
    }

    @Test
    public void testHash() {
        //添加数据
        Map<String, String> map = new HashMap<String, String>(32);
        map.put("name", "lishq");
        map.put("age", "100");
        map.put("email", "isqmail@163.com");
        jedis.hmset("user", map);
        //取出user中的name，结果是一个泛型的List
        //第一个参数是存入redis中map对象的key，后面跟的是放入map中的对象的key，后面的key是可变参数
        List<String> list = jedis.hmget("user", "name", "age", "email");
        System.out.println(list);

        //删除map中的某个键值
        jedis.hdel("user", "age");
        System.out.println("age:" + jedis.hmget("user", "age")); //因为删除了，所以返回的是null
        System.out.println("user的键中存放的值的个数:" + jedis.hlen("user")); //返回key为user的键中存放的值的个数2
        System.out.println("是否存在key为user的记录:" + jedis.exists("user"));//是否存在key为user的记录 返回true
        System.out.println("user对象中的所有key:" + jedis.hkeys("user"));//返回user对象中的所有key
        System.out.println("user对象中的所有value:" + jedis.hvals("user"));//返回map对象中的所有value

        //拿到key，再通过迭代器得到值
        Iterator<String> iterator = jedis.hkeys("user").iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            System.out.println(key + ":" + jedis.hmget("user", key));
        }
        jedis.del("user");
        System.out.println("删除后是否存在key为user的记录:" + jedis.exists("user"));//是否存在key为user的记录

    }

    /**
     * jedis操作List
     */
    @Test
    public void testList(){
        //移除javaFramwork所有内容
        jedis.del("javaFramwork");
        //存放数据
        jedis.lpush("javaFramework","spring");
        jedis.lpush("javaFramework","springMVC");
        jedis.lpush("javaFramework","springBoot");
        //取出所有数据,jedis.lrange是按范围取出
        //第一个是key，第二个是起始位置，第三个是结束位置
        System.out.println("长度:"+jedis.llen("javaFramework"));
        //jedis.llen获取长度，-1表示取得所有
        System.out.println("javaFramework:"+jedis.lrange("javaFramework",0,-1));

        jedis.del("javaFramework");
        System.out.println("删除后长度:"+jedis.llen("javaFramework"));
        System.out.println(jedis.lrange("javaFramework",0,-1));
    }

    /**
     * jedis操作Set
     */
    @Test
    public void testSet(){
        //添加
        jedis.sadd("company","baidu");
        jedis.sadd("company","alibaba");
        jedis.sadd("company","tencent");
        jedis.sadd("company","sina");
        jedis.sadd("company","Aliyun");
        jedis.sadd("company","NetEase");
        jedis.sadd("organization","apache");
        System.out.println("2个集合的差集:"+jedis.sdiff("company","organization"));
        System.out.println("2个集合的交集:"+jedis.sinter("company","organization"));
        System.out.println("2个集合的并集:"+jedis.sunion("company","organization"));
        System.out.println("2个集合的差集组成一个新的集合JPA:"+
                jedis.sdiffstore("JPA", "company","organization"));
        System.out.println("JPA的元素:"+jedis.smembers("JPA"));
        //移除company集合中的元素are
        jedis.srem("company","Aliyun");
        System.out.println("company中的value:"+jedis.smembers("company"));//获取所有加入company的value
        System.out.println("baidu是否是company中的元素:"+jedis.sismember("company","baidu"));
        System.out.println("集合中的一个随机元素:"+jedis.srandmember("company"));//返回集合中的一个随机元素
        System.out.println("company中元素的个数:"+jedis.scard("company"));
    }
    
    /**
     * jedis操作zset(sorted set)
     */
    @Test
    public void testZset(){
        Map<String, Double> scoreMembers = new HashMap<>();
        scoreMembers.put("Python",90d);
        scoreMembers.put("Javascript",80d);
        jedis.zadd("mysort", scoreMembers);
        jedis.zadd("mysort",100,"Java");//ZADD

        System.out.println("Number of Java users:" + jedis.zscore("mysort", "Java"));

        System.out.println("Number of elements:" + jedis.zcard("mysort"));//ZCARD
        
        System.out.println(jedis.zrange("mysort", 0, -1));

        System.out.println(jedis.zrevrange("mysort", 0, -1));
        Set<Tuple> elements = jedis.zrevrangeWithScores("mysort", 0, -1);
        for(Tuple tuple: elements){
           System.out.println(tuple.getElement() + "-" + tuple.getScore());
        }

        System.out.println("Score before zincrby:" + jedis.zscore("mysort", "Python"));
        jedis.zincrby("mysort", 1, "Python");
        System.out.println("Score after zincrby:" + jedis.zscore("mysort", "Python"));
    }

    /**
     * 排序
     */
    @Test
    public void test(){
        jedis.rpush("number","4");//将一个或多个值插入到列表的尾部(最右边)
        jedis.rpush("number","5");
        jedis.rpush("number","3");

        jedis.lpush("number","9");//将一个或多个值插入到列表头部
        jedis.lpush("number","1");
        jedis.lpush("number","2");
        System.out.println(jedis.lrange("number",0,jedis.llen("number")));
        System.out.println("排序:"+jedis.sort("number"));
        System.out.println(jedis.lrange("number",0,-1));//不改变原来的排序
        jedis.del("number");//测试完删除数据
    }

}
