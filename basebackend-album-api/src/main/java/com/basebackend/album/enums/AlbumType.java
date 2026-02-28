package com.basebackend.album.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 相册类型枚举
 *
 * @author BearTeam
 */
@Getter
@AllArgsConstructor
public enum AlbumType {

    /** 普通相册 */
    NORMAL(0, "普通"),
    /** 时间轴自动相册 */
    TIMELINE(1, "时间轴"),
    /** 智能相册 */
    SMART(2, "智能");

    private final int code;
    private final String description;
}
