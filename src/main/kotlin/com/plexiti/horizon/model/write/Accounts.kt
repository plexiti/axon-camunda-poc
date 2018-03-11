package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.commandhandling.model.AggregateNotFoundException
import org.axonframework.commandhandling.model.Repository

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.axonframework.eventsourcing.CachingEventSourcingRepository
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition
import org.axonframework.eventsourcing.EventSourcingRepository
import org.springframework.context.annotation.Bean
import org.axonframework.eventsourcing.eventstore.EventStore




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
        apply(AccountCreated(AccountId(command.name)))
    }

    @CommandHandler
    fun handle(command: WithdrawAmount) {
        logger.debug(command.toString())
        val debit = if (command.amount > balance) balance else command.amount
        apply(AmountWithdrawn(command.accountId, command.referenceId, debit))
    }

    @CommandHandler
    fun handle(command: CreditAmount) {
        logger.debug(command.toString())
        apply(AmountCredited(command.accountId, command.referenceId, command.amount))
    }

    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        this.id = event.accountId
    }

    @EventSourcingHandler
    protected fun on(event: AmountWithdrawn) {
        this.balance -= event.amount
    }

    @EventSourcingHandler
    protected fun on(event: AmountCredited) {
        this.balance += event.amount
    }

}

@Component
class AccountService(
    private var accountRepository: Repository<Account>,
    private val commandGateway: CommandGateway
) {

    private val logger = LoggerFactory.getLogger(AccountService::class.java)

    @CommandHandler
    fun handle(command: VerifyOrCreateAccount) {
        logger.debug(command.toString())
        try {
            accountRepository.load(command.name)
        } catch (exception: AggregateNotFoundException) {
            commandGateway.sendAndWait<CreateAccount>(CreateAccount(command.name))
        }
    }

}

@Configuration
private class Configuration {

    @Autowired
    private lateinit var eventStore: EventStore

    @Bean
    fun accountRepository(): Repository<Account> {
        return EventSourcingRepository(Account::class.java, eventStore)
    }

}