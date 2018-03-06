package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import com.plexiti.generics.flow.CommandIssued
import com.plexiti.generics.flow.QueryRequested
import com.plexiti.generics.flow.SagaMessageFactory
import com.plexiti.horizon.query.AccountSummary
import com.plexiti.horizon.query.CheckBalance
import org.axonframework.commandhandling.*

import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.spring.stereotype.Saga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.queryhandling.GenericQueryMessage
import org.axonframework.queryhandling.QueryBus
import org.axonframework.spring.stereotype.Aggregate
import org.springframework.beans.factory.annotation.Autowired
import org.camunda.bpm.engine.ProcessEngine
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Aggregate
class Payment(): AggregateIdentifiedBy<PaymentId>() {

    @CommandHandler
    constructor(command: RetrievePayment): this() {
        apply(PaymentCreated(PaymentId(UUID.randomUUID().toString()), command.account, command.amount))
    }

    @EventSourcingHandler
    protected fun on(event: PaymentCreated) {
        this.id = PaymentId(event.paymentId.id)
    }

}

class PaymentId(id: String): Identifier<String>(id)

data class RetrievePayment(val account: String, val amount: Float)
data class PaymentCreated(val paymentId: PaymentId, val account: String, val amount: Float)

@Saga
class PaymentFlow {

    private val logger = LoggerFactory.getLogger(PaymentFlow::class.java)

    @Autowired @Transient
    private lateinit var commandBus: CommandBus

    @Autowired @Transient
    private lateinit var queryBus: QueryBus

    @Autowired @Transient
    private lateinit var processEngine: ProcessEngine

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: CommandIssued) {
        val command = createMessage(event.commandName)
        logger.debug(command.toString())
        commandBus.dispatch(GenericCommandMessage(command))
    }

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: QueryRequested) {
        val query = createMessage(event.queryName)
        logger.debug(query.toString())
        val q = queryBus.query(GenericQueryMessage(query, AccountSummary::class.java)) // TODO generify
        val result = q.get()
        logger.debug(result.toString())
    }

    private fun createMessage(type: String): Any {
        val re = Class.forName(type).kotlin
        val factoryMethod = this::class.memberFunctions.find {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val isSagaMessageFactory = it.findAnnotation<SagaMessageFactory>() != null
            returnTypeOfMethod.equals(re) && isSagaMessageFactory
        }!!
        return factoryMethod.call(this)!!
    }

    private fun createFlow(type: String) {
        val instance = processEngine.runtimeService
                .startProcessInstanceByKey(type)
        SagaLifecycle.associateWith("processInstanceId", instance.processInstanceId)
    }

    //

    private lateinit var account: String
    private var amount: Float = 0F

    @StartSaga @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentCreated) {
        account = event.account
        amount = event.amount
        createFlow("Payment")
    }

    @SagaMessageFactory
    fun createCheckBalance(): CheckBalance {
        return CheckBalance(account)
    }

}
