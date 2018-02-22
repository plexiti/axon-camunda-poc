package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier
import org.axonframework.commandhandling.model.Aggregate
import org.axonframework.commandhandling.model.AggregateRoot

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.commandhandling.model.AggregateNotFoundException
import org.axonframework.commandhandling.model.Repository
import org.axonframework.eventsourcing.EventSourcingHandler
import org.springframework.stereotype.Service


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@AggregateRoot
class Account(): AggregateIdentifiedBy<AccountId>() {

    internal lateinit var name: String private set

    @CommandHandler
    constructor(command: CreateAccount): this() {
        if (validate(command))
            apply(AccountCreated(command.accountId, command.name))
    }

    fun validate(command: CreateAccount): Boolean {
        return true
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

@Service
class AccountService() {

    private lateinit var accounts: Repository<Account>

    constructor(accounts: Repository<Account>): this() {
        this.accounts = accounts
    }

    @CommandHandler
    fun handle(command: RenameAccount) {
        val loaded = validate(command)
        if (loaded != null) {
            val account = loaded[0] as Aggregate<Account>
            account.execute { a ->
                if (a.name != "testAccountRenamed")
                    apply(AccountRenamed(a.id, command.name))
            }
        }
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
