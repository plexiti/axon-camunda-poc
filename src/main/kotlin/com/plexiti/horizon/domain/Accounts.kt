package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.commandhandling.model.AggregateNotFoundException
import org.axonframework.commandhandling.model.Repository
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Account(): AggregateIdentifiedBy<AccountId>() {

    internal lateinit var name: String

    @CommandHandler
    constructor(command: CreateAccount): this() {
        apply(AccountCreated(command.accountId, command.name))
    }

    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        this.id = AccountId(event.accountId.id)
        this.name = event.name
    }

    @EventSourcingHandler
    protected fun on(event: AccountRenamed) {
        this.name = event.name
    }

}

class AccountId(id: String): Identifier<String>(id)

data class CreateAccount(@TargetAggregateIdentifier val accountId: AccountId, val name: String)
data class AccountCreated(val accountId: AccountId, val name: String)
data class CheckBalance(@TargetAggregateIdentifier val accountId: AccountId)

@Service
class AccountService() {

    private val logger = LoggerFactory.getLogger(Account::class.java)

    private lateinit var accounts: Repository<Account>

    constructor(accounts: Repository<Account>): this() {
        this.accounts = accounts
    }

    @CommandHandler
    fun handle(command: RenameAccount) {
        try {
            val account = accounts.load(command.accountId.id)
            if (account != null) {
                account.execute { a ->
                    if (a.name != "testAccountRenamed")
                        apply(AccountRenamed(a.id, command.name))
                }
            }
        } catch (e: AggregateNotFoundException) {
            //
        }
    }

    @CommandHandler
    fun handle(command: CheckBalance) {
        logger.debug(command.toString())
    }

    fun validate(command: RenameAccount): List<Any>? {
        try {
            return listOf(accounts.load(command.accountId.id))
        } catch (e: AggregateNotFoundException) {
            return null
        }
    }

}

data class RenameAccount(@TargetAggregateIdentifier val accountId: AccountId, val name: String)
data class AccountRenamed(val accountId: AccountId, val name: String)
