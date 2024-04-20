package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Test
    void test() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        valueOperations.set("gaoshuo", "hao");
        valueOperations.set("1", 1);
        valueOperations.set("double", 2.0);
        User user = new User();
        user.setId(0L);
        user.setUsername("原神启动");
        valueOperations.set("玩原神玩的", user);

        // 查
        Object s = valueOperations.get("gaoshuo");
        Assertions.assertTrue("hao".equals(s));

        s = valueOperations.get("1");
        Assertions.assertTrue(1 == (Integer) s);
        s = valueOperations.get("double");
        Assertions.assertTrue(2.0 == (Double) s);
        System.out.println(valueOperations.get("原神启动"));

        valueOperations.set("hello", "world");
        // 删
        // redisTemplate.delete();

    }
}
