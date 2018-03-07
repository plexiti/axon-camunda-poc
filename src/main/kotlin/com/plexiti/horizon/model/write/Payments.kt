package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.*
import org.axonframework.commandhandling.*
import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Payment(): AggregateIdentifiedBy<PaymentId>() {

    private val logger = LoggerFactory.getLogger(Account::class.java)

    private var received = false
    private var failed = false

    @CommandHandler
    constructor(command: RetrievePayment): this() {
        logger.debug(command.toString())
        apply(PaymentCreated(PaymentId(UUID.randomUUID().toString()), command.accountId, command.amount))
    }

    @EventSourcingHandler
    protected fun on(event: PaymentCreated) {
        logger.debug(event.toString())
        this.id = event.paymentId
        logger.debug(this.toString())
    }

    @EventSourcingHandler
    protected fun on(event: PaymentNotReceived) {
        logger.debug(event.toString())
        this.failed = true
        logger.debug(this.toString())
    }

    @EventSourcingHandler
    protected fun on(event: PaymentReceived) {
        logger.debug(event.toString())
        this.received = true
        logger.debug(this.toString())
    }

}

