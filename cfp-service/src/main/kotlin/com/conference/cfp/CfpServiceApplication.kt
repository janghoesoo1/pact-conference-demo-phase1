package com.conference.cfp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.conference"])
class CfpServiceApplication

fun main(args: Array<String>) {
    runApplication<CfpServiceApplication>(*args)
}
