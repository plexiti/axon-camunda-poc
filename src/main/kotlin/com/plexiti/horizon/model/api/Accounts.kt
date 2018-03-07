package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier
import javax.persistence.Entity
import javax.persistence.Id

class AccountId(id: String): Identifier<String>(id)

data class CreateAccount(val name: String)
data class AccountCreated(@TargetAggregateIdentifier val accountId: AccountId, val name: String)

data class DebitFromAccount(@TargetAggregateIdentifier val accountId: AccountId, val amount: Float)
data class AmountWithdrawn(@TargetAggregateIdentifier val accountId: AccountId, val amount: Float)

data class CreditToAccount(@TargetAggregateIdentifier val accountId: AccountId, val amount: Float)
data class AmountCredited(@TargetAggregateIdentifier val accountId: AccountId, val amount: Float)

data class ChargeCreditCard(val owner: String, val amount: Float, val expired: Boolean)
data class CreditCardCharged(val owner: String, val amount: Float)

data class UpdateCreditCardDetails(val owner: String)
data class CreditCardDetailsUpdated(val owner: String)

data class DocumentAccountSummary(val name: String)
@Entity data class AccountSummary(@Id val id: String, val name: String, var balance: Float)
