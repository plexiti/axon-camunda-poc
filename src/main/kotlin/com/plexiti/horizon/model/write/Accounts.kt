package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.model.AggregateLifecycle.apply

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Account(): AggregateIdentifiedBy<AccountId>() {

    private val logger = LoggerFactory.getLogger(Account::class.java)

    private var balance = 0F

    @CommandHandler
    constructor(command: CreateAccount): this() {
        logger.debug(command.toString())
        apply(AccountCreated(AccountId(UUID.randomUUID().toString()), command.name))
    }

    @CommandHandler
    fun handle(command: DebitFromAccount) {
        logger.debug(command.toString())
        val debit = if (command.amount > balance) balance else command.amount
        apply(AmountWithdrawn(command.accountId, debit))
    }

    @CommandHandler
    fun handle(command: CreditToAccount) {
        logger.debug(command.toString())
        apply(AmountCredited(command.accountId, command.amount))
    }

    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        logger.debug(event.toString())
        this.id = event.accountId
        logger.debug(this.toString())
    }

    @EventSourcingHandler
    protected fun on(event: AmountWithdrawn) {
        logger.debug(event.toString())
        this.balance -= event.amount
        logger.debug(this.toString())
    }

    @EventSourcingHandler
    protected fun on(event: AmountCredited) {
        logger.debug(event.toString())
        this.balance += event.amount
        logger.debug(this.toString())
    }

}
