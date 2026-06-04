package com.conference.gateway.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val buckets = ConcurrentHashMap<String, TokenBucket>()
    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val clientIp = request.remoteAddr
        val bucket = buckets.computeIfAbsent(clientIp) { TokenBucket(maxTokens = 100, refillRate = 10) }

        if (bucket.tryConsume()) {
            response.setHeader("X-RateLimit-Remaining", bucket.remaining.toString())
            filterChain.doFilter(request, response)
        } else {
            log.warn("Rate limit exceeded for client: {}", clientIp)
            response.status = 429
            response.contentType = "application/problem+json"
            response.writer.write(
                """{"type":"about:blank","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Try again later."}"""
            )
        }
    }
}

data class TokenBucket(val maxTokens: Int, val refillRate: Int) {
    private val tokens = AtomicInteger(maxTokens)

    @Volatile
    private var lastRefill = System.currentTimeMillis()

    val remaining: Int get() = tokens.get()

    fun tryConsume(): Boolean {
        refill()
        return tokens.getAndUpdate { if (it > 0) it - 1 else 0 } > 0
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefill
        if (elapsed > 1000) {
            val tokensToAdd = (elapsed / 1000 * refillRate).toInt()
            tokens.getAndUpdate { minOf(it + tokensToAdd, maxTokens) }
            lastRefill = now
        }
    }
}
