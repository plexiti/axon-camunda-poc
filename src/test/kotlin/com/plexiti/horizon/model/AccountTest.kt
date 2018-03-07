package com.plexiti.horizon.model

import com.plexiti.generics.UnitTest
import com.plexiti.horizon.model.api.AccountCreated
import com.plexiti.horizon.model.api.AccountId
import com.plexiti.horizon.model.api.CreateAccount
import com.plexiti.horizon.model.write.Account
import com.plexiti.horizon.model.write.AccountService
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
        account.registerAnnotatedCommandHandler(AccountService(account.repository))
    }

    @Test
    fun testCreateAccount() {
        account.givenNoPriorActivity()
            .`when`(CreateAccount("testAccount"))
            .expectEvents(AccountCreated(AccountId("accountId"), "testAccount"))
    }

}