package com.basebackend.album;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 家庭相册共享微服务启动类
 *
 * @author BearTeam
 */
@SpringBootApplication
@MapperScan("com.basebackend.album.mapper")
public class AlbumApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlbumApiApplication.class, args);
    }
}
