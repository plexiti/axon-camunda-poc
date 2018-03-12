package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentId(id: String): Identifier<String>(id)

data class RequestPayment(val accountId: AccountId, val orderId: OrderId, val amount: Float)
data class PaymentRequested(val paymentId: PaymentId, val accountId: AccountId, val orderId: OrderId, val amount: Float)

data class CoverPayment(@TargetAggregateIdentifier val paymentId: PaymentId, val amount: Float)
data class PaymentPartlyCovered(@TargetAggregateIdentifier val paymentId: PaymentId, val amount: Float)
data class PaymentReceived(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)

data class CancelPayment(@TargetAggregateIdentifier val paymentId: PaymentId)
data class PaymentCanceled(@TargetAggregateIdentifier val paymentId: PaymentId, val accountId: AccountId, val amount: Float)
