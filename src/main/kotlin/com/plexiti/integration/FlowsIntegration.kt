package com.plexiti.integration

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
import org.springframework.beans.factory.annotation.Autowired
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

abstract class Flow {

    @Autowired
    @Transient
    private lateinit var commandBus: CommandBus

    @Autowired
    @Transient
    private lateinit var eventBus: EventBus

    @Autowired
    @Transient
    private lateinit var queryBus: QueryBus

    protected lateinit var correlationKey: String

    @SagaEventHandler(associationProperty = "correlationKey")
    fun on(event: CommandIssued) {

        val message = createMessage(event.command)

        commandBus.dispatch(GenericCommandMessage(message), object: CommandCallback<Any, Any> {

            override fun onSuccess(commandMessage: CommandMessage<out Any>?, result: Any?) {
                val succeeded = GenericEventMessage(CommandSucceeded(event.executionId, variables()))
                eventBus.publish(succeeded)
            }

            override fun onFailure(commandMessage: CommandMessage<out Any>?, cause: Throwable) {
                val failed = GenericEventMessage(CommandFailed(event.executionId, cause::class.java.canonicalName, cause.message))
                eventBus.publish(failed)
            }

        })

    }

    @SagaEventHandler(associationProperty = "correlationKey")
    fun on(event: EventRaised) {

        val message = createMessage(event.event)
        eventBus.publish(GenericEventMessage(message))

    }

    @SagaEventHandler(associationProperty = "correlationKey")
    fun on(event: QueryRequested) {

        val message = createMessage(event.query)
        val q = queryBus.query(GenericQueryMessage(message, returnType(message::class).java))

        val result = q.get()
        handleResponse(result)

        val responded = GenericEventMessage(QueryResponded(event.executionId, variables()))
        eventBus.publish(responded)

    }

    private fun createMessage(type: String): Any {
        val re = Class.forName(type).kotlin
        val factoryMethod = this::class.memberFunctions.find {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val isSagaCommandFactory = it.findAnnotation<FlowCommandFactory>() != null
            val isSagaEventFactory = it.findAnnotation<FlowEventFactory>() != null
            val isSagaQueryFactory = it.findAnnotation<FlowQueryFactory>() != null
            returnTypeOfMethod.equals(re) && (isSagaCommandFactory || isSagaEventFactory || isSagaQueryFactory)
        }!!
        return factoryMethod.call(this)!!
    }

    private fun handleResponse(response: Any) {
        val responseHandlingMethod = this::class.memberFunctions.find {
            val parameterType = if (it.parameters.size == 2) it.parameters[1].type else null
            val isSagaResponseHandler = it.findAnnotation<FlowResponseHandler>() != null
            parameterType != null && parameterType.jvmErasure.equals(response::class) && isSagaResponseHandler
        }
        if (responseHandlingMethod == null)
            throw IllegalArgumentException("No handler found for response $response!")
        responseHandlingMethod.call(this, response)
    }

    private fun returnType(type: KClass<*>): KClass<*> {
        this::class.memberFunctions.forEach {
            val returnTypeOfMethod = it.returnType.jvmErasure
            val sagaQueryFactory = it.findAnnotation<FlowQueryFactory>()
            if (returnTypeOfMethod.equals(type) && sagaQueryFactory != null) {
                return sagaQueryFactory.responseType
            }
        }
        throw IllegalArgumentException()
    }

    protected fun correlate(event: Any, correlationKey: String? = null) {
        if (correlationKey != null) {
            this.correlationKey = correlationKey
            SagaLifecycle.associateWith("correlationKey", correlationKey)
        }
        val received = GenericEventMessage(EventReceived(this.correlationKey, event::class.java.canonicalName), variables())
        eventBus.publish(received)
    }

    abstract fun variables(): Map<String, Any>
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FlowCommandFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FlowEventFactory

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FlowQueryFactory(val responseType: KClass<*>)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class FlowResponseHandler

data class CommandIssued(val correlationKey: String, val executionId: String, val command: String)
data class CommandSucceeded(val executionId: String, val variables: Map<String, Any?>)
data class CommandFailed(val executionId: String, val errorCode: String, val errorMessage: String?)

data class EventRaised(val correlationKey: String, val executionId: String, val event: String)

data class QueryRequested(val correlationKey: String, val executionId: String, val query: String)
data class QueryResponded(val executionId: String, val variables: Map<String, Any?>)

data class EventReceived(val correlationKey: String, val event: String)
