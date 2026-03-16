package com.basebackend.album.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 媒体类型枚举
 *
 * @author BearTeam
 */
@Getter
@AllArgsConstructor
public enum MediaType {

    /** 照片 */
    PHOTO(0, "照片"),
    /** 视频 */
    VIDEO(1, "视频");

    private final int code;
    private final String description;
}
