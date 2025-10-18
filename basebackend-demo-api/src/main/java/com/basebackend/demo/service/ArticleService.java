package com.basebackend.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.demo.entity.Article;
import com.basebackend.demo.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;

    /**
     * 根据ID查询文章
     */
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    /**
     * 查询所有文章
     */
    public List<Article> list() {
        return articleMapper.selectList(null);
    }

    /**
     * 分页查询文章
     */
    public Page<Article> page(int current, int size, String category, Integer status) {
        Page<Article> page = new Page<>(current, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();

        if (category != null && !category.isEmpty()) {
            wrapper.eq(Article::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }

        wrapper.orderByDesc(Article::getPublishTime);
        return articleMapper.selectPage(page, wrapper);
    }

    /**
     * 根据作者ID查询文章
     */
    public List<Article> listByAuthor(Long authorId) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getAuthorId, authorId)
                .orderByDesc(Article::getPublishTime);
        return articleMapper.selectList(wrapper);
    }

    /**
     * 创建文章
     */
    public boolean create(Article article) {
        return articleMapper.insert(article) > 0;
    }

    /**
     * 更新文章
     */
    public boolean update(Article article) {
        return articleMapper.updateById(article) > 0;
    }

    /**
     * 删除文章（逻辑删除）
     */
    public boolean delete(Long id) {
        return articleMapper.deleteById(id) > 0;
    }

    /**
     * 发布文章
     */
    public boolean publish(Long id) {
        Article article = new Article();
        article.setId(id);
        article.setStatus(1);
        article.setPublishTime(LocalDateTime.now());
        return articleMapper.updateById(article) > 0;
    }

    /**
     * 增加浏览次数
     */
    public boolean incrementViewCount(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() + 1);
            return articleMapper.updateById(article) > 0;
        }
        return false;
    }

    /**
     * 搜索文章
     */
    public List<Article> search(String keyword) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Article::getTitle, keyword)
                .or()
                .like(Article::getSummary, keyword)
                .or()
                .like(Article::getContent, keyword)
                .or()
                .like(Article::getTags, keyword);
        wrapper.eq(Article::getStatus, 1); // 只搜索已发布的文章
        wrapper.orderByDesc(Article::getPublishTime);
        return articleMapper.selectList(wrapper);
    }
}
