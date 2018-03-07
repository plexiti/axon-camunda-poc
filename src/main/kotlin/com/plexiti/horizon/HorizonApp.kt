package com.plexiti.horizon

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication
@ComponentScan("com.plexiti", "org.camunda")
class HorizonApp {

    private val logger = LoggerFactory.getLogger(HorizonApp::class.java)

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(HorizonApp::class.java, *args)
        }

    }

}
