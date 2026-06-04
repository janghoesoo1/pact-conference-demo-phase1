package com.conference.common.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

object JwtUtil {
    private const val SECRET = "conference-demo-secret-key-must-be-at-least-256-bits-long!!"
    private val key: SecretKey = Keys.hmacShaKeyFor(SECRET.toByteArray())
    private const val EXPIRATION_MS = 3600000L // 1 hour

    fun generateToken(userId: Int, roles: List<String>): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(Date(now.time + EXPIRATION_MS))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): TokenClaims? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            @Suppress("UNCHECKED_CAST")
            TokenClaims(
                userId = claims.subject.toInt(),
                roles = claims.get("roles", List::class.java) as List<String>
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class TokenClaims(
    val userId: Int,
    val roles: List<String>
)
