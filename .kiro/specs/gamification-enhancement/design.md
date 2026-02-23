# Design Document: Gamification Enhancement Module

## Overview

本设计文档描述了游戏化增强模块（basebackend-gamification）的技术架构和实现方案。该模块作为一个独立的 Spring Boot Starter，提供成就系统、每日/周活动、等级系统等游戏化功能。

模块采用事件驱动架构，通过 Spring Event 机制与业务系统解耦，支持灵活的配置和扩展。

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      Business Services                          │
│  (User Service, Wheel Service, Couple Service, etc.)           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Events)
┌─────────────────────────────────────────────────────────────────┐
│                  Gamification Module                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Achievement │  │   Activity  │  │    Level    │             │
│  │   Service   │  │   Service   │  │   Service   │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
│         │                │                │                     │
│         └────────────────┼────────────────┘                     │
│                          ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   Reward Service                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                          │                                      │
│                          ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Progress Tracker Service                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Storage Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │    MySQL    │  │    Redis    │  │   Message   │             │
│  │  (持久化)   │  │   (缓存)    │  │   Queue     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

### 模块结构

```
basebackend-gamification/
├── src/main/java/com/basebackend/gamification/
│   ├── config/                    # 配置类
│   │   ├── GamificationAutoConfiguration.java
│   │   └── GamificationProperties.java
│   ├── achievement/               # 成就系统
│   │   ├── entity/
│   │   ├── dto/
│   │   ├── service/
│   │   ├── repository/
│   │   └── event/
│   ├── activity/                  # 活动系统
│   │   ├── checkin/              # 签到
│   │   ├── challenge/            # 挑战
│   │   └── event/                # 特殊活动
│   ├── level/                     # 等级系统
│   │   ├── entity/
│   │   ├── service/
│   │   └── perk/
│   ├── reward/                    # 奖励系统
│   ├── progress/                  # 进度追踪
│   └── common/                    # 公共组件
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```


## Components and Interfaces

### 1. 成就系统组件

#### Achievement Entity

```java
@Data
@Entity
@Table(name = "gm_achievement")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String code;                    // 成就唯一编码
    private String name;                    // 成就名称
    private String description;             // 成就描述
    private String iconUrl;                 // 图标URL
    
    @Enumerated(EnumType.STRING)
    private AchievementType type;           // 类型：NORMAL, HIDDEN, TIME_LIMITED, COOPERATIVE
    
    @Enumerated(EnumType.STRING)
    private AchievementCategory category;   // 分类
    
    private String conditionExpression;     // 条件表达式（SpEL）
    private Integer targetValue;            // 目标值
    
    private Long seriesId;                  // 所属系列ID
    private Integer seriesOrder;            // 系列中的顺序
    private Long prerequisiteId;            // 前置成就ID
    
    private LocalDateTime startTime;        // 限时成就开始时间
    private LocalDateTime endTime;          // 限时成就结束时间
    
    @Enumerated(EnumType.STRING)
    private HintLevel hintLevel;            // 隐藏成就提示级别
    
    private Integer participantCount;       // 协作成就参与者数量
    
    private Integer expReward;              // 经验值奖励
    private String itemRewards;             // 物品奖励（JSON）
    
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### UserAchievement Entity

```java
@Data
@Entity
@Table(name = "gm_user_achievement")
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long achievementId;
    
    @Enumerated(EnumType.STRING)
    private UnlockStatus status;            // LOCKED, IN_PROGRESS, UNLOCKED, EXPIRED
    
    private Integer currentProgress;        // 当前进度
    private LocalDateTime unlockTime;       // 解锁时间
    private Boolean showcased;              // 是否展示
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### AchievementService Interface

```java
public interface AchievementService {
    // 成就查询
    List<AchievementDTO> getAchievements(Long userId);
    AchievementDetailDTO getAchievementDetail(Long userId, Long achievementId);
    List<AchievementDTO> getSeriesAchievements(Long userId, Long seriesId);
    
    // 成就解锁
    void checkAndUnlock(Long userId, String eventType, Map<String, Object> context);
    void unlockAchievement(Long userId, Long achievementId);
    
    // 进度更新
    void updateProgress(Long userId, Long achievementId, Integer progress);
    
    // 展示墙
    List<AchievementDTO> getShowcase(Long userId);
    void updateShowcase(Long userId, List<Long> achievementIds);
    
    // 协作成就
    void updateCooperativeProgress(Long coupleId, Long achievementId, Long userId);
}
```


### 2. 活动系统组件

#### CheckIn Entity

```java
@Data
@Entity
@Table(name = "gm_check_in")
public class CheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private LocalDate checkInDate;          // 签到日期
    private Integer consecutiveDays;        // 连续签到天数
    private Integer monthlyCount;           // 当月签到次数
    
    private LocalDateTime createTime;
}
```

#### WeeklyChallenge Entity

