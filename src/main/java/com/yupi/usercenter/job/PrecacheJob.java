package com.yupi.usercenter.job;

import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 预热缓存
 */

@Component
@Slf4j
public class PrecacheJob {

    @Resource
    UserService userService;

    @Resource
    RedissonClient redissonClient;

    List<User> activeUserList;

    //
    @PostConstruct
    public void init() {
        activeUserList = Arrays.asList(userService.getById(3L));
    }

    // 表达式解释网站：https://cron.ciding.cc/
    @Scheduled(cron = "0 50 16 * * ? ")
    public void doCacheRecommendUsers() {
        // 因为是需要登录的用户的首页加载进行预热，
        // 这里不可能把所有用户都预热
        // 选择一些活跃的用户进行预热即可
        RLock lock = redissonClient.getLock("mysoul:precachejob:docache:lock");
        try {
            // 只有一台服务器的一个线程可以抢到，抢不到不等待
            // 不设置 leaseTime（租约时间）就会开启看门狗机制，即业务没执行完，锁到期，会自动续期，使用看门狗机制需要手动释放锁
            // 设置 leaseTime 到期自动释放锁，不会续期, 也即不会开启看门狗机制
            // https://blog.csdn.net/weixin_51146329/article/details/129612350
            if (lock.tryLock(0L, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock: " + Thread.currentThread().getId());

                for (User user : activeUserList) {
                    userService.recommendUsers(8, 2, user);
                }
            }
        } catch (InterruptedException e) {
            log.error("tryLock error", e);
        } finally {
            // 释放锁必须写在finally中
            // 因为当在执行业务逻辑时，如果突然服务器挂了，锁就会一直锁上
            // 不过看门狗机制有宕机处理，当程序异常时，不会再续期

            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Scheduled(cron = "0 0 4 * * ? ")
    public void doCacheMatchUsers() {
        RLock lock = redissonClient.getLock("mysoul:precache:match:lock");
        try {
            if (lock.tryLock(0L, TimeUnit.MILLISECONDS)) {
                for (User user : activeUserList) {
                    userService.matchUsers(10, user);
                }
            }
        } catch (InterruptedException e) {
            log.error("tryLock error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
