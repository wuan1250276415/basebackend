# Requirements Document

## Introduction

本文档定义了游戏化增强模块（basebackend-gamification）的需求规范。该模块旨在为应用提供完整的游戏化能力，包括成就系统、每日/周活动、等级系统等功能，以提升用户粘性和参与度。

游戏化增强模块将作为一个独立的 Spring Boot Starter 模块，可被其他业务服务引用，提供开箱即用的游戏化功能支持。

## Glossary

- **Gamification_Module**: basebackend-gamification 模块，提供游戏化能力的基础组件
- **Achievement**: 成就，用户完成特定目标后获得的奖励标识
- **Achievement_Series**: 系列成就，具有递进关系的成就链（如：首次约会 → 10次约会 → 50次约会）
- **Hidden_Achievement**: 隐藏成就，在达成前不显示条件的特殊成就
- **Time_Limited_Achievement**: 限时成就，仅在特定时间段内可获得的成就
- **Cooperative_Achievement**: 协作成就，需要多人协作完成的成就
- **Daily_Check_In**: 每日签到，用户每天登录并签到获取奖励的机制
- **Weekly_Challenge**: 每周挑战，每周更新的目标任务，完成后获得奖励
- **Special_Event**: 特殊活动，节日或特定时期举办的限时活动
- **Level_System**: 等级系统，基于经验值的用户成长体系
- **Experience_Point**: 经验值（EXP），用户通过各种行为获得的成长数值
- **Level_Perk**: 等级特权，达到特定等级后解锁的功能或奖励
- **Couple_Level**: 情侣等级，情侣双方共同的等级
- **Achievement_Showcase**: 成就展示墙，用户展示已获得成就的个人空间
- **Reward**: 奖励，完成任务或达成成就后获得的虚拟物品或权益
- **Progress_Tracker**: 进度追踪器，记录用户完成目标的进度

## Requirements

### Requirement 1: 成就系统基础功能

**User Story:** 作为用户，我希望通过完成各种目标获得成就，以便获得成就感和展示我的活跃度

#### Acceptance Criteria

1. WHEN 用户完成成就条件 THEN Gamification_Module SHALL 自动解锁该成就并记录解锁时间
2. WHEN 用户查询成就列表 THEN Gamification_Module SHALL 返回所有可见成就及其解锁状态
3. WHEN 成就被解锁 THEN Gamification_Module SHALL 发送成就解锁通知给用户
4. WHEN 成就被解锁 THEN Gamification_Module SHALL 发放成就关联的奖励
5. WHEN 用户查询单个成就详情 THEN Gamification_Module SHALL 返回成就描述、条件、奖励和解锁状态

### Requirement 2: 系列成就

**User Story:** 作为用户，我希望看到成就的递进关系，以便了解我的成长路径和下一个目标

#### Acceptance Criteria

1. WHEN 配置系列成就时 THEN Gamification_Module SHALL 支持定义成就链的前后依赖关系
2. WHEN 用户解锁系列中的成就 THEN Gamification_Module SHALL 显示下一个可解锁的成就
3. WHEN 用户查询系列成就 THEN Gamification_Module SHALL 返回整个系列的进度信息
4. WHEN 系列成就全部完成 THEN Gamification_Module SHALL 授予系列完成的额外奖励
5. WHEN 前置成就未解锁 THEN Gamification_Module SHALL 将后续成就标记为锁定状态

### Requirement 3: 隐藏成就

**User Story:** 作为用户，我希望发现隐藏成就，以便获得探索的惊喜感

#### Acceptance Criteria

1. WHEN 用户查询成就列表 THEN Gamification_Module SHALL 对未解锁的隐藏成就仅显示占位信息
2. WHEN 用户解锁隐藏成就 THEN Gamification_Module SHALL 显示完整的成就信息和描述
3. WHEN 配置隐藏成就时 THEN Gamification_Module SHALL 支持设置提示级别（无提示/模糊提示/详细提示）
4. WHEN 用户接近解锁隐藏成就 THEN Gamification_Module SHALL 根据配置决定是否显示进度提示
5. IF 隐藏成就被解锁 THEN Gamification_Module SHALL 在通知中标注"隐藏成就已发现"

### Requirement 4: 限时成就

**User Story:** 作为用户，我希望参与限时成就挑战，以便获得稀有的限定奖励

