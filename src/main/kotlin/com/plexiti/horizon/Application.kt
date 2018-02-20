package com.plexiti.horizon

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@SpringBootApplication
open class Application {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(Application::class.java, *args)
        }

    }

}
