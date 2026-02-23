package com.basebackend.scheduler.form.storage;

import com.basebackend.scheduler.form.model.data.FormData;
import com.basebackend.scheduler.form.model.schema.FormSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表单存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormStorageService {
    
    private final Map<String, FormSchema> templates = new ConcurrentHashMap<>();
    private final Map<String, FormData> dataStore = new ConcurrentHashMap<>();
    
    public void saveTemplate(String templateId, FormSchema schema) {
        log.debug("Saving form template: {}", templateId);
        templates.put(templateId, schema);
    }
    
    public Optional<FormSchema> getTemplate(String templateId) {
        return Optional.ofNullable(templates.get(templateId));
    }
    
    public List<FormSchema> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }
    
    public void deleteTemplate(String templateId, String deletedBy) {
        log.debug("Deleting form template: {}", templateId);
        templates.remove(templateId);
    }
    
    public void saveData(String dataId, FormData formData) {
        log.debug("Saving form data: {}", dataId);
        dataStore.put(dataId, formData);
    }
    
    public Optional<FormData> getData(String dataId) {
        return Optional.ofNullable(dataStore.get(dataId));
    }
    
    public List<FormData> getDataList(String templateId) {
        return dataStore.values().stream()
                .filter(data -> data.getTemplateId().equals(templateId))
                .filter(data -> !data.isDeleted())
                .collect(ArrayList::new, (list, data) -> list.add(data), ArrayList::addAll);
    }
    
    public void deleteData(String dataId, String deletedBy) {
        Optional<FormData> dataOpt = getData(dataId);
        if (dataOpt.isPresent()) {
            FormData data = dataOpt.get();
            data.setDeleted(true);
            data.setDeletedAt(Instant.now());
            data.setDeletedBy(deletedBy);
            saveData(dataId, data);
        }
    }
    
    public List<FormData> searchData(String templateId, Map<String, Object> criteria) {
        List<FormData> results = getDataList(templateId);
        
        if (criteria == null || criteria.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .filter(data -> matchesCriteria(data, criteria))
                .collect(ArrayList::new, (list, data) -> list.add(data), ArrayList::addAll);
    }
    
    private boolean matchesCriteria(FormData data, Map<String, Object> criteria) {
        return criteria.entrySet().stream()
                .allMatch(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Object dataValue = data.getData().get(key);
                    return value.equals(dataValue);
                });
    }
}
