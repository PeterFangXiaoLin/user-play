package com.yupi.usercenter.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author helloworld
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2023-11-29 16:56:51
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userAccount 账户
     * @param userPassword 密码
     * @param checkPassword 验证密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount 账户
     * @param userPassword 密码
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getSafetyUser(User originUser);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     */
    Page<User> searchUserByTags(List<String>tagNameList);

    /**
     * 修改用户信息
     *
     * @param user
     * @param request
     * @return
     */
    int updateUser(User user, HttpServletRequest request);


    /**
     * 获取当前登录的用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 判断当前用户是否为管理员
     * 根据请求里面的 session 来判断用户是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断当前用户是否为管理员
     * 根据 user的 userRole 来判断用户是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 根据用户 id 推荐用户
     * 使用 redis 缓存实现
     *
     * @param pageSize
     * @param pageNum
     * @param user
     * @return
     */
    Page<User> recommendUsers(long pageSize, long pageNum, User user);

    /**
     * 为每个用户推荐 num 个用户
     *
     * @param num
     * @param loginUser
     * @return
     */
    List<UserVO> matchUsers(long num, User loginUser);
}
