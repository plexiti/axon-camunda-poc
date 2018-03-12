package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier
import javax.persistence.Entity
import javax.persistence.Id

class AccountId(id: String): Identifier<String>(id)

data class CreateAccount(val name: String)
data class VerifyOrCreateAccount(val name: String)
data class AccountCreated(@TargetAggregateIdentifier val accountId: AccountId)

data class WithdrawAmount(@TargetAggregateIdentifier val accountId: AccountId, val referenceId: String, val amount: Float)
data class AmountWithdrawn(@TargetAggregateIdentifier val accountId: AccountId, val referenceId: String, val amount: Float)

data class CreditAmount(@TargetAggregateIdentifier val accountId: AccountId, val referenceId: String, val amount: Float)
data class AmountCredited(@TargetAggregateIdentifier val accountId: AccountId, val referenceId: String, val amount: Float)

data class ChargeCreditCard(val accountId: AccountId, val referenceId: String, val amount: Float, val expired: Boolean)
data class CreditCardCharged(val accountId: AccountId, val referenceId: String, val amount: Float)

data class UpdateCreditCardDetails(val accountId: AccountId)
data class CreditCardDetailsUpdated(val accountId: AccountId)

data class UpdateCreditCardReminded(val accountId: AccountId)

data class DocumentAccountSummary(val accountId: AccountId)
@Entity data class AccountSummary(@Id val accountId: String, var balance: Float)
