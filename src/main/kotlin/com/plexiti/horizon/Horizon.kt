package com.plexiti.horizon

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication
open class Horizon {

    private val logger = LoggerFactory.getLogger(Horizon::class.java)

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Horizon::class.java, *args)
        }

    }

}
