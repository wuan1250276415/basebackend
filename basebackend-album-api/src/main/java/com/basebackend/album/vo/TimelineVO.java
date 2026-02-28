package com.basebackend.album.vo;

import java.time.LocalDate;
import java.util.List;

/**
 * 时间轴响应 VO
 *
 * @param date   日期
 * @param count  当日照片数量
 * @param photos 照片列表
 * @author BearTeam
 */
public record TimelineVO(
        LocalDate date,
        Integer count,
        List<PhotoVO> photos
) {
}
