package com.plexiti.horizon.model

import com.plexiti.generics.AggregateIdentifiedBy
import com.plexiti.generics.Identifier
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier
import org.axonframework.commandhandling.model.AggregateRoot

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler




/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@AggregateRoot
class Account(): AggregateIdentifiedBy<AccountId>() {

    @CommandHandler
    constructor(command: CreateAccount): this() {
        apply(AccountCreated(command.accountId, command.name))
    }

    @EventSourcingHandler
    protected fun on(event: AccountCreated) {
        this.id = AccountId(event.accountId)
    }

}

class AccountId(id: String): Identifier<String>(id)

data class CreateAccount(@TargetAggregateIdentifier val accountId: String, val name: String)
data class AccountCreated(val accountId: String, val name: String)
