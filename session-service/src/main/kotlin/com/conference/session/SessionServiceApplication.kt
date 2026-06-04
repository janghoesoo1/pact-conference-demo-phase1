package com.conference.session

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.conference"])
class SessionServiceApplication

fun main(args: Array<String>) {
    runApplication<SessionServiceApplication>(*args)
}
