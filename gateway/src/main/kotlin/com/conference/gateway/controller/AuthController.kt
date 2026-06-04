package com.conference.gateway.controller

import com.conference.common.security.JwtUtil
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController {

    @PostMapping("/token")
    fun generateToken(@RequestBody request: TokenRequest): TokenResponse {
        val token = JwtUtil.generateToken(request.userId, request.roles)
        return TokenResponse(token = token, expiresIn = 3600)
    }
}

data class TokenRequest(
    val userId: Int,
    val roles: List<String> = listOf("ATTENDEE")
)

data class TokenResponse(
    val token: String,
    val expiresIn: Int
)
