package com.basebackend.cache.structure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * 分布式 Set 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedSetServiceImpl implements DistributedSetService {

    private final RedissonClient redissonClient;

    @Override
    public <T> RSet<T> getSet(String name) {
        log.debug("Getting distributed set: {}", name);
        return redissonClient.getSet(name);
    }

    @Override
    public <T> boolean add(String setName, T element) {
        RSet<T> set = getSet(setName);
        boolean added = set.add(element);
        log.debug("Added element to set {}: {} (added: {})", setName, element, added);
        return added;
    }

    @Override
    public <T> boolean addAll(String setName, Collection<T> elements) {
        RSet<T> set = getSet(setName);
        boolean added = set.addAll(elements);
        log.debug("Added all {} elements to set {}: {}", elements.size(), setName, added);
        return added;
    }

    @Override
    public <T> boolean remove(String setName, T element) {
        RSet<T> set = getSet(setName);
        boolean removed = set.remove(element);
        log.debug("Removed element from set {}: {} (removed: {})", setName, element, removed);
        return removed;
    }

    @Override
    public <T> boolean contains(String setName, T element) {
        RSet<T> set = getSet(setName);
        boolean contains = set.contains(element);
        log.debug("Set {} contains element {}: {}", setName, element, contains);
        return contains;
    }

    @Override
    public int size(String setName) {
        RSet<Object> set = getSet(setName);
        int size = set.size();
        log.debug("Set {} size: {}", setName, size);
        return size;
    }

    @Override
    public boolean isEmpty(String setName) {
        RSet<Object> set = getSet(setName);
        boolean empty = set.isEmpty();
        log.debug("Set {} is empty: {}", setName, empty);
        return empty;
    }

    @Override
    public void clear(String setName) {
        RSet<Object> set = getSet(setName);
        set.clear();
        log.debug("Cleared set: {}", setName);
    }

    @Override
    public <T> Set<T> readAll(String setName) {
        RSet<T> set = getSet(setName);
        Set<T> elements = set.readAll();
        log.debug("Read all {} elements from set {}", elements.size(), setName);
        return elements;
    }

    @Override
    public <T> T removeRandom(String setName) {
        RSet<T> set = getSet(setName);
        T element = set.removeRandom();
        log.debug("Removed random element from set {}: {}", setName, element);
        return element;
    }

    @Override
    public <T> T random(String setName) {
        RSet<T> set = getSet(setName);
        T element = set.random();
        log.debug("Got random element from set {}: {}", setName, element);
        return element;
    }

    @Override
    public <T> Set<T> random(String setName, int count) {
        RSet<T> set = getSet(setName);
        Set<T> elements = set.random(count);
        log.debug("Got {} random elements from set {}", count, setName);
        return elements;
    }

    @Override
    public <T> Set<T> intersection(String setName1, String setName2) {
        RSet<T> set1 = getSet(setName1);
        RSet<T> set2 = getSet(setName2);
        Set<T> result = set1.readAll();
        result.retainAll(set2.readAll());
        log.debug("Calculated intersection of sets {} and {}: {} elements", 
                setName1, setName2, result.size());
        return result;
    }

    @Override
    public <T> Set<T> union(String setName1, String setName2) {
        RSet<T> set1 = getSet(setName1);
        RSet<T> set2 = getSet(setName2);
        Set<T> result = set1.readAll();
        result.addAll(set2.readAll());
        log.debug("Calculated union of sets {} and {}: {} elements", 
                setName1, setName2, result.size());
        return result;
    }

    @Override
    public <T> Set<T> difference(String setName1, String setName2) {
        RSet<T> set1 = getSet(setName1);
        RSet<T> set2 = getSet(setName2);
        Set<T> result = set1.readAll();
        result.removeAll(set2.readAll());
        log.debug("Calculated difference of sets {} and {}: {} elements", 
                setName1, setName2, result.size());
        return result;
    }
}
