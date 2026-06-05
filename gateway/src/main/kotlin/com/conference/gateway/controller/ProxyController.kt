package com.conference.gateway.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.mvc.ProxyExchange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ProxyController {

    @Value("\${services.session.url:http://localhost:8082}")
    private lateinit var sessionUrl: String

    @Value("\${services.attendee.url:http://localhost:8081}")
    private lateinit var attendeeUrl: String

    @Value("\${services.cfp.url:http://localhost:8083}")
    private lateinit var cfpUrl: String

    @RequestMapping("/sessions/**")
    fun sessions(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$sessionUrl${proxy.path()}", proxy, request)
    }

    @RequestMapping("/attendees/**")
    fun attendees(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$attendeeUrl${proxy.path()}", proxy, request)
    }

    @RequestMapping("/proposals/**")
    fun proposals(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$cfpUrl${proxy.path()}", proxy, request)
    }

    @RequestMapping("/v1/sessions/**")
    fun v1Sessions(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$sessionUrl${proxy.path()}", proxy, request)
    }

    @RequestMapping("/v2/sessions/**")
    fun v2Sessions(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$sessionUrl${proxy.path()}", proxy, request)
    }

    private val hopByHopHeaders = setOf(
        "Transfer-Encoding", "Keep-Alive", "Connection",
        "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailer", "Upgrade"
    )

    private fun proxyTo(
        targetUri: String,
        proxy: ProxyExchange<ByteArray>,
        request: HttpServletRequest
    ): ResponseEntity<ByteArray> {
        val configured = proxy.uri(targetUri)
        val response = when (request.method.uppercase()) {
            "POST" -> configured.post()
            "PUT" -> configured.put()
            "DELETE" -> configured.delete()
            "PATCH" -> configured.patch()
            "HEAD" -> configured.head()
            "OPTIONS" -> configured.options()
            else -> configured.get()
        }
        // Remove hop-by-hop headers to prevent chunked encoding conflicts
        val cleanHeaders = org.springframework.http.HttpHeaders()
        response.headers.forEach { (name, values) ->
            if (!hopByHopHeaders.contains(name)) {
                cleanHeaders[name] = values
            }
        }
        return ResponseEntity.status(response.statusCode)
            .headers(cleanHeaders)
            .body(response.body)
    }
}
