package com.plexiti.horizon.model.api

import org.axonframework.commandhandling.TargetAggregateIdentifier

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
class OrderId(id: String): Identifier<String>(id)

data class PlaceOrder(val customer: AccountId, val sum: Float)
data class OrderPlaced(@TargetAggregateIdentifier val orderId: OrderId, val customer: AccountId, val sum: Float)

data class FinishOrder(@TargetAggregateIdentifier val orderId: OrderId, val success: Boolean)
data class OrderFulfilled(@TargetAggregateIdentifier val orderId: OrderId)
data class OrderNotFulfilled(@TargetAggregateIdentifier val orderId: OrderId)
