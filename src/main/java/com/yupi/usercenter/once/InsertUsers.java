package com.yupi.usercenter.once;

import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class InsertUsers {
    @Resource
    private UserService userService;

    public void doInsertUsers() {
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
}
