package com.basebackend.album.service;

import com.basebackend.album.dto.CreateShareDTO;
import com.basebackend.album.dto.ShareAccessDTO;
import com.basebackend.album.vo.AlbumVO;
import com.basebackend.album.vo.ShareLinkVO;

import java.util.List;

/**
 * 分享服务接口
 *
 * @author BearTeam
 */
public interface ShareService {

    /** 创建分享链接 */
    ShareLinkVO createShare(CreateShareDTO dto, Long userId);

    /** 我的分享列表 */
    List<ShareLinkVO> myShares(Long userId);

    /** 取消分享 */
    void deleteShare(Long shareId, Long userId);

    /** 访问分享（校验密码、过期、次数限制） */
    AlbumVO accessShare(String shareCode, ShareAccessDTO dto);
}
