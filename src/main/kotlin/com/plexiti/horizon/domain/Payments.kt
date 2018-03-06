package com.plexiti.horizon.domain

import com.plexiti.generics.domain.AggregateIdentifiedBy
import com.plexiti.generics.domain.Identifier
import com.plexiti.generics.flow.*
import com.plexiti.horizon.query.AccountSummary
import com.plexiti.horizon.query.CheckBalance
import org.axonframework.commandhandling.*
import org.axonframework.commandhandling.model.AggregateLifecycle.apply
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.queryhandling.GenericQueryMessage
import org.axonframework.queryhandling.QueryBus
import org.axonframework.spring.stereotype.Aggregate
import org.axonframework.spring.stereotype.Saga
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.ProcessEngineException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.reflect.KClass
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
        this.id = event.paymentId
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
    private lateinit var eventBus: EventBus

    @Autowired @Transient
    private lateinit var queryBus: QueryBus

    @Autowired @Transient
    private lateinit var processEngine: ProcessEngine

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: MessageRequested) {
        val message = createMessage(event.messageName)
        logger.debug(message.toString())
        if (isCommand(message)) {
            processCommand(event, message)
        } else if (isEvent(message)) {
            processEvent(event, message)
        } else {
            processQuery(event, message)
        }
    }

    private fun processCommand(event: MessageRequested, message: Any) {
        commandBus.dispatch(GenericCommandMessage(message), object: CommandCallback<Any,Any> {

            override fun onSuccess(commandMessage: CommandMessage<out Any>?, result: Any?) {
                var done = false
                do {
                    try {
                        Thread.sleep(25)
                        processEngine.runtimeService.signal(event.executionId, null, null, variables())
                        done = true
                    } catch (e: ProcessEngineException) {
                    }
                } while (!done)
            }

            override fun onFailure(commandMessage: CommandMessage<out Any>?, cause: Throwable) {
                var done = false
                do {
                    try {
                        Thread.sleep(25)
                        processEngine.runtimeService.signal(event.executionId, cause::class.java.canonicalName, cause.message, null)
                        done = true
                    } catch (e: ProcessEngineException) {
                    }
                } while (!done)
            }

        })
    }

    private fun processEvent(event: MessageRequested, message: Any) {
        eventBus.publish(GenericEventMessage(message))
    }

    private fun processQuery(event: MessageRequested, query: Any) {
        val q = queryBus.query(GenericQueryMessage(query, returnType(query::class).java))
        q.whenCompleteAsync { result, exception ->
            handleResponse(result)
            var done = false
            do {
                try {
                    Thread.sleep(25)
                    processEngine.runtimeService.signal(event.executionId, null, null, variables())
                    done = true
                } catch (e: ProcessEngineException) {
                }
            } while (!done)
            logger.debug(result.toString())
        }
    }

    private fun isCommand(message: Any): Boolean {
        return this::class.memberFunctions.find {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val isSagaCommandFactory = it.findAnnotation<SagaCommandFactory>() != null
            returnTypeOfMethod.equals(message::class) && isSagaCommandFactory
        } != null
    }

    private fun isEvent(message: Any): Boolean {
        return this::class.memberFunctions.find {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val isSagaEventFactory = it.findAnnotation<SagaEventFactory>() != null
            returnTypeOfMethod.equals(message::class) && isSagaEventFactory
        } != null
    }

    private fun createMessage(type: String): Any {
        val re = Class.forName(type).kotlin
        val factoryMethod = this::class.memberFunctions.find {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val isSagaCommandFactory = it.findAnnotation<SagaCommandFactory>() != null
            val isSagaEventFactory = it.findAnnotation<SagaEventFactory>() != null
            val isSagaQueryFactory = it.findAnnotation<SagaQueryFactory>() != null
            returnTypeOfMethod.equals(re) && (isSagaCommandFactory || isSagaEventFactory || isSagaQueryFactory)
        }!!
        return factoryMethod.call(this)!!
    }

    private fun handleResponse(response: Any) {
        val responseHandlingMethod = this::class.memberFunctions.find {
            val parameterType = if (it.parameters.size == 2) it.parameters[1].type else null
            val isSagaResponseHandler = it.findAnnotation<SagaResponseHandler>() != null
            parameterType != null && parameterType.jvmErasure.equals(response::class) && isSagaResponseHandler
        }
        if (responseHandlingMethod == null)
            throw IllegalArgumentException("No handler found for response $response!")
        responseHandlingMethod.call(this, response)
    }

    private fun returnType(type: KClass<*>): KClass<*> {
        this::class.memberFunctions.forEach {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val sagaQueryFactory = it.findAnnotation<SagaQueryFactory>()
            if (returnTypeOfMethod.equals(type) && sagaQueryFactory != null) {
                return sagaQueryFactory.responseType
            }
        }
        throw IllegalArgumentException()
    }

    private fun attachProcessInstance(type: String) {
        val instance = processEngine.runtimeService
            .startProcessInstanceByKey(type, variables())
        SagaLifecycle.associateWith("processInstanceId", instance.processInstanceId)
    }

    fun variables(): Map<String, Any> { // TODO generify
        return mapOf (
            "account" to account,
            "amount" to amount,
            "creditAvailable" to creditAvailable
        )
    }

    // This is the only saga code necessary once a proper integration Axon / Camunda exists

    private lateinit var account: String
    private var amount: Float = 0F
    private var creditAvailable = false

    @StartSaga @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentCreated) {
        account = event.account
        amount = event.amount
        attachProcessInstance("Payment")
    }

    @SagaQueryFactory(responseType = AccountSummary::class)
    fun createCheckBalance(): CheckBalance {
        return CheckBalance(account)
    }

    @SagaCommandFactory
    fun createChargeCreditCard(): ChargeCreditCard {
        return ChargeCreditCard(account, amount)
    }

    @SagaResponseHandler
    fun handle(accountSummary: AccountSummary) {
        creditAvailable = accountSummary.balance > 0
    }

}
