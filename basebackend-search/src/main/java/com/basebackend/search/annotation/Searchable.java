package com.basebackend.search.annotation;

import java.lang.annotation.*;

/**
 * 标记可搜索实体
 * <p>
 * 标注在实体类上，声明该实体可被索引和搜索。
 *
 * <pre>
 * &#64;Searchable(index = "articles", shards = 1, replicas = 1)
 * public class Article {
 *     &#64;SearchField(type = FieldType.TEXT, analyzer = "ik_max_word")
 *     private String title;
 *
 *     &#64;SearchField(type = FieldType.KEYWORD)
 *     private String status;
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Searchable {

    /** 索引名称 */
    String index();

    /** 分片数 */
    int shards() default 1;

    /** 副本数 */
    int replicas() default 1;
}
