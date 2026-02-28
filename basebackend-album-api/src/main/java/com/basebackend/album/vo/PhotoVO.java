package com.basebackend.album.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 照片响应 VO
 *
 * @param id            照片ID
 * @param albumId       所属相册ID
 * @param fileName      原始文件名
 * @param fileUrl       文件访问URL
 * @param thumbnailUrl  缩略图URL
 * @param fileSize      文件大小(字节)
 * @param mimeType      MIME类型
 * @param width         宽度(px)
 * @param height        高度(px)
 * @param mediaType     媒体类型: 0=照片 1=视频
 * @param duration      视频时长(秒)
 * @param takenAt       拍摄时间
 * @param location      拍摄地点
 * @param latitude      纬度
 * @param longitude     经度
 * @param description   描述
 * @param tags          标签
 * @param likeCount     点赞数
 * @param commentCount  评论数
 * @param liked         当前用户是否已点赞
 * @param ownerName     上传者名称
 * @param createTime    上传时间
 * @author BearTeam
 */
public record PhotoVO(
        Long id,
        Long albumId,
        String fileName,
        String fileUrl,
        String thumbnailUrl,
        Long fileSize,
        String mimeType,
        Integer width,
        Integer height,
        Integer mediaType,
        Integer duration,
        LocalDateTime takenAt,
        String location,
        BigDecimal latitude,
        BigDecimal longitude,
        String description,
        String tags,
        Integer likeCount,
        Integer commentCount,
        Boolean liked,
        String ownerName,
        LocalDateTime createTime
) {
}
