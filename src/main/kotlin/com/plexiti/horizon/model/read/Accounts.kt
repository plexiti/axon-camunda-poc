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
        logger.debug(event.toString())
        entityManager.persist(AccountSummary(event.accountId.id, event.name, 0F))
    }

    @QueryHandler
    fun process(query: DocumentAccountSummary): AccountSummary {
        logger.debug(query.toString())
        return entityManager.createQuery("select a from AccountSummary a where a.name=:account")
            .setParameter("account", query.name)
            .singleResult as AccountSummary
    }

    @EventHandler
    protected fun on(event: AmountWithdrawn) {
        logger.debug(event.toString())
        val accountSummary = entityManager.find(AccountSummary::class.java, event.accountId.id)
        accountSummary.balance -= event.amount
    }

    @EventHandler
    protected fun on(event: AmountCredited) {
        logger.debug(event.toString())
        val accountSummary = entityManager.find(AccountSummary::class.java, event.accountId.id)
        accountSummary.balance += event.amount
    }

}

