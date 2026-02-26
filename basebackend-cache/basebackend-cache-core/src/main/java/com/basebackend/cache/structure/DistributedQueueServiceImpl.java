package com.basebackend.cache.structure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分布式队列服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedQueueServiceImpl implements DistributedQueueService {

    private final RedissonClient redissonClient;

    @Override
    public <T> RQueue<T> getQueue(String name) {
        log.debug("Getting distributed queue: {}", name);
        return redissonClient.getQueue(name);
    }

    @Override
    public <T> boolean offer(String queueName, T element) {
        RQueue<T> queue = getQueue(queueName);
        boolean added = queue.offer(element);
        log.debug("Offered element to queue {}: {} (added: {})", queueName, element, added);
        return added;
    }

    @Override
    public <T> T poll(String queueName) {
        RQueue<T> queue = getQueue(queueName);
        T element = queue.poll();
        log.debug("Polled element from queue {}: {}", queueName, element);
        return element;
    }

    @Override
    public <T> T peek(String queueName) {
        RQueue<T> queue = getQueue(queueName);
        T element = queue.peek();
        log.debug("Peeked element from queue {}: {}", queueName, element);
        return element;
    }

    @Override
    public long size(String queueName) {
        RQueue<Object> queue = getQueue(queueName);
        int size = queue.size();
        log.debug("Queue {} size: {}", queueName, size);
        return size;
    }

    @Override
    public boolean isEmpty(String queueName) {
        RQueue<Object> queue = getQueue(queueName);
        boolean empty = queue.isEmpty();
        log.debug("Queue {} is empty: {}", queueName, empty);
        return empty;
    }

    @Override
    public void clear(String queueName) {
        RQueue<Object> queue = getQueue(queueName);
        queue.clear();
        log.debug("Cleared queue: {}", queueName);
    }

    @Override
    public <T> boolean addAll(String queueName, Collection<T> elements) {
        RQueue<T> queue = getQueue(queueName);
        boolean added = queue.addAll(elements);
        log.debug("Added all {} elements to queue {}: {}", elements.size(), queueName, added);
        return added;
    }

    @Override
    public <T> boolean remove(String queueName, T element) {
        RQueue<T> queue = getQueue(queueName);
        boolean removed = queue.remove(element);
        log.debug("Removed element from queue {}: {} (removed: {})", queueName, element, removed);
        return removed;
    }

    @Override
    public <T> boolean contains(String queueName, T element) {
        RQueue<T> queue = getQueue(queueName);
        boolean contains = queue.contains(element);
        log.debug("Queue {} contains element {}: {}", queueName, element, contains);
        return contains;
    }

    @Override
    public <T> List<T> readAll(String queueName) {
        RQueue<T> queue = getQueue(queueName);
        List<T> elements = queue.readAll();
        log.debug("Read all {} elements from queue {}", elements.size(), queueName);
        return elements;
    }
}