```java
@Data
@Entity
@Table(name = "gm_weekly_challenge")
public class WeeklyChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private ChallengeType type;             // 挑战类型
    
    private Integer targetValue;            // 目标值
    private LocalDate weekStart;            // 周开始日期
    private LocalDate weekEnd;              // 周结束日期
    
    private Integer expReward;              // 经验值奖励
    private String rankRewards;             // 排名奖励（JSON）
    
    private Boolean enabled;
    private LocalDateTime createTime;
}
```

#### UserChallengeProgress Entity

```java
@Data
@Entity
@Table(name = "gm_user_challenge_progress")
public class UserChallengeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long challengeId;
    private Integer currentProgress;
    private Boolean completed;
    private Integer ranking;                // 排名
    
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### SpecialEvent Entity

```java
@Data
@Entity
@Table(name = "gm_special_event")
public class SpecialEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String code;
    private String name;
    private String description;
    private String bannerUrl;
    
    @Enumerated(EnumType.STRING)
    private EventType eventType;            // VALENTINE, CHRISTMAS, NEW_YEAR, ANNIVERSARY, CUSTOM
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private String tasks;                   // 活动任务（JSON）
    private String rewards;                 // 活动奖励（JSON）
    
    @Enumerated(EnumType.STRING)
    private EventStatus status;             // UPCOMING, ACTIVE, ENDED
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### ActivityService Interface

```java
public interface ActivityService {
    // 签到
    CheckInResultDTO checkIn(Long userId);
    CheckInStatusDTO getCheckInStatus(Long userId);
    List<LocalDate> getMonthlyCheckIns(Long userId, int year, int month);
    
    // 周挑战
    WeeklyChallengeDTO getCurrentChallenge();
    UserChallengeProgressDTO getChallengeProgress(Long userId, Long challengeId);
    void updateChallengeProgress(Long userId, Long challengeId, Integer increment);
    List<ChallengeRankingDTO> getChallengeRanking(Long challengeId, int limit);
    
    // 特殊活动
    List<SpecialEventDTO> getActiveEvents();
    List<SpecialEventDTO> getUpcomingEvents();
    EventProgressDTO getEventProgress(Long userId, Long eventId);
    void participateEvent(Long userId, Long eventId);
}
```


### 3. 等级系统组件

#### UserLevel Entity

```java
@Data
@Entity
@Table(name = "gm_user_level")
public class UserLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Integer currentLevel;           // 当前等级
    private Long currentExp;                // 当前经验值
    private Long totalExp;                  // 累计经验值
    
    private LocalDateTime lastLevelUpTime;  // 上次升级时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### CoupleLevel Entity

```java
@Data
@Entity
@Table(name = "gm_couple_level")
public class CoupleLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long coupleId;
    private Integer currentLevel;
    private Long currentExp;
    private Long totalExp;
    
    private Long user1Contribution;         // 用户1贡献的经验值
    private Long user2Contribution;         // 用户2贡献的经验值
    
    private LocalDateTime lastLevelUpTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

#### ExpRecord Entity

```java
@Data
@Entity
@Table(name = "gm_exp_record")
public class ExpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private Long coupleId;                  // 可选，情侣经验
    
    @Enumerated(EnumType.STRING)
    private ExpSourceType sourceType;       // 经验来源类型
    
    private String sourceId;                // 来源ID
    private Integer expAmount;              // 经验值数量
    private String description;             // 描述
    
    private LocalDateTime createTime;
}
```

#### LevelPerk Entity

```java
@Data
@Entity
@Table(name = "gm_level_perk")
public class LevelPerk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String code;
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private PerkType perkType;              // 特权类型
    
    private Integer requiredLevel;          // 解锁所需等级
    private Boolean coupleOnly;             // 是否仅情侣特权
    
    private Integer usageLimit;             // 使用次数限制（null表示无限）
    private String perkConfig;              // 特权配置（JSON）
    
    private Boolean enabled;
    private LocalDateTime createTime;
}
```

#### LevelService Interface

```java
public interface LevelService {
    // 等级查询
    UserLevelDTO getUserLevel(Long userId);
    CoupleLevelDTO getCoupleLevel(Long coupleId);
    
    // 经验值操作
    void addExp(Long userId, ExpSourceType sourceType, String sourceId, Integer amount);
    void addCoupleExp(Long coupleId, Long contributorId, ExpSourceType sourceType, Integer amount);
    List<ExpRecordDTO> getExpHistory(Long userId, int page, int size);
    
    // 等级特权
    List<LevelPerkDTO> getPerks(Long userId);
    List<LevelPerkDTO> getCouplePerks(Long coupleId);
    boolean hasPerk(Long userId, String perkCode);
    void usePerk(Long userId, String perkCode);
    
    // 等级配置
    LevelConfigDTO getLevelConfig(Integer level);
    Long getExpRequiredForLevel(Integer level);
}
```
