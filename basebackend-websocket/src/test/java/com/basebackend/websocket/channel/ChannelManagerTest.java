package com.basebackend.websocket.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChannelManager 测试")
class ChannelManagerTest {

    private ChannelManager manager;

    @BeforeEach
    void setUp() {
        manager = new ChannelManager();
    }

    @Test
    @DisplayName("加入频道")
    void join() {
        manager.join("room1", "user1");
        assertThat(manager.isMember("room1", "user1")).isTrue();
        assertThat(manager.getMemberCount("room1")).isEqualTo(1);
    }

    @Test
    @DisplayName("多用户加入同一频道")
    void multipleUsersJoin() {
        manager.join("room1", "user1");
        manager.join("room1", "user2");
        manager.join("room1", "user3");
        assertThat(manager.getMemberCount("room1")).isEqualTo(3);
        assertThat(manager.getMembers("room1")).containsExactlyInAnyOrder("user1", "user2", "user3");
    }

    @Test
    @DisplayName("离开频道")
    void leave() {
        manager.join("room1", "user1");
        manager.leave("room1", "user1");
        assertThat(manager.isMember("room1", "user1")).isFalse();
        assertThat(manager.exists("room1")).isFalse(); // 最后一人离开，频道销毁
    }

    @Test
    @DisplayName("离开后频道仍有其他人")
    void leaveWithOthersRemaining() {
        manager.join("room1", "user1");
        manager.join("room1", "user2");
        manager.leave("room1", "user1");
        assertThat(manager.exists("room1")).isTrue();
        assertThat(manager.getMemberCount("room1")).isEqualTo(1);
    }

    @Test
    @DisplayName("leaveAll 离开所有频道")
    void leaveAll() {
        manager.join("room1", "user1");
        manager.join("room2", "user1");
        manager.join("room3", "user1");
        manager.leaveAll("user1");

        assertThat(manager.getUserChannels("user1")).isEmpty();
        assertThat(manager.isMember("room1", "user1")).isFalse();
        assertThat(manager.isMember("room2", "user1")).isFalse();
    }

    @Test
    @DisplayName("getUserChannels 获取用户加入的频道")
    void getUserChannels() {
        manager.join("room1", "user1");
        manager.join("room2", "user1");
        assertThat(manager.getUserChannels("user1")).containsExactlyInAnyOrder("room1", "room2");
    }

    @Test
    @DisplayName("不存在的频道返回空集合")
    void nonexistentChannel() {
        assertThat(manager.getMembers("unknown")).isEmpty();
        assertThat(manager.getMemberCount("unknown")).isZero();
        assertThat(manager.exists("unknown")).isFalse();
    }

    @Test
    @DisplayName("不存在的用户返回空频道列表")
    void nonexistentUser() {
        assertThat(manager.getUserChannels("unknown")).isEmpty();
    }

    @Test
    @DisplayName("isMember 判断")
    void isMember() {
        manager.join("room1", "user1");
        assertThat(manager.isMember("room1", "user1")).isTrue();
        assertThat(manager.isMember("room1", "user2")).isFalse();
        assertThat(manager.isMember("room2", "user1")).isFalse();
    }

    @Test
    @DisplayName("getAllChannelIds 获取所有频道")
    void getAllChannelIds() {
        manager.join("a", "u1");
        manager.join("b", "u2");
        manager.join("c", "u3");
        assertThat(manager.getAllChannelIds()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    @DisplayName("getChannelCount 统计频道数")
    void getChannelCount() {
        assertThat(manager.getChannelCount()).isZero();
        manager.join("a", "u1");
        manager.join("b", "u1");
        assertThat(manager.getChannelCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("getMembers 返回不可变副本")
    void membersImmutable() {
        manager.join("room1", "user1");
        assertThatThrownBy(() -> manager.getMembers("room1").add("hacker"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("重复加入不产生重复成员")
    void duplicateJoin() {
        manager.join("room1", "user1");
        manager.join("room1", "user1");
        assertThat(manager.getMemberCount("room1")).isEqualTo(1);
    }

    @Test
    @DisplayName("leaveAll 不影响其他用户")
    void leaveAllDoesNotAffectOthers() {
        manager.join("room1", "user1");
        manager.join("room1", "user2");
        manager.leaveAll("user1");
        assertThat(manager.isMember("room1", "user2")).isTrue();
        assertThat(manager.getMemberCount("room1")).isEqualTo(1);
    }
}
