package com.basebackend.user.service;

import com.basebackend.user.dto.ApplicationDTO;

import java.util.List;

/**
 * 应用管理Service接口
 */
public interface ApplicationService {

    /**
     * 查询应用列表
     *
     * @return 应用列表
     */
    List<ApplicationDTO> listApplications();

    /**
     * 查询所有启用的应用
     *
     * @return 应用列表
     */
    List<ApplicationDTO> listEnabledApplications();

    /**
     * 根据ID查询应用
     *
     * @param id 应用ID
     * @return 应用信息
     */
    ApplicationDTO getApplicationById(Long id);

    /**
     * 根据应用编码查询应用
     *
     * @param appCode 应用编码
     * @return 应用信息
     */
    ApplicationDTO getApplicationByCode(String appCode);

    /**
     * 创建应用
     *
     * @param dto 应用信息
     * @return 是否成功
     */
    boolean createApplication(ApplicationDTO dto);

    /**
     * 更新应用
     *
     * @param dto 应用信息
     * @return 是否成功
     */
    boolean updateApplication(ApplicationDTO dto);

    /**
     * 删除应用
     *
     * @param id 应用ID
     * @return 是否成功
     */
    boolean deleteApplication(Long id);

    /**
     * 启用/禁用应用
     *
     * @param id     应用ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, Integer status);
}
