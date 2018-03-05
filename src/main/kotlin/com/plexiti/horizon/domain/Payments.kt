package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.TargetAggregateIdentifier

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Saga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.beans.factory.annotation.Autowired
import org.camunda.bpm.engine.ProcessEngine






/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Payment(): AggregateIdentifiedBy<PaymentId>() {

    internal var amount = 0F;

    @CommandHandler
    constructor(command: RetrievePayment): this() {
        apply(PaymentCreated(command.paymentId, command.amount))
    }

    @EventSourcingHandler
    protected fun on(event: PaymentCreated) {
        this.id = PaymentId(event.paymentId.id)
        this.amount = event.amount
    }

}

class PaymentId(id: String): Identifier<String>(id)

data class RetrievePayment(@TargetAggregateIdentifier val paymentId: PaymentId, val amount: Float)
data class PaymentCreated(val paymentId: PaymentId, val amount: Float)

@Saga
class PaymentFlow {

    @Autowired @Transient
    private lateinit var commandGateway: CommandGateway

    @Autowired @Transient
    private lateinit var processEngine: ProcessEngine

    @StartSaga @SagaEventHandler(associationProperty = "paymentId")
    fun handle(event: PaymentCreated) {
        val instance = processEngine.runtimeService
            .createProcessInstanceByKey("Payment")
            .businessKey(event.paymentId.id)
            .setVariable("amount", event.amount)
            .execute()
    }

}
