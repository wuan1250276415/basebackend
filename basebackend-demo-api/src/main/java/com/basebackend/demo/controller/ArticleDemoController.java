package com.basebackend.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.demo.entity.Article;
import com.basebackend.demo.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Validated
public class ArticleDemoController {

    private final ArticleService articleService;

    /**
     * 获取所有文章
     */
    @GetMapping
    public Result<List<Article>> list() {
        log.info("获取所有文章");
        List<Article> articles = articleService.list();
        return Result.success(articles);
    }

    /**
     * 根据ID获取文章
     */
    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id) {
        log.info("根据ID获取文章: {}", id);

        Article article = articleService.getById(id);
        if (article != null) {
            // 增加浏览次数
            articleService.incrementViewCount(id);
            return Result.success(article);
        }
        return Result.error(404, "文章不存在");
    }

    /**
     * 分页查询文章
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询文章 - current: {}, size: {}, category: {}, status: {}",
                current, size, category, status);

        Page<Article> page = articleService.page(current, size, category, status);

        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());

        return Result.success(data);
    }

    /**
     * 根据作者ID查询文章
     */
    @GetMapping("/author/{authorId}")
    public Result<List<Article>> listByAuthor(@PathVariable Long authorId) {
        log.info("根据作者ID查询文章: {}", authorId);
        List<Article> articles = articleService.listByAuthor(authorId);
        return Result.success(articles);
    }

    /**
     * 创建文章
     */
    @PostMapping
    public Result<String> create(@RequestBody Article article) {
        log.info("创建文章: {}", article.getTitle());

        // 设置初始值
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        if (article.getLikeCount() == null) {
            article.setLikeCount(0);
        }
        if (article.getStatus() == null) {
            article.setStatus(0); // 默认为草稿
        }

        boolean success = articleService.create(article);
        if (success) {
            return Result.success("文章创建成功");
        }
        return Result.error(500, "文章创建失败");
    }

    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Article article) {
        log.info("更新文章: {}", id);

        Article existArticle = articleService.getById(id);
        if (existArticle == null) {
            return Result.error(404, "文章不存在");
        }

        article.setId(id);
        boolean success = articleService.update(article);
        if (success) {
            return Result.success("文章更新成功");
        }
        return Result.error(500, "文章更新失败");
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("删除文章: {}", id);

        Article existArticle = articleService.getById(id);
        if (existArticle == null) {
            return Result.error(404, "文章不存在");
        }

        boolean success = articleService.delete(id);
        if (success) {
            return Result.success("文章删除成功");
        }
        return Result.error(500, "文章删除失败");
    }

    /**
     * 发布文章
     */
    @PostMapping("/{id}/publish")
    public Result<String> publish(@PathVariable Long id) {
        log.info("发布文章: {}", id);

        Article existArticle = articleService.getById(id);
        if (existArticle == null) {
            return Result.error(404, "文章不存在");
        }

        boolean success = articleService.publish(id);
        if (success) {
            return Result.success("文章发布成功");
        }
        return Result.error(500, "文章发布失败");
    }

    /**
     * 搜索文章
     */
    @GetMapping("/search")
    public Result<List<Article>> search(@RequestParam String keyword) {
        log.info("搜索文章: {}", keyword);
        List<Article> articles = articleService.search(keyword);
        return Result.success(articles);
    }

    /**
     * 获取文章统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        log.info("获取文章统计信息");

        List<Article> allArticles = articleService.list();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", allArticles.size());
        stats.put("publishedCount", allArticles.stream().filter(a -> a.getStatus() == 1).count());
        stats.put("draftCount", allArticles.stream().filter(a -> a.getStatus() == 0).count());
        stats.put("totalViewCount", allArticles.stream().mapToInt(Article::getViewCount).sum());
        stats.put("totalLikeCount", allArticles.stream().mapToInt(Article::getLikeCount).sum());

        return Result.success(stats);
    }

    /**
     * 获取热门文章（按浏览次数排序）
     */
    @GetMapping("/hot")
    public Result<List<Article>> hot(@RequestParam(defaultValue = "10") int limit) {
        log.info("获取热门文章，限制: {}", limit);
        List<Article> articles = articleService.list();
        articles.sort((a, b) -> b.getViewCount().compareTo(a.getViewCount()));
        return Result.success(articles.subList(0, Math.min(limit, articles.size())));
    }
}
