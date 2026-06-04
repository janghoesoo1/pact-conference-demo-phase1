package com.conference.common.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class SecurityWebConfig(
    private val roleCheckInterceptor: RoleCheckInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(roleCheckInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
    }
}
