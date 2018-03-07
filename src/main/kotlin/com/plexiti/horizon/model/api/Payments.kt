package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class PaymentId(id: String): Identifier<String>(id)

data class RetrievePayment(val account: String, val amount: Float)
data class PaymentCreated(val paymentId: PaymentId, val account: String, val amount: Float)

data class PaymentReceived(@TargetAggregateIdentifier val paymentId: PaymentId, val account: String, val amount: Float)
