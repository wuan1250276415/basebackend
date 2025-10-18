package com.basebackend.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.basebackend.demo.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