#### Acceptance Criteria

1. WHEN 限时成就活动开始 THEN Gamification_Module SHALL 自动激活该成就并通知用户
2. WHEN 限时成就活动结束 THEN Gamification_Module SHALL 自动关闭该成就的解锁通道
3. WHEN 用户在活动期间完成条件 THEN Gamification_Module SHALL 正常解锁限时成就
4. WHEN 用户查询限时成就 THEN Gamification_Module SHALL 显示剩余时间和活动状态
5. WHEN 限时成就过期未完成 THEN Gamification_Module SHALL 将该成就标记为"已过期"状态

### Requirement 5: 协作成就

**User Story:** 作为情侣用户，我希望与伴侣共同完成协作成就，以便增进互动和默契

#### Acceptance Criteria

1. WHEN 配置协作成就时 THEN Gamification_Module SHALL 支持定义参与者数量和角色要求
2. WHEN 协作成就的所有参与者都完成各自任务 THEN Gamification_Module SHALL 为所有参与者解锁成就
3. WHEN 用户查询协作成就进度 THEN Gamification_Module SHALL 显示所有参与者的完成状态
4. WHEN 部分参与者完成任务 THEN Gamification_Module SHALL 记录进度但不解锁成就
5. WHEN 协作成就被解锁 THEN Gamification_Module SHALL 为所有参与者发放相同奖励

### Requirement 6: 成就展示墙

**User Story:** 作为用户，我希望展示我获得的成就，以便向他人展示我的成就和活跃度

#### Acceptance Criteria

1. WHEN 用户配置展示墙 THEN Gamification_Module SHALL 允许选择最多N个成就进行展示
2. WHEN 其他用户查看展示墙 THEN Gamification_Module SHALL 返回被展示的成就列表
3. WHEN 用户更新展示墙配置 THEN Gamification_Module SHALL 立即生效并持久化配置
4. WHEN 展示墙中的成就被撤销 THEN Gamification_Module SHALL 自动从展示墙移除该成就
5. WHEN 用户未配置展示墙 THEN Gamification_Module SHALL 返回默认的最新解锁成就

### Requirement 7: 每日签到

**User Story:** 作为用户，我希望通过每日签到获得奖励，以便培养使用习惯并获得持续收益

#### Acceptance Criteria

1. WHEN 用户首次当日签到 THEN Gamification_Module SHALL 记录签到并发放当日奖励
2. WHEN 用户重复当日签到 THEN Gamification_Module SHALL 返回已签到状态且不重复发放奖励
3. WHEN 用户连续签到 THEN Gamification_Module SHALL 累计连续天数并发放额外奖励
4. WHEN 用户中断签到 THEN Gamification_Module SHALL 重置连续签到天数
5. WHEN 用户查询签到状态 THEN Gamification_Module SHALL 返回当月签到记录和连续天数

### Requirement 8: 每周挑战

**User Story:** 作为用户，我希望参与每周挑战，以便获得额外奖励和排行榜竞争乐趣

#### Acceptance Criteria

1. WHEN 新的一周开始 THEN Gamification_Module SHALL 自动生成新的周挑战任务
2. WHEN 用户完成周挑战目标 THEN Gamification_Module SHALL 发放挑战奖励
3. WHEN 用户查询周挑战 THEN Gamification_Module SHALL 返回当前挑战内容、进度和剩余时间
4. WHEN 周挑战结束 THEN Gamification_Module SHALL 生成排行榜并发放排名奖励
5. WHEN 用户参与周挑战 THEN Gamification_Module SHALL 实时更新排行榜排名

### Requirement 9: 特殊活动

**User Story:** 作为用户，我希望参与节日特殊活动，以便获得限定奖励和节日氛围

#### Acceptance Criteria

1. WHEN 特殊活动开始 THEN Gamification_Module SHALL 激活活动并推送通知给所有用户
2. WHEN 用户参与特殊活动 THEN Gamification_Module SHALL 记录参与状态和进度
3. WHEN 用户完成活动任务 THEN Gamification_Module SHALL 发放活动专属奖励
4. WHEN 特殊活动结束 THEN Gamification_Module SHALL 关闭活动入口并保留历史记录
5. WHEN 用户查询活动列表 THEN Gamification_Module SHALL 返回进行中和即将开始的活动

### Requirement 10: 等级系统

