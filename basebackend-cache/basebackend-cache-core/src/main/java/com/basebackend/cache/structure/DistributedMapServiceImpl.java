package com.basebackend.cache.structure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分布式 Map 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedMapServiceImpl implements DistributedMapService {

    private final RedissonClient redissonClient;

    @Override
    public <K, V> RMap<K, V> getMap(String name) {
        log.debug("Getting distributed map: {}", name);
        return redissonClient.getMap(name);
    }

    @Override
    public <K, V> void put(String mapName, K key, V value) {
        RMap<K, V> map = getMap(mapName);
        map.put(key, value);
        log.debug("Put key-value pair in map {}: {} = {}", mapName, key, value);
    }

    @Override
    public <K, V> void put(String mapName, K key, V value, long ttl, TimeUnit timeUnit) {
        RMap<K, V> map = getMap(mapName);
        map.fastPut(key, value);
        map.expire(java.time.Duration.ofMillis(timeUnit.toMillis(ttl)));
        log.debug("Put key-value pair in map {} with TTL {}{}: {} = {}", 
                mapName, ttl, timeUnit, key, value);
    }

    @Override
    public <K, V> V get(String mapName, K key) {
        RMap<K, V> map = getMap(mapName);
        V value = map.get(key);
        log.debug("Get value from map {} for key {}: {}", mapName, key, value);
        return value;
    }

    @Override
    public <K> void remove(String mapName, K key) {
        RMap<K, Object> map = getMap(mapName);
        map.remove(key);
        log.debug("Removed key from map {}: {}", mapName, key);
    }

    @Override
    public <K> boolean containsKey(String mapName, K key) {
        RMap<K, Object> map = getMap(mapName);
        boolean contains = map.containsKey(key);
        log.debug("Map {} contains key {}: {}", mapName, key, contains);
        return contains;
    }

    @Override
    public int size(String mapName) {
        RMap<Object, Object> map = getMap(mapName);
        int size = map.size();
        log.debug("Map {} size: {}", mapName, size);
        return size;
    }

    @Override
    public void clear(String mapName) {
        RMap<Object, Object> map = getMap(mapName);
        map.clear();
        log.debug("Cleared map: {}", mapName);
    }

    @Override
    public <K> Set<K> keySet(String mapName) {
        RMap<K, Object> map = getMap(mapName);
        Set<K> keys = map.keySet();
        log.debug("Got key set from map {}: {} keys", mapName, keys.size());
        return keys;
    }

    @Override
    public <K, V> void putAll(String mapName, Map<K, V> entries) {
        RMap<K, V> map = getMap(mapName);
        map.putAll(entries);
        log.debug("Put all {} entries in map {}", entries.size(), mapName);
    }

    @Override
    public <K, V> boolean putIfAbsent(String mapName, K key, V value) {
        RMap<K, V> map = getMap(mapName);
        V previous = map.putIfAbsent(key, value);
        boolean added = previous == null;
        log.debug("Put if absent in map {}: {} = {} (added: {})", mapName, key, value, added);
        return added;
    }
}
