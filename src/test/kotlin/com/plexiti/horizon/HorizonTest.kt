package com.plexiti.horizon

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Category(IntegrationTest::class)
class HorizonTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun testAbout() {
        Assertions.assertThat(this.restTemplate).isNotNull();
    }

}