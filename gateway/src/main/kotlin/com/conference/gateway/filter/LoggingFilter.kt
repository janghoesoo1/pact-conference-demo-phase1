package com.conference.gateway.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val startTime = System.currentTimeMillis()
        log.info(">>> {} {}", request.method, request.requestURI)
        filterChain.doFilter(request, response)
        val duration = System.currentTimeMillis() - startTime
        log.info("<<< {} {} {} ({}ms)", request.method, request.requestURI, response.status, duration)
    }
}
