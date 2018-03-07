package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentId(id: String): Identifier<String>(id)

data class RetrievePayment(val accountId: AccountId, val amount: Float)
data class PaymentCreated(val paymentId: PaymentId, val accountId: AccountId, val amount: Float)

data class PaymentFullyCoveredByAccount(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)
data class PaymentPartlyCoveredByAccount(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)

data class PaymentReceived(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)
data class PaymentNotReceived(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)
