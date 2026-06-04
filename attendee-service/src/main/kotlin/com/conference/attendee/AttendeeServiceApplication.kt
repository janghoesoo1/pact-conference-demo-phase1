package com.conference.attendee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.conference"])
class AttendeeServiceApplication

fun main(args: Array<String>) {
    runApplication<AttendeeServiceApplication>(*args)
}
