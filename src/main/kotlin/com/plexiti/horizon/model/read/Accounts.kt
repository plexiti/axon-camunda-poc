package com.plexiti.horizon.model.read

import com.plexiti.horizon.model.api.*
import org.axonframework.eventhandling.EventHandler
import org.axonframework.queryhandling.QueryHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.persistence.EntityManager

@Component
class Accounts(private val entityManager: EntityManager) {

    private val logger = LoggerFactory.getLogger(Accounts::class.java)

    @EventHandler
    fun on(event: AccountCreated) {
        entityManager.persist(AccountSummary(event.accountId.id, 0F))
    }

    @QueryHandler
    fun process(query: DocumentAccountSummary): AccountSummary {
        logger.debug(query.toString())
        return entityManager.find(AccountSummary::class.java, query.accountId.id)
    }

    @EventHandler
    protected fun on(event: AmountWithdrawn) {
        val accountSummary = entityManager.find(AccountSummary::class.java, event.accountId.id)
        accountSummary.balance -= event.amount
    }

    @EventHandler
    protected fun on(event: AmountCredited) {
        val accountSummary = entityManager.find(AccountSummary::class.java, event.accountId.id)
        accountSummary.balance += event.amount
    }

}

