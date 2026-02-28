package com.basebackend.album.service;

import com.basebackend.album.vo.TimelineVO;

import java.util.List;

/**
 * 时间轴服务接口
 *
 * @author BearTeam
 */
public interface TimelineService {

    /**
     * 按日期分组查询照片
     *
     * @param userId 当前用户ID
     * @param page   页码
     * @param size   每页大小（按日期分组数）
     * @return 时间轴列表
     */
    List<TimelineVO> getTimeline(Long userId, int page, int size);
}
