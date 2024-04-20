package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.mapper.UserMapper;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.vo.UserVO;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.utils.AlogrithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.yupi.usercenter.constant.UserConstant.*;

/**
* @author helloworld
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2023-11-29 16:56:51
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    /**
     * 正则表达式
     */
    private static final String PATTERN = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        /**
         * 三个有任何一个为空则返回
         * 该类是 maven 引入的
         *
         */
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账户小于4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码小于8位");
        }

        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }

        // 两次密码不一致返回
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
        
        Matcher matcher = Pattern.compile(PATTERN).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能包含特殊字符");
        }
        // 向数据库查找账户，不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户已存在");
        }

        // 向数据库查看有没有该星球编号
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.USER_ERROR, "用户已存在");
        }

        // 对密码进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 向数据库插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.USER_ERROR, "账户注册失败");
        }

        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户或密码为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户长度不足4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度不足8位");
        }

        Matcher matcher = Pattern.compile(PATTERN).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能包含特殊字符");
        }
        // 对密码进行加密，盐值要相同
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 查询账户是否存在
        // 这里有一个问题就是逻辑删除的账号也会被查出来，我们需要过滤掉
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            // 用户不存在记录一些日志，使用@Slf4j 注解，由Lombok提供
            log.info("user login failed, userAccount Cannot match userPassword");
            throw new BusinessException(ErrorCode.NULL_ERROR, "账户不存在");
        }

        // 用户脱敏
        User safetyUser = getSafetyUser(user);

        // 记录用户的登录态, 需要使用httpServletRequest
        // session 可以当成 map 使用
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "脱敏用户为空");
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setUserProfile(originUser.getUserProfile());
        return safetyUser;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     * 只要有一个标签匹配就可以
     * 使用分页查询加快速度
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     */
    @Override
    public Page<User> searchUserByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        for (String tagName : tagNameList) {
            userLambdaQueryWrapper.or().like(StringUtils.isNotEmpty(tagName), User::getTags, tagName);
        }
        return page(new Page<>(PAGE_NUM, PAGE_SIZE), userLambdaQueryWrapper);
    }

    /**
     * 使用内存查询
     * @param tagNameList
     * @return
     */
    @Deprecated
    public List<User> searchUserByTagsByMemory(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询标签为空");
        }

        // 第二种方式：使用内存查询
        // 查出所有的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        // 遍历所有用户的tags
        // 由于用户的tags是json字符串，使用库gson进行解析，通过maven 引入
        Gson gson = new Gson();
        return userList.stream().filter(
                user -> {
                    String userTags = user.getTags();
//                    if (StringUtils.isBlank(userTags)) {
//                        return false;
//                    }
                    Set<String> tempTagNameList = gson.fromJson(userTags, new TypeToken<HashSet<String>>(){}.getType());
                    tempTagNameList = Optional.ofNullable(tempTagNameList).orElse(new HashSet<>());
                    for (String tag : tagNameList) {
                        if (!tempTagNameList.contains(tag)) {
                            return false;
                        }
                    }
                    return true;
                }
        ).map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public int updateUser(User user, HttpServletRequest request) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = getLoginUser(request);

        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 如果更新标签则删除对应的缓存
        if (StringUtils.isNotEmpty(user.getTags())) {
            String userKey = String.format(USER_MATCH_KEY, userId);
            redisTemplate.delete(userKey);
        }

        int result = userMapper.updateById(user);
        // 如果登录的用户是自己且修改了标签，则同时更新登录的缓存
        if (userId == loginUser.getId() && StringUtils.isNotEmpty(user.getTags())) {
            loginUser = this.getById(userId);
            User safetyUser = getSafetyUser(loginUser);
            request.getSession().removeAttribute(USER_LOGIN_STATE);
            request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        }


        return result;
    }

    /**
     * 获取当前登录的用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null || user.getUserRole() != ADMIN_ROLE) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAdmin(User user) {
        if (user == null || user.getUserRole() != ADMIN_ROLE) {
            return false;
        }
        return true;
    }

    @Override
    public Page<User> recommendUsers(long pageSize, long pageNum, User user) {
        // 先查缓存，缓存有直接返回，没有再向数据库查, 查完之后设置缓存

        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String userKey = String.format("mysoul:user:%s", user.getId());
        Page<User> userPage = (Page<User>) valueOperations.get(userKey);
        if (userPage != null) {
            return userPage;
        }

        // 向数据库查
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        List<User> userList = userPage.getRecords();
        userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
        userPage.setRecords(userList);

        // 设置缓存
        // 过期时间 1小时
        try {
            valueOperations.set(userKey, userPage, 1L, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return userPage;
    }

    @Override
    public List<UserVO> matchUsers(long num, User loginUser) {
        if (num < 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 先查缓存
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String userKey = String.format(USER_MATCH_KEY, loginUser.getId());
        List<UserVO> userVOList = (List<UserVO>) valueOperations.get(userKey);
        if (userVOList != null) {
            return userVOList;
        }

        // 向数据库查出所有的用户
        // 这里可以选择部分字段查询，但是最后我们是要用户的信息，
        // 不如一次性拿到在过滤, 实现发现，全量查数据库很慢
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");

        List<User> userList = this.list(queryWrapper);

        // 拿到当前登录用户的 tags （注意 json 转 list）
        // "[\"男\", \"python\"]" -> List<String>
        String loginUserTags = loginUser.getTags();

        Gson gson = new Gson();
        Type listType = new TypeToken<List<String>>() {}.getType();
        List<String> loginUserTagsList = gson.fromJson(loginUserTags, listType);

        // 把 用户 和 编辑距离 搞成一个 pair
        List<Pair<User, Integer>> userListPair = new ArrayList<>();

        // 遍历每个 user 和 当前登录的用户的tags 做一次编辑距离
        for (User user : userList) {
            String userTags = user.getTags();
            // 排除掉 标签为空 或 自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }

            // string 转 list
            List<String> userTagsList = gson.fromJson(userTags, listType);

            // 执行编辑距离
            int instance = AlogrithmUtils.minDistanceTagList(loginUserTagsList, userTagsList);

            userListPair.add(new Pair<>(user, instance));
        }

        // 对 list 按 instance 进行升序排序
        List<Pair<User, Integer>> pairList = userListPair.stream().sorted((a, b) -> a.getValue() - b.getValue()).limit(num).collect(Collectors.toList());


        List<Long> userIdList = pairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());

        queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", userIdList);
        Map<Long, List<User>> userIdMap = this.list(queryWrapper).stream().collect(Collectors.groupingBy(User::getId));

        List<UserVO> result = new ArrayList<>();
        for (Long id : userIdList) {
            UserVO userVO = new UserVO();
            User user = userIdMap.get(id).get(0);
            BeanUtils.copyProperties(user, userVO);
            result.add(userVO);
        }

        // 设置缓存
        try {
            valueOperations.set(userKey, result, 1L, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }

        return result;
    }


    /**
     * sql 查询 该方法已过时
     * @param tagNameList
     * @return
     */
    @Deprecated
    public List<User> searchUserByTagsBySql(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查找标签为空");
        }

        // 拼接所有的标签，例如：'like '%java%' and like '%C++%''
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tag : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tag);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }
}
