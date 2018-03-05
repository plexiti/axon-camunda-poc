package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import com.plexiti.generics.flow.CommandIssued
import com.plexiti.generics.flow.SagaMessageFactory
import org.axonframework.commandhandling.*

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Saga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.beans.factory.annotation.Autowired
import org.camunda.bpm.engine.ProcessEngine
import org.slf4j.LoggerFactory
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure


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

    private val logger = LoggerFactory.getLogger(PaymentFlow::class.java)

    @Autowired @Transient
    private lateinit var commandBus: CommandBus

    @Autowired @Transient
    private lateinit var processEngine: ProcessEngine

    @StartSaga @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentCreated) {
        val instance = processEngine.runtimeService
            .startProcessInstanceByMessage(event.javaClass.canonicalName)
        SagaLifecycle.associateWith("processInstanceId", instance.processInstanceId)
    }

    @StartSaga @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: CommandIssued) {
        val clazz = Class.forName(event.commandName).kotlin
        val factoryMethod = this::class.memberFunctions.find {
            val returnType = it.returnType.jvmErasure
            val annotation = it.findAnnotation<SagaMessageFactory>() != null
            val returnTypeOk = returnType.equals(clazz)
            returnTypeOk && annotation
        }!!
        val command = factoryMethod.call(this)

        val commandMessage = GenericCommandMessage(command)
        commandBus.dispatch(commandMessage)
    }

    @SagaMessageFactory
    fun createCheckBalance(): CheckBalance {
        return CheckBalance(AccountId("someAccount"))
    }

}
