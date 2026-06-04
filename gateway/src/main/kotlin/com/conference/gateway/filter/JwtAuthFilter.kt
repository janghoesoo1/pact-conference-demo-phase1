package com.conference.gateway.filter

import com.conference.common.security.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class JwtAuthFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    private val publicPaths = listOf(
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/auth"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (isPublicPath(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        // Allow read operations without auth for demo purposes
        if (request.method == "GET") {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "Authorization header required for write operations")
            return
        }

        val token = authHeader.substring(7)
        val claims = JwtUtil.validateToken(token)
        if (claims == null) {
            sendError(response, 401, "Invalid or expired token")
            return
        }

        // Add user info as headers for downstream services
        val wrappedRequest = object : HttpServletRequestWrapper(request) {
            override fun getHeader(name: String): String? {
                return when (name) {
                    "X-User-Id" -> claims.userId.toString()
                    "X-User-Roles" -> claims.roles.joinToString(",")
                    else -> super.getHeader(name)
                }
            }

            override fun getHeaderNames(): java.util.Enumeration<String> {
                val names = super.getHeaderNames().toList().toMutableList()
                names.add("X-User-Id")
                names.add("X-User-Roles")
                return java.util.Collections.enumeration(names)
            }
        }

        log.info("Authenticated user: {} roles: {}", claims.userId, claims.roles)
        filterChain.doFilter(wrappedRequest, response)
    }

    private fun isPublicPath(uri: String): Boolean {
        return publicPaths.any { uri.startsWith(it) }
    }

    private fun sendError(response: HttpServletResponse, status: Int, detail: String) {
        response.status = status
        response.contentType = "application/problem+json"
        val title = if (status == 401) "Unauthorized" else "Forbidden"
        response.writer.write("""{"type":"https://conference.example.com/errors/${title.lowercase()}","title":"$title","status":$status,"detail":"$detail"}""")
    }
}
