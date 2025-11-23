package com.basebackend.cache.structure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

/**
 * 分布式 Sorted Set 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedSortedSetServiceImpl implements DistributedSortedSetService {

    private final RedissonClient redissonClient;

    @Override
    public <T> RScoredSortedSet<T> getSortedSet(String name) {
        log.debug("Getting distributed sorted set: {}", name);
        return redissonClient.getScoredSortedSet(name);
    }

    @Override
    public <T> boolean add(String setName, double score, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        boolean added = sortedSet.add(score, element);
        log.debug("Added element to sorted set {} with score {}: {} (added: {})", 
                setName, score, element, added);
        return added;
    }

    @Override
    public <T> int addAll(String setName, Map<T, Double> entries) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        int added = sortedSet.addAll(entries);
        log.debug("Added all {} entries to sorted set {}: {} new elements", 
                entries.size(), setName, added);
        return added;
    }

    @Override
    public <T> boolean remove(String setName, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        boolean removed = sortedSet.remove(element);
        log.debug("Removed element from sorted set {}: {} (removed: {})", 
                setName, element, removed);
        return removed;
    }

    @Override
    public <T> Double getScore(String setName, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Double score = sortedSet.getScore(element);
        log.debug("Got score for element {} in sorted set {}: {}", element, setName, score);
        return score;
    }

    @Override
    public <T> Integer rank(String setName, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Integer rank = sortedSet.rank(element);
        log.debug("Got rank for element {} in sorted set {}: {}", element, setName, rank);
        return rank;
    }

    @Override
    public <T> Integer revRank(String setName, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Integer rank = sortedSet.revRank(element);
        log.debug("Got reverse rank for element {} in sorted set {}: {}", element, setName, rank);
        return rank;
    }

    @Override
    public int size(String setName) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        int size = sortedSet.size();
        log.debug("Sorted set {} size: {}", setName, size);
        return size;
    }

    @Override
    public boolean isEmpty(String setName) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        boolean empty = sortedSet.isEmpty();
        log.debug("Sorted set {} is empty: {}", setName, empty);
        return empty;
    }

    @Override
    public void clear(String setName) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        sortedSet.clear();
        log.debug("Cleared sorted set: {}", setName);
    }

    @Override
    public <T> boolean contains(String setName, T element) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        boolean contains = sortedSet.contains(element);
        log.debug("Sorted set {} contains element {}: {}", setName, element, contains);
        return contains;
    }

    @Override
    public <T> Collection<T> range(String setName, int startRank, int endRank) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Collection<T> elements = sortedSet.valueRange(startRank, endRank);
        log.debug("Got range [{}, {}] from sorted set {}: {} elements", 
                startRank, endRank, setName, elements.size());
        return elements;
    }

    @Override
    public <T> Collection<T> revRange(String setName, int startRank, int endRank) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Collection<T> elements = sortedSet.valueRangeReversed(startRank, endRank);
        log.debug("Got reverse range [{}, {}] from sorted set {}: {} elements", 
                startRank, endRank, setName, elements.size());
        return elements;
    }

    @Override
    public <T> Collection<T> rangeByScore(String setName, double startScore, double endScore) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Collection<T> elements = sortedSet.valueRange(startScore, true, endScore, true);
        log.debug("Got score range [{}, {}] from sorted set {}: {} elements", 
                startScore, endScore, setName, elements.size());
        return elements;
    }

    @Override
    public int count(String setName, double startScore, double endScore) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        int count = sortedSet.count(startScore, true, endScore, true);
        log.debug("Count in score range [{}, {}] for sorted set {}: {}", 
                startScore, endScore, setName, count);
        return count;
    }

    @Override
    public <T> Double addScore(String setName, T element, double delta) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Double newScore = sortedSet.addScore(element, delta);
        log.debug("Added score {} to element {} in sorted set {}: new score {}", 
                delta, element, setName, newScore);
        return newScore;
    }

    @Override
    public int removeRange(String setName, int startRank, int endRank) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        int removed = sortedSet.removeRangeByRank(startRank, endRank);
        log.debug("Removed range [{}, {}] from sorted set {}: {} elements", 
                startRank, endRank, setName, removed);
        return removed;
    }

    @Override
    public int removeRangeByScore(String setName, double startScore, double endScore) {
        RScoredSortedSet<Object> sortedSet = getSortedSet(setName);
        int removed = sortedSet.removeRangeByScore(startScore, true, endScore, true);
        log.debug("Removed score range [{}, {}] from sorted set {}: {} elements", 
                startScore, endScore, setName, removed);
        return removed;
    }

    @Override
    public <T> T first(String setName) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Collection<T> elements = sortedSet.valueRangeReversed(0, 0);
        T first = elements.isEmpty() ? null : elements.iterator().next();
        log.debug("Got first element from sorted set {}: {}", setName, first);
        return first;
    }

    @Override
    public <T> T last(String setName) {
        RScoredSortedSet<T> sortedSet = getSortedSet(setName);
        Collection<T> elements = sortedSet.valueRange(0, 0);
        T last = elements.isEmpty() ? null : elements.iterator().next();
        log.debug("Got last element from sorted set {}: {}", setName, last);
        return last;
    }
}
