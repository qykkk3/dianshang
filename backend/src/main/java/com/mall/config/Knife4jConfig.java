package com.mall.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（Knife4j / OpenAPI 3）
 *
 * <p>访问地址：http://localhost:8080/doc.html</p>
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer";

    @Bean
    public OpenAPI mallOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("电商管理系统 API 文档")
                        .description("""
                                基于 Spring Boot 3.3 + MyBatis-Plus + Spring Security/JWT 的 B2C 电商系统接口文档。

                                **鉴权说明**：先调用登录接口获取 token，再点击右上角「Authorize」填入 token（无需 Bearer 前缀）。

                                - 买家端登录：POST /api/auth/login（演示账号 zhangsan / 123456）
                                - 管理端登录：POST /api/admin/auth/login（演示账号 admin / 123456）
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("mall-team").email("dev@mall.com"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /** 买家商城端 */
    @Bean
    public GroupedOpenApi shopOpenApi() {
        return GroupedOpenApi.builder()
                .group("1-买家商城端")
                .pathsToMatch("/api/shop/**", "/api/auth/**", "/api/cart/**", "/api/address/**",
                        "/api/order/**", "/api/comment/**", "/api/refund/**", "/api/user/**")
                .build();
    }

    /** 管理后台 */
    @Bean
    public GroupedOpenApi adminOpenApi() {
        return GroupedOpenApi.builder()
                .group("2-管理后台")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
