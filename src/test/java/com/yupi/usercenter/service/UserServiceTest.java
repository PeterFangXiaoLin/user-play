package com.yupi.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.usercenter.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * UserService测试
 */

@SpringBootTest
class UserServiceTest {
    @Resource
    private UserService userService;

    @Test
    void test() {
        User user = new User();
        user.setUsername("yupi");
        user.setUserAccount("123");
        user.setUserPassword("456");
        user.setAvatarUrl("");
        user.setGender(0);
        user.setPhone("1234");
        user.setEmail("1234");
        boolean result = userService.save(user);
        Assertions.assertTrue(result);
        // 这里可以拿到user的id，是因为插入成功之后，框架会帮助我们把id注入到user里面
        System.out.println(user.getId());
    }

    @Test
    void userRegister() {
        // 账户为空
        String userAccount = "";
        String userPassword = "yupi";
        String checkPassword = "sss";
        String planetCode = "1";
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 密码为空
        userAccount = "yupi";
        userPassword = "";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 校验密码为空
        userPassword = "123";
        checkPassword = "";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 检查账户长度
        userAccount = "yu";
        userPassword = "1234567";
        checkPassword = "1234567";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 检查密码长度
        userAccount = "yupi";
        userPassword = "123";
        checkPassword = "123";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 两次密码不一致检查
        userAccount = "yupi";
        userPassword = "12345678";
        checkPassword = "345676454";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 账户包含特殊字符
        userAccount = "yu pi";
        userPassword = "12345678";
        checkPassword = "12345678";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 账户重复
        userAccount = "1234";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);

        // 星球编号重复
        userAccount = "yupidog";
        result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        Assertions.assertEquals(-1, result);
    }

    @Test
    void testSearchUserByTags() {
        List<String> tagNameList = Arrays.asList("java", "python");
        Page<User> userList = userService.searchUserByTags(tagNameList);
        Assertions.assertNotNull(userList);
    }
}