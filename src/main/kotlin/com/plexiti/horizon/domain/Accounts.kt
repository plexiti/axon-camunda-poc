package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.commandhandling.model.Repository
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Account(): AggregateIdentifiedBy<AccountId>() {

    private val logger = LoggerFactory.getLogger(Account::class.java)

    @CommandHandler
    constructor(command: CreateAccount): this() {
        logger.debug(command.toString())
        apply(AccountCreated(AccountId(UUID.randomUUID().toString()), command.name))
    }

    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        logger.debug(event.toString())
        this.id = AccountId(event.accountId.id)
    }

}

class AccountId(id: String): Identifier<String>(id)

data class CreateAccount(val name: String)
data class AccountCreated(@TargetAggregateIdentifier val accountId: AccountId, val name: String)

@Service
class AccountService() {

    private lateinit var accounts: Repository<Account>

    private val logger = LoggerFactory.getLogger(Account::class.java)

    constructor(accounts: Repository<Account>): this() {
        this.accounts = accounts
    }

}
