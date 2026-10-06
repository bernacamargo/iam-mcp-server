package io.github.bernacamargo.iam

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IamMcpServerApplication

fun main(args: Array<String>) {
    runApplication<IamMcpServerApplication>(*args)
}
