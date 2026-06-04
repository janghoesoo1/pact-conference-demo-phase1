package com.conference.gateway

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayApplicationTest {
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `application context loads`() {
        assertNotNull(context)
    }
}
