package com.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 电商管理系统 启动类
 *
 * <p>技术栈：Spring Boot 3.3 + MyBatis-Plus + Spring Security/JWT + MySQL 8 + Redis</p>
 *
 * @author mall-team
 */
@EnableAsync
@EnableTransactionManagement
// 本项目使用自定义 JWT 认证，关闭 Spring Security 默认内存用户（避免启动时打印随机密码）
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
        System.out.println("""

                ============================================================
                  电商管理系统后端启动成功
                  商城前台接口: http://localhost:8080/api/shop/**
                  管理后台接口: http://localhost:8080/api/admin/**
                  在线接口文档: http://localhost:8080/doc.html
                ============================================================
                """);
    }
}
