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

    private val logger = LoggerFactory.getLogger(Payment::class.java)

    private var amount = 0F
    private var covered = 0F

    private var orderId: OrderId? = null
    private var accountId: AccountId? = null

    @CommandHandler
    constructor(command: RequestPayment): this() {
        logger.debug(command.toString())
        apply(PaymentRequested(PaymentId(UUID.randomUUID().toString()), command.accountId, command.orderId, command.amount))
    }

    @CommandHandler
    fun handle(command: CoverPayment) {
        logger.debug(command.toString())
        if ((amount - covered - command.amount) > 0F) {
            apply(PaymentPartlyCovered(id, command.amount))
        } else {
            apply(PaymentFullyCovered(id, command.amount))
        }
    }

    @CommandHandler
    fun handle(command: FinishPayment) {
        logger.debug(command.toString())
        if ((amount - covered) > 0F) {
            apply(PaymentNotReceived(id, accountId!!, amount - covered))
        } else {
            apply(PaymentReceived(id, accountId!!, covered))
        }
    }

    @EventSourcingHandler
    protected fun on(event: PaymentRequested) {
        this.id = event.paymentId
        this.orderId = event.orderId
        this.accountId = event.accountId
        this.amount = event.amount
    }

    @EventSourcingHandler
    protected fun on(event: PaymentPartlyCovered) {
        this.id = event.paymentId
        this.covered += event.amount
    }

    @EventSourcingHandler
    protected fun on(event: PaymentFullyCovered) {
        this.id = event.paymentId
        this.covered += event.amount
    }

    @EventSourcingHandler
    protected fun on(event: PaymentReceived) {
    }

    @EventSourcingHandler
    protected fun on(event: PaymentNotReceived) {
    }

}

