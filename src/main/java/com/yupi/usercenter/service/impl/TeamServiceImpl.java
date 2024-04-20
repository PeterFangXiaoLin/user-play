package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.mapper.TeamMapper;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.UserTeam;
import com.yupi.usercenter.model.dto.TeamQuery;
import com.yupi.usercenter.model.enums.TeamStatus;
import com.yupi.usercenter.model.request.DeleteTeamRequest;
import com.yupi.usercenter.model.request.JoinTeamRequest;
import com.yupi.usercenter.model.request.QuitTeamRequest;
import com.yupi.usercenter.model.request.UpdateTeamRequest;
import com.yupi.usercenter.model.vo.TeamUserVO;
import com.yupi.usercenter.model.vo.UserVO;
import com.yupi.usercenter.service.TeamService;
import com.yupi.usercenter.service.UserService;
import com.yupi.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
* @author helloworld
* @description 针对表【team(队伍)】的数据库操作Service实现
* @createDate 2024-03-21 19:48:25
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        // 1. 请求参是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "未登录");
        }

        // 3. 队伍人数 > 1 且 <= 20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数 > 1 且 <= 20");
        }
        // 4. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isNotBlank(name) && name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称过长");
        }
        // 5. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述太长");
        }
        // 6. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatus teamStatus = TeamStatus.getTeamStatus(status);
        if (teamStatus == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不合法");
        }
        // 7. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        if (TeamStatus.SECRET.equals(teamStatus)) {
            String password = team.getPassword();
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不合法");
            }
        }
        // 8. 超时时间  > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间  > 当前时间");
        }
        // 9. 校验用户最多创建 5 个队伍
        final long userId = loginUser.getId();
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamCount = this.count(queryWrapper);
        if (hasTeamCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每个用户最多创建 5 个队伍");
        }
        // 下面的两步操作是一个事务，原子性的操作，要么全部成功，要么回滚
        // 10. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建队伍失败");
        }
        // 11. 插入 用户 => 队伍关系 到 关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setId(null);
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户队伍关系创建失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();

        if (teamQuery != null) {
            Long teamId = teamQuery.getId();
            if (teamId != null && teamId > 0) {
                queryWrapper.eq("id", teamId);
            }

            // 根据队伍 id 列表查询
            List<Long> teamIdList = teamQuery.getTeamIdList();
            if (!CollectionUtils.isEmpty(teamIdList)) {
                queryWrapper.in("id", teamIdList);
            }

            // 根据用户输入的关键词进行查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }

            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }

            // 根据 队长 id 查询
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }

            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }

            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }

            // 根据状态查询
            Integer status = teamQuery.getStatus();
            TeamStatus teamStatus = TeamStatus.getTeamStatus(status);

            // 只有管理员才能看到私有的队伍
            if (!isAdmin && TeamStatus.PRIVATE.equals(teamStatus)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }

            // 用户没有设置查询状态或者查询状态为 null(非法)
            if (teamStatus == null) {
                // 是管理员，查公开的和加密的以及私有的
                // 不是管理员，就只查公开
                if (!isAdmin) {
                    teamStatus = TeamStatus.PUBLIC;
                    queryWrapper.eq("status", teamStatus.getValue());
                }
            } else {
                queryWrapper.eq("status", teamStatus.getValue());
            }
        }

        // 不展示已过期的队伍
        // expireTime is noll or expireTime > Now()
        queryWrapper.and(qw -> qw.isNull("expireTime").or().gt("expireTime", new Date()));

        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }

        // 遍历每个队伍，为每个队伍补充队长的信息, 以及成员的列表
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            // 用户 id 不能为空，且必须大于 0
            if (userId == null || userId <= 0) {
                continue;
            }

            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);

            // 关联查询队长的信息
            User user = userService.getById(userId);
            // 查出来的用户可能为空，因为用户可能被删除
            if (user != null) {
                // 脱敏用户信息
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreateUser(userVO);
            }

            // 关联查询队员的信息
            List<UserVO> userVOList = teamMapper.selectTeamUserList(team.getId());

            teamUserVO.setUserVOList(userVOList);

            teamUserVOList.add(teamUserVO);
        }

        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(UpdateTeamRequest updateTeamRequest, User loginUser) {
        if (updateTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long teamId = updateTeamRequest.getId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Team oldTeam = this.getById(teamId);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }

        if (!userService.isAdmin(loginUser) && oldTeam.getUserId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }

        TeamStatus oldTeamStatus = TeamStatus.getTeamStatus(oldTeam.getStatus());
        TeamStatus updateTeamStatus = TeamStatus.getTeamStatus(updateTeamRequest.getStatus());
        if (oldTeamStatus != updateTeamStatus) {
            if (!TeamStatus.SECRET.equals(updateTeamStatus)) {
                updateTeamRequest.setPassword(null);
            } else {
                String password = updateTeamRequest.getPassword();
                if (StringUtils.isBlank(password)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密队伍密码不能为空");
                }
            }
        }

        Team team = new Team();
        BeanUtils.copyProperties(updateTeamRequest, team);

        return this.updateById(team);
    }

    @Override
    public boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser) {
        if (joinTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 用户最多加入 5 个队伍
        // 获取登录的用户
        Long userId = loginUser.getId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询当前用户已加入的队伍数量
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasJoinNum = userTeamService.count(queryWrapper);
        if (hasJoinNum >= 5L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多加入 5 个队伍");
        }

        // 队伍必须存在，只能加入未满、未过期的队伍
        Long teamId = joinTeamRequest.getId();
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        // 禁止加入私有的队伍
        Integer status = team.getStatus();
        TeamStatus teamStatus = TeamStatus.getTeamStatus(status);
        if (TeamStatus.PRIVATE.equals(teamStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法加入私有队伍");
        }

        // 密码匹配才能加入
        if (TeamStatus.SECRET.equals(teamStatus)) {
            String password = team.getPassword();
            if (StringUtils.isNotBlank(password) && !password.equals(joinTeamRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不正确");
            }
        }

        // 加入队伍未满
        // 从这里开始就得加锁
        // 如果两个用户加入的是不同的队伍，那么就没必要加锁
        // 如果加入的是同一个队伍，这时就得加锁
        // 但是所有的加入队伍操作都是调用该方法
        // 如何实现把加入同一队伍的进行加锁，把加入不同的进行并发
        // 通过 Ai 得知，可以为 每个队伍增加一个唯一的锁标识
        // 假设有两个用户同时申请加入同一个队伍，且队伍仅剩一个位置，这时如何解决
        String lockKey = "mysoul:dojointeam:" + teamId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10L, TimeUnit.SECONDS)) {
                Integer maxNum = team.getMaxNum();
                queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("teamId", teamId);
                long teamCount = userTeamService.count(queryWrapper);
                if (teamCount == maxNum) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入的队伍已满");
                }

                // 不能重复加入已加入的队伍
                // 当用户快速点击加入队伍时，可能存在问题，可能一个队伍加入多次？
                queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userId", userId);
                queryWrapper.eq("teamId", teamId);
                long hasJoinTeamCount = userTeamService.count(queryWrapper);
                if (hasJoinTeamCount > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已在队伍中");
                }

                // 更新数据库记录
                // 如果更新的过程中，服务器宕机，那么还是加入失败
                UserTeam userTeam = new UserTeam();
                userTeam.setUserId(userId);
                userTeam.setTeamId(teamId);
                userTeam.setJoinTime(new Date());
                return userTeamService.save(userTeam);
            }
        } catch (InterruptedException e) {
            log.error("tryLock error", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(QuitTeamRequest quitTeamRequest, User loginUser) {
        // 校验请求参数
        if (quitTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long teamId = quitTeamRequest.getId();

        // 判断数据库中是否存在该队伍
        Team team = this.getTeamById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }


        // 向 用户 - 队伍关系表查询是否存在该用户的id
        UserTeam userTeam = new UserTeam();
        long userId = loginUser.getId();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>(userTeam);
        long count = userTeamService.count(userTeamQueryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未加入该队伍");
        }

        // 查询队伍的人数
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        long teamNum = userTeamService.count(userTeamQueryWrapper);
        if (teamNum <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        if (teamNum == 1) {
            // 解散队伍
            this.removeById(teamId);
        } else {
            // 队伍剩余人数 >= 2
            // 判断是否是队长
            if (team.getUserId() == userId) {
                // 转移队长
                userTeamQueryWrapper.last("order by id asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }

                // 修改为新的队长id
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(userTeamList.get(1).getUserId());
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 删除 用户 - 队伍 关系表的信息
        userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.remove(userTeamQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(DeleteTeamRequest deleteTeamRequest, User loginUser) {
        if (deleteTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long teamId = deleteTeamRequest.getId();

        // 向数据库查是否存在该队伍
        Team team = this.getTeamById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍不存在");
        }

        // 校验你是不是队伍的队长
        if (team.getUserId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是该队伍的队长");
        }

        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "移除队伍的关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }


    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        return this.getById(teamId);
    }
}




