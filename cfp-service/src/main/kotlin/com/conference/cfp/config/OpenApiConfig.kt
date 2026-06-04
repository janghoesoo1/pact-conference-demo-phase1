package com.conference.cfp.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(Info()
            .title("CFP Service API")
            .description("컨퍼런스 발표 제안(Call for Papers) 관리 서비스")
            .version("1.0.0")
            .contact(Contact().name("Conference Team").email("team@conference.com"))
        )
}
