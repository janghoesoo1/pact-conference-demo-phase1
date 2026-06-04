package com.conference.common.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RoleCheckInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler !is HandlerMethod) return true

        val annotation = handler.getMethodAnnotation(RoleRequired::class.java) ?: return true

        val userIdHeader = request.getHeader("X-User-Id")
        val rolesHeader = request.getHeader("X-User-Roles")

        if (userIdHeader == null || rolesHeader == null) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            response.contentType = "application/problem+json"
            response.writer.write("""{"type":"https://conference.example.com/errors/unauthorized","title":"Unauthorized","status":401,"detail":"Authentication required"}""")
            return false
        }

        val userRoles = rolesHeader.split(",").map { it.trim() }
        val requiredRoles = annotation.roles.toList()

        if (requiredRoles.none { it in userRoles }) {
            response.status = HttpStatus.FORBIDDEN.value()
            response.contentType = "application/problem+json"
            response.writer.write("""{"type":"https://conference.example.com/errors/forbidden","title":"Forbidden","status":403,"detail":"Insufficient permissions. Required roles: ${requiredRoles.joinToString()}"}""")
            return false
        }

        UserContextHolder.set(UserContext(
            userId = userIdHeader.toInt(),
            roles = userRoles
        ))

        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        UserContextHolder.clear()
    }
}
