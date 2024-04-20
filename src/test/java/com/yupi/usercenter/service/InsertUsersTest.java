package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUsersTest {

    @Resource
    private UserService userService;

    private ExecutorService executorService = new ThreadPoolExecutor(60, 100, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * for 循环批量插入用户
     */

    @Test
    void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("原神启动");
            user.setUserAccount("12345");
            user.setUserPassword("12345678");
            user.setAvatarUrl("https://cdn.acwing.com/media/user/profile/photo/189084_lg_ebf3381682.jpg");
            user.setGender(0);
            user.setPhone("123");
            user.setEmail("12312@121");
            user.setTags("[]");
            user.setUserProfile("玩原神玩的");
            userList.add(user);
        }
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发批量插入用户
     */
    @Test
    void doConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 插入 100000 条数据
        // 分 10 组
        final int INSERT_NUM = 100000;
        int base = 2500;
        int j = 0;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            List<User> userList = new ArrayList<>();

            while (true) {
                j++;

                User user = new User();
                user.setUsername("原神启动" + j);
                user.setUserAccount("12345");
                user.setUserPassword("12345678");
                user.setAvatarUrl("https://cdn.acwing.com/media/user/profile/photo/189084_lg_ebf3381682.jpg");
                user.setGender(0);
                user.setPhone("123");
                user.setEmail("12312@121");
                user.setTags("[]");
                user.setUserProfile("玩原神玩的");
                userList.add(user);

                if (j % base == 0) {
                    break;
                }
            }

            // 异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, base);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());

        // 经测试 10 万条数据 分20组 4622
        // 分10组 5163
        // 分 40组 4789
        // 分 25 组 4766

        // ---- 使用自定义线程池
        // 25 组 5565
        // 10 组 6000
        // 40 组 5060
    }
}
