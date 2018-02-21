package com.plexiti.horizon.model

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier
import org.axonframework.commandhandling.model.AggregateRoot
import java.io.Serializable
import org.axonframework.commandhandling.model.AggregateIdentifier

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler




/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@AggregateRoot
class Account(): Serializable {

    @AggregateIdentifier
    private lateinit var id: AccountId

    @CommandHandler
    constructor(command: CreateAccount): this() {
        apply(AccountCreated(command.accountId, command.name))
    }


    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        this.id = AccountId(event.accountId)
    }

}

data class AccountId(val id: String)

data class CreateAccount(@TargetAggregateIdentifier val accountId: String, val name: String)
data class AccountCreated(val accountId: String, val name: String)
