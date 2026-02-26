package com.basebackend.cache.structure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 分布式 List 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedListServiceImpl implements DistributedListService {

    private final RedissonClient redissonClient;

    @Override
    public <T> RList<T> getList(String name) {
        log.debug("Getting distributed list: {}", name);
        return redissonClient.getList(name);
    }

    @Override
    public <T> boolean add(String listName, T element) {
        RList<T> list = getList(listName);
        boolean added = list.add(element);
        log.debug("Added element to list {}: {} (added: {})", listName, element, added);
        return added;
    }

    @Override
    public <T> void add(String listName, int index, T element) {
        RList<T> list = getList(listName);
        list.add(index, element);
        log.debug("Added element to list {} at index {}: {}", listName, index, element);
    }

    @Override
    public <T> boolean addAll(String listName, Collection<T> elements) {
        RList<T> list = getList(listName);
        boolean added = list.addAll(elements);
        log.debug("Added all {} elements to list {}: {}", elements.size(), listName, added);
        return added;
    }

    @Override
    public <T> T get(String listName, int index) {
        RList<T> list = getList(listName);
        T element = list.get(index);
        log.debug("Got element from list {} at index {}: {}", listName, index, element);
        return element;
    }

    @Override
    public <T> T set(String listName, int index, T element) {
        RList<T> list = getList(listName);
        T previous = list.set(index, element);
        log.debug("Set element in list {} at index {}: {} (previous: {})", 
                listName, index, element, previous);
        return previous;
    }

    @Override
    public <T> T remove(String listName, int index) {
        RList<T> list = getList(listName);
        T removed = list.remove(index);
        log.debug("Removed element from list {} at index {}: {}", listName, index, removed);
        return removed;
    }

    @Override
    public <T> boolean remove(String listName, T element) {
        RList<T> list = getList(listName);
        boolean removed = list.remove(element);
        log.debug("Removed element from list {}: {} (removed: {})", listName, element, removed);
        return removed;
    }

    @Override
    public int size(String listName) {
        RList<Object> list = getList(listName);
        int size = list.size();
        log.debug("List {} size: {}", listName, size);
        return size;
    }

    @Override
    public boolean isEmpty(String listName) {
        RList<Object> list = getList(listName);
        boolean empty = list.isEmpty();
        log.debug("List {} is empty: {}", listName, empty);
        return empty;
    }

    @Override
    public void clear(String listName) {
        RList<Object> list = getList(listName);
        list.clear();
        log.debug("Cleared list: {}", listName);
    }

    @Override
    public <T> boolean contains(String listName, T element) {
        RList<T> list = getList(listName);
        boolean contains = list.contains(element);
        log.debug("List {} contains element {}: {}", listName, element, contains);
        return contains;
    }

    @Override
    public <T> int indexOf(String listName, T element) {
        RList<T> list = getList(listName);
        int index = list.indexOf(element);
        log.debug("Element {} in list {} at index: {}", element, listName, index);
        return index;
    }

    @Override
    public <T> List<T> readAll(String listName) {
        RList<T> list = getList(listName);
        List<T> elements = list.readAll();
        log.debug("Read all {} elements from list {}", elements.size(), listName);
        return elements;
    }

    @Override
    public <T> List<T> range(String listName, int fromIndex, int toIndex) {
        RList<T> list = getList(listName);
        List<T> elements = list.range(fromIndex, toIndex);
        log.debug("Read range [{}, {}] from list {}: {} elements", 
                fromIndex, toIndex, listName, elements.size());
        return elements;
    }

    @Override
    public <T> T removeFirst(String listName) {
        RList<T> list = getList(listName);
        if (list.isEmpty()) {
            log.debug("List {} is empty, cannot remove first element", listName);
            return null;
        }
        T element = list.remove(0);
        log.debug("Removed first element from list {}: {}", listName, element);
        return element;
    }

    @Override
    public <T> T removeLast(String listName) {
        RList<T> list = getList(listName);
        if (list.isEmpty()) {
            log.debug("List {} is empty, cannot remove last element", listName);
            return null;
        }
        T element = list.remove(list.size() - 1);
        log.debug("Removed last element from list {}: {}", listName, element);
        return element;
    }
}
