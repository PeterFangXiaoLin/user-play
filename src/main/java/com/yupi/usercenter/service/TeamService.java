package com.yupi.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.dto.TeamQuery;
import com.yupi.usercenter.model.request.DeleteTeamRequest;
import com.yupi.usercenter.model.request.JoinTeamRequest;
import com.yupi.usercenter.model.request.QuitTeamRequest;
import com.yupi.usercenter.model.request.UpdateTeamRequest;
import com.yupi.usercenter.model.vo.TeamUserVO;

import java.util.List;

/**
* @author helloworld
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-03-21 19:48:25
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 查询队伍列表
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 修改队伍信息
     *
     * @param updateTeamRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(UpdateTeamRequest updateTeamRequest, User loginUser);

    /**
     * 加入队伍
     *
     * @param joinTeamRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser);

    /**
     * 退出队伍
     * @param quitTeamRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(QuitTeamRequest quitTeamRequest, User loginUser);

    /**
     * 删除（解散）队伍
     * @param deleteTeamRequest
     * @param loginUser
     * @return
     */
    boolean deleteTeam(DeleteTeamRequest deleteTeamRequest, User loginUser);
}
