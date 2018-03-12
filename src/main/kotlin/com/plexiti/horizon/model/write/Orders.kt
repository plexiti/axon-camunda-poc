package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.model.AggregateLifecycle.apply

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Order(): AggregateIdentifiedBy<OrderId>() {

    private val logger = LoggerFactory.getLogger(Order::class.java)

    @CommandHandler
    constructor(command: PlaceOrder): this() {
        logger.debug(command.toString())
        apply(OrderPlaced(OrderId(UUID.randomUUID().toString()), command.customer, command.sum))
    }

    @CommandHandler
    fun handle(command: FinishOrder) {
        logger.debug(command.toString())
        if (command.success) {
            apply(OrderFulfilled(id))
        } else {
            apply(OrderNotFulfilled(id))
        }
    }

    @EventSourcingHandler
    protected fun on(event: OrderPlaced) {
        this.id = event.orderId
    }

    @EventSourcingHandler
    protected fun on(event: OrderFulfilled) {
    }

    @EventSourcingHandler
    protected fun on(event: OrderNotFulfilled) {
    }

}
