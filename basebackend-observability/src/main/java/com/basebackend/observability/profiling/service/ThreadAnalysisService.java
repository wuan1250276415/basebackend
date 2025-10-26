package com.basebackend.observability.profiling.service;

import com.basebackend.observability.profiling.model.DeadlockInfo;
import com.basebackend.observability.profiling.model.ThreadInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 线程分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadAnalysisService {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    /**
     * 获取所有线程信息
     */
    public List<ThreadInfo> getAllThreads() {
        long[] threadIds = threadMXBean.getAllThreadIds();
        List<ThreadInfo> threads = new ArrayList<>();
        
        for (long threadId : threadIds) {
            java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
            if (info != null) {
                threads.add(convertThreadInfo(info, threadId));
            }
        }
        
        return threads;
    }

    /**
     * 获取CPU使用率最高的线程
     */
    public List<ThreadInfo> getTopCpuThreads(int limit) {
        List<ThreadInfo> allThreads = getAllThreads();
        
        return allThreads.stream()
                .sorted(Comparator.comparing(ThreadInfo::getCpuTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取阻塞最多的线程
     */
    public List<ThreadInfo> getTopBlockedThreads(int limit) {
        List<ThreadInfo> allThreads = getAllThreads();
        
        return allThreads.stream()
                .sorted(Comparator.comparing(ThreadInfo::getBlockedTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 检测死锁
     */
    public List<DeadlockInfo> detectDeadlocks() {
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads == null || deadlockedThreads.length == 0) {
            return Collections.emptyList();
        }
        
        List<DeadlockInfo> deadlocks = new ArrayList<>();
        
        // 获取死锁线程的详细信息
        java.lang.management.ThreadInfo[] threadInfos = 
                threadMXBean.getThreadInfo(deadlockedThreads, true, true);
        
        // 分组：找出相互死锁的线程组
        Map<Long, Set<Long>> deadlockGroups = buildDeadlockGroups(threadInfos);
        
        for (Set<Long> group : deadlockGroups.values()) {
            List<ThreadInfo> threads = new ArrayList<>();
            for (Long threadId : group) {
                java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
                if (info != null) {
                    threads.add(convertThreadInfo(info, threadId));
                }
            }
            
            if (!threads.isEmpty()) {
                deadlocks.add(DeadlockInfo.builder()
                        .threads(threads)
                        .description(buildDeadlockDescription(threads))
                        .detectedAt(System.currentTimeMillis())
                        .severity("CRITICAL")
                        .build());
            }
        }
        
        return deadlocks;
    }

    /**
     * 获取线程统计
     */
    public Map<String, Object> getThreadStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalThreads", threadMXBean.getThreadCount());
        stats.put("peakThreads", threadMXBean.getPeakThreadCount());
        stats.put("daemonThreads", threadMXBean.getDaemonThreadCount());
        stats.put("totalStartedThreads", threadMXBean.getTotalStartedThreadCount());
        
        // 按状态统计
        Map<String, Long> stateCount = new HashMap<>();
        long[] threadIds = threadMXBean.getAllThreadIds();
        for (long threadId : threadIds) {
            java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(threadId);
            if (info != null) {
                String state = info.getThreadState().name();
                stateCount.put(state, stateCount.getOrDefault(state, 0L) + 1);
            }
        }
        stats.put("stateDistribution", stateCount);
        
        return stats;
    }

    /**
     * 获取线程堆栈快照
     */
    public Map<String, Object> getThreadDump() {
        Map<String, Object> dump = new HashMap<>();
        
        dump.put("timestamp", System.currentTimeMillis());
        dump.put("threads", getAllThreads());
        dump.put("deadlocks", detectDeadlocks());
        dump.put("statistics", getThreadStatistics());
        
        return dump;
    }

    /**
     * 转换线程信息
     */
    private ThreadInfo convertThreadInfo(java.lang.management.ThreadInfo info, long threadId) {
        List<String> stackTrace = new ArrayList<>();
        for (StackTraceElement element : info.getStackTrace()) {
            stackTrace.add(element.toString());
        }
        
        return ThreadInfo.builder()
                .threadId(threadId)
                .threadName(info.getThreadName())
                .state(info.getThreadState().name())
                .cpuTime(threadMXBean.getThreadCpuTime(threadId))
                .userTime(threadMXBean.getThreadUserTime(threadId))
                .blockedCount(info.getBlockedCount())
                .blockedTime(info.getBlockedTime())
                .waitedCount(info.getWaitedCount())
                .waitedTime(info.getWaitedTime())
                .lockName(info.getLockName())
                .lockOwnerId(info.getLockOwnerId())
                .stackTrace(stackTrace)
                .daemon(Thread.getAllStackTraces().keySet().stream()
                        .filter(t -> t.getId() == threadId)
                        .findFirst()
                        .map(Thread::isDaemon)
                        .orElse(false))
                .priority(Thread.getAllStackTraces().keySet().stream()
                        .filter(t -> t.getId() == threadId)
                        .findFirst()
                        .map(Thread::getPriority)
                        .orElse(Thread.NORM_PRIORITY))
                .build();
    }

    /**
     * 构建死锁组
     */
    private Map<Long, Set<Long>> buildDeadlockGroups(java.lang.management.ThreadInfo[] threadInfos) {
        Map<Long, Set<Long>> groups = new HashMap<>();
        
        for (java.lang.management.ThreadInfo info : threadInfos) {
            if (info == null) continue;
            
            long threadId = info.getThreadId();
            long lockOwnerId = info.getLockOwnerId();
            
            // 找到或创建死锁组
            Set<Long> group = groups.computeIfAbsent(threadId, k -> new HashSet<>());
            group.add(threadId);
            
            if (lockOwnerId != -1) {
                group.add(lockOwnerId);
                // 合并组
                Set<Long> ownerGroup = groups.get(lockOwnerId);
                if (ownerGroup != null) {
                    group.addAll(ownerGroup);
                    for (Long id : ownerGroup) {
                        groups.put(id, group);
                    }
                }
            }
        }
        
        return groups;
    }

    /**
     * 构建死锁描述
     */
    private String buildDeadlockDescription(List<ThreadInfo> threads) {
        StringBuilder sb = new StringBuilder();
        sb.append("检测到 ").append(threads.size()).append(" 个线程互相死锁:\n");
        
        for (ThreadInfo thread : threads) {
            sb.append("  - 线程 '").append(thread.getThreadName())
              .append("' (ID: ").append(thread.getThreadId()).append(")")
              .append(" 等待锁: ").append(thread.getLockName());
            
            if (thread.getLockOwnerId() != null && thread.getLockOwnerId() != -1) {
                sb.append(", 锁被线程 ").append(thread.getLockOwnerId()).append(" 持有");
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
}
