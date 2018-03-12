package com.plexiti.horizon

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication
@ComponentScan("com.plexiti", "org.camunda")
class Horizon {

    companion object Application {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Horizon::class.java, *args)
        }

    }

}
