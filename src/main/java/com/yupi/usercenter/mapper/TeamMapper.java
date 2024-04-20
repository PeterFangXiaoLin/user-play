package com.yupi.usercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yupi.usercenter.model.domain.Team;
import com.yupi.usercenter.model.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author helloworld
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2024-03-21 19:48:25
* @Entity com.yupi.usercenter.model.domain.Team
*/
@Mapper
public interface TeamMapper extends BaseMapper<Team> {
    List<UserVO> selectTeamUserList(@Param("teamId")Long teamId);

}