**User Story:** 作为用户，我希望通过积累经验值提升等级，以便解锁更多特权和展示成长

#### Acceptance Criteria

1. WHEN 用户获得经验值 THEN Gamification_Module SHALL 累加到用户当前经验值
2. WHEN 用户经验值达到升级阈值 THEN Gamification_Module SHALL 自动提升用户等级
3. WHEN 用户升级 THEN Gamification_Module SHALL 发送升级通知并解锁对应等级特权
4. WHEN 用户查询等级信息 THEN Gamification_Module SHALL 返回当前等级、经验值和升级进度
5. WHEN 配置等级系统时 THEN Gamification_Module SHALL 支持自定义各等级所需经验值

### Requirement 11: 经验值获取

**User Story:** 作为用户，我希望通过各种行为获得经验值，以便持续成长和提升等级

#### Acceptance Criteria

1. WHEN 用户完成指定行为 THEN Gamification_Module SHALL 根据配置发放对应经验值
2. WHEN 配置经验值来源时 THEN Gamification_Module SHALL 支持定义行为类型和对应经验值
3. WHEN 用户获得经验值 THEN Gamification_Module SHALL 记录经验值来源和获取时间
4. WHEN 用户查询经验值记录 THEN Gamification_Module SHALL 返回经验值获取历史
5. IF 经验值获取存在每日上限 THEN Gamification_Module SHALL 在达到上限后停止发放

### Requirement 12: 等级特权

**User Story:** 作为用户，我希望通过提升等级解锁特权，以便获得更好的使用体验

#### Acceptance Criteria

1. WHEN 用户达到特定等级 THEN Gamification_Module SHALL 自动解锁该等级的特权
2. WHEN 用户查询特权列表 THEN Gamification_Module SHALL 返回已解锁和待解锁的特权
3. WHEN 配置等级特权时 THEN Gamification_Module SHALL 支持定义特权类型和解锁等级
4. WHEN 用户使用特权功能 THEN Gamification_Module SHALL 验证用户是否已解锁该特权
5. WHEN 特权有使用次数限制 THEN Gamification_Module SHALL 追踪使用次数并在耗尽后禁用

### Requirement 13: 情侣等级

**User Story:** 作为情侣用户，我希望与伴侣共享等级成长，以便增强情侣互动和共同目标感

#### Acceptance Criteria

1. WHEN 情侣任一方获得经验值 THEN Gamification_Module SHALL 将经验值累加到情侣共享池
2. WHEN 情侣等级提升 THEN Gamification_Module SHALL 通知双方并解锁情侣专属特权
3. WHEN 用户查询情侣等级 THEN Gamification_Module SHALL 返回情侣等级、经验值和双方贡献
4. WHEN 情侣关系解除 THEN Gamification_Module SHALL 保留历史等级记录但停止经验累积
5. WHEN 配置情侣等级时 THEN Gamification_Module SHALL 支持定义情侣专属的等级特权

### Requirement 14: 奖励系统

**User Story:** 作为用户，我希望获得的奖励能够正确发放和使用，以便享受游戏化带来的实际收益

#### Acceptance Criteria

1. WHEN 用户获得奖励 THEN Gamification_Module SHALL 将奖励添加到用户背包
2. WHEN 用户使用奖励 THEN Gamification_Module SHALL 验证奖励有效性并执行奖励效果
3. WHEN 奖励有过期时间 THEN Gamification_Module SHALL 在过期后自动标记为失效
4. WHEN 用户查询奖励列表 THEN Gamification_Module SHALL 返回所有有效奖励及其状态
5. WHEN 奖励发放失败 THEN Gamification_Module SHALL 记录失败原因并支持重试机制

### Requirement 15: 进度追踪

**User Story:** 作为用户，我希望实时查看各项任务的完成进度，以便了解距离目标还有多远

#### Acceptance Criteria

1. WHEN 用户行为影响任务进度 THEN Gamification_Module SHALL 实时更新进度数据
2. WHEN 用户查询任务进度 THEN Gamification_Module SHALL 返回当前进度和目标值
3. WHEN 进度达到100% THEN Gamification_Module SHALL 触发任务完成逻辑
4. WHEN 进度数据更新 THEN Gamification_Module SHALL 持久化进度以防数据丢失
5. WHEN 任务重置 THEN Gamification_Module SHALL 清零进度并保留历史完成记录

