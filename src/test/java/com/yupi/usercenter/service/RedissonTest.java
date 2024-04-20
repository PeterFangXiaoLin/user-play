package com.yupi.usercenter.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void doRedissonTest() {
        // java list
        List<Integer> list = new ArrayList<>();
        list.add(1);
        System.out.println(list.get(0));
        list.remove(0);

        // redisson list
        RList<Integer> rList = redissonClient.getList("test-rlist");
        rList.add(1);
        System.out.println(rList.get(0));
        rList.remove(0);


        // java map
        Map<String, Integer> map = new HashMap<>();
        map.put("高手", 1);
        System.out.println("map: " + map.get("高手"));

        // redisson map
        RMap<String, Integer> rMap = redissonClient.getMap("test-rMap");
        rMap.put("高手", 1);
        System.out.println("rMap: " + rMap.get("高手"));

        // java set

        // java stack
    }
}
