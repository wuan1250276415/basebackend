package com.basebackend.chat.enums;

import com.basebackend.common.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聊天模块错误码 (6000-6999)
 */
@Getter
@AllArgsConstructor
public enum ChatErrorCode implements ErrorCode {

    CONVERSATION_NOT_FOUND(6001, "会话不存在"),
    CONVERSATION_NO_PERMISSION(6002, "无权限操作该会话"),
    MESSAGE_SEND_FAILED(6003, "消息发送失败"),
    MESSAGE_REVOKE_TIMEOUT(6004, "消息已过撤回时限"),
    FRIEND_NOT_FOUND(6005, "好友关系不存在"),
    ALREADY_BLOCKED(6006, "已在黑名单中"),
    FRIEND_REQUEST_INVALID(6007, "好友申请不存在或已处理"),
    GROUP_NOT_FOUND(6010, "群组不存在"),
    NOT_GROUP_MEMBER(6011, "非群成员无权操作"),
    GROUP_MEMBER_FULL(6012, "群成员已满"),
    GROUP_PERMISSION_DENIED(6013, "权限不足（需群主/管理员）"),
    CANNOT_OPERATE_OWNER(6014, "不能对群主执行该操作"),
    GROUP_DISSOLVED(6015, "群已解散"),
    USER_MUTED(6020, "禁言中，无法发送消息"),
    FILE_UPLOAD_FAILED(6030, "文件上传失败"),
    SEARCH_UNAVAILABLE(6040, "搜索服务不可用");

    private final Integer code;
    private final String message;

    @Override
    public String getModule() {
        return "chat";
    }
}
