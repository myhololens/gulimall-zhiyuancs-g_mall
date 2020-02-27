package com.neu.gmall.order.service.impl;

import com.neu.gmall.bean.Constants;
import com.neu.gmall.service.OrderService;
import com.neu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

public class OrderServiceImpl implements OrderService {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = Constants.user + memberId +Constants.tradeCode;
            String code = UUID.randomUUID().toString();
            jedis.setex(tradeKey,60*15,code);
            return code;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        return null;
    }

    @Override
    public boolean checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try{
            jedis = redisUtil.getJedis();
            String key = Constants.user + memberId + Constants.tradeCode;
            //String code = jedis.get(key);
            // 使用lua脚本在发现key的同时将key删除，防止并发订单攻击,产生多个订单
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(key), Collections.singletonList(tradeCode));
            if(eval!=null && eval!=0){

                //jedis.del(key);
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        return false;
    }
}