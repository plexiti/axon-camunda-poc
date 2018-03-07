package com.plexiti.integration

import com.plexiti.horizon.model.PaymentSaga
import com.plexiti.horizon.model.api.PaymentId
import org.axonframework.commandhandling.CommandBus
import org.axonframework.commandhandling.CommandCallback
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.commandhandling.GenericCommandMessage
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.queryhandling.GenericQueryMessage
import org.axonframework.queryhandling.QueryBus
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.ProcessEngineException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

abstract class CamundaSaga {

    protected val logger = LoggerFactory.getLogger(PaymentSaga::class.java)
    @Autowired
    @Transient
    private lateinit var commandBus: CommandBus
    @Autowired
    @Transient
    private lateinit var eventBus: EventBus
    @Autowired
    @Transient
    private lateinit var queryBus: QueryBus
    @Autowired
    @Transient
    private lateinit var processEngine: ProcessEngine
    protected lateinit var businessProcessKey: PaymentId

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: CommandIssued) {

        val message = createMessage(event.messageName)

        commandBus.dispatch(GenericCommandMessage(message), object: CommandCallback<Any, Any> {

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

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: EventRaised) {

        val message = createMessage(event.messageName)
        eventBus.publish(GenericEventMessage(message))

    }

    @SagaEventHandler(associationProperty = "processInstanceId")
    fun on(event: QueryRequested) {

        val message = createMessage(event.messageName)
        val q = queryBus.query(GenericQueryMessage(message, returnType(message::class).java))
        val result = q.get()
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

    protected fun attachProcessInstance(type: String) {
        val instance = processEngine.runtimeService
            .startProcessInstanceByKey(type, businessProcessKey.id, variables())
        SagaLifecycle.associateWith("processInstanceId", instance.processInstanceId)
    }

    protected fun messageProcessInstance(message: String) {
        val instance = processEngine.runtimeService
                .correlateMessage(message, businessProcessKey.id)
    }

    abstract fun variables(): Map<String, Any>
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaCommandFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaEventFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaQueryFactory(val responseType: KClass<*>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SagaResponseHandler