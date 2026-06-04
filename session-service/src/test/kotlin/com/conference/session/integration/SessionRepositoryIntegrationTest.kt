package com.conference.session.integration

import com.conference.session.entity.SessionEntity
import com.conference.session.repository.SessionJpaRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
@SpringBootTest
@ActiveProfiles("jpa")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@EnabledIf(value = "isDockerAvailable", disabledReason = "Docker is not available")
class SessionRepositoryIntegrationTest {

    companion object {
        @JvmStatic
        fun isDockerAvailable(): Boolean {
            return try {
                DockerClientFactory.instance().isDockerAvailable
            } catch (e: Exception) {
                false
            }
        }

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("conference_test")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
        }
    }

    @Autowired
    private lateinit var repository: SessionJpaRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    @Order(1)
    fun `세션을 저장하고 조회할 수 있다`() {
        val entity = SessionEntity(
            title = "Kotlin Coroutines",
            speaker = "장호",
            description = "코루틴 심화",
            dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
        )
        val saved = repository.save(entity)
        val found = repository.findById(saved.id!!).orElseThrow()

        Assertions.assertEquals("Kotlin Coroutines", found.title)
        Assertions.assertEquals("장호", found.speaker)
        Assertions.assertEquals("코루틴 심화", found.description)
    }

    @Test
    @Order(2)
    fun `세션 목록을 조회할 수 있다`() {
        repository.save(SessionEntity(title = "Session 1", speaker = "Speaker 1"))
        repository.save(SessionEntity(title = "Session 2", speaker = "Speaker 2"))

        val sessions = repository.findAll()
        Assertions.assertEquals(2, sessions.size)
    }

    @Test
    @Order(3)
    fun `세션을 수정할 수 있다`() {
        val saved = repository.save(SessionEntity(title = "Old Title", speaker = "Speaker"))
        saved.title = "New Title"
        repository.save(saved)

        val found = repository.findById(saved.id!!).orElseThrow()
        Assertions.assertEquals("New Title", found.title)
    }

    @Test
    @Order(4)
    fun `세션을 삭제할 수 있다`() {
        val saved = repository.save(SessionEntity(title = "To Delete", speaker = "Speaker"))
        repository.deleteById(saved.id!!)

        Assertions.assertFalse(repository.findById(saved.id!!).isPresent)
    }

    @Test
    @Order(5)
    fun `존재하지 않는 세션 조회 시 빈 Optional을 반환한다`() {
        val result = repository.findById(999)
        Assertions.assertFalse(result.isPresent)
    }
}
