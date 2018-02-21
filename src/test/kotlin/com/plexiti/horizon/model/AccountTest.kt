package com.plexiti.horizon.model

import com.plexiti.horizon.UnitTest
import org.axonframework.test.aggregate.AggregateTestFixture
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Category(UnitTest::class)
class AccountTest {

    private lateinit var account: AggregateTestFixture<Account>

    @Before
    fun setUp() {
        account = AggregateTestFixture(Account::class.java)
    }

    @Test
    fun testCreateAccount() {
        account.givenNoPriorActivity()
            .`when`(CreateAccount("accountId", "testAccount"))
            .expectEvents(AccountCreated("accountId", "testAccount"))
    }

}