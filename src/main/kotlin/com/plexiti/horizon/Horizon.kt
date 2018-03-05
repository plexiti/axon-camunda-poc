package com.plexiti.horizon

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication
@ComponentScan("com.plexiti", "org.camunda")
class Horizon {

    private val logger = LoggerFactory.getLogger(Horizon::class.java)

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Horizon::class.java, *args)
        }

    }

}
