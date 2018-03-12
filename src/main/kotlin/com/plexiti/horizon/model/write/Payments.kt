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
            apply(PaymentPartlyCovered(id, covered + command.amount))
        } else {
            apply(PaymentReceived(id, accountId!!, amount))
        }
    }

    @CommandHandler
    fun handle(command: CancelPayment) {
        logger.debug(command.toString())
        apply(PaymentCanceled(id, accountId!!, amount - covered))
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
        this.covered = event.amount
    }

    @EventSourcingHandler
    protected fun on(event: PaymentReceived) {
        this.covered = event.amount
    }

    @EventSourcingHandler
    protected fun on(event: PaymentCanceled) {
    }

}

