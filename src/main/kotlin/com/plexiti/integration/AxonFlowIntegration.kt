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
import org.springframework.stereotype.Component
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.jvmErasure

// TODO support explicit message names
// TODO alternatives: implement an Axon Saga Manager
// TODO inheritance: replace abstract class with something better
// One could e.g. hook in the saga event handlers into every saga
// annotated with @Flow

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class AxonFlowIntegration {

    @Autowired @Transient
    private lateinit var commandBus: CommandBus

    @Autowired @Transient
    private lateinit var eventBus: EventBus

    @Autowired @Transient
    private lateinit var queryBus: QueryBus

    @Autowired @Transient
    private lateinit var messageFactory: MessageFactory

    @Autowired @Transient
    private lateinit var responseHandler: ResponseHandler

    private lateinit var sagaAssociationId: String

    // TODO efficiency: instantiate just once
    @SagaEventHandler(associationProperty = "sagaAssociationId")
    internal fun on(event: FlowCommandIssued) {

        // TODO resilience: shutdown safety

        commandBus.dispatch(GenericCommandMessage(messageFactory.create(event.command, this)), object: CommandCallback<Any, Any> {

            override fun onSuccess(commandMessage: CommandMessage<out Any>?, result: Any?) {
                eventBus.publish(GenericEventMessage(FlowCommandSucceeded(event.flowAssociationId, bindValuesToFlow())))
            }

            override fun onFailure(commandMessage: CommandMessage<out Any>?, cause: Throwable) {
                eventBus.publish(GenericEventMessage(FlowCommandFailed(event.flowAssociationId, cause::class.java.canonicalName, cause.message)))
            }

        })

    }

    @SagaEventHandler(associationProperty = "sagaAssociationId")
    internal fun on(event: FlowEventRaised) {

        eventBus.publish(GenericEventMessage(messageFactory.create(event.event, this)))

    }

    @SagaEventHandler(associationProperty = "sagaAssociationId")
    internal fun on(event: FlowQueryRequested) {

        val query = messageFactory.create(event.query, this)
        val queryResponseClass = responseHandler.responseClass(query::class, this)

        // TODO resilience: asynchrony, shutdown safety

        responseHandler.handle(queryBus.query(GenericQueryMessage(query, queryResponseClass.java)).get(), this)
        eventBus.publish(GenericEventMessage(FlowQueryResponded(event.flowAssociationId, bindValuesToFlow())))

    }

    protected fun correlateEventToFlow(event: Any, sagaAssociationId: String? = null) {

        if (sagaAssociationId != null) {
            this.sagaAssociationId = sagaAssociationId
            SagaLifecycle.associateWith("sagaAssociationId", sagaAssociationId)
        }

        eventBus.publish(GenericEventMessage(FlowEventReceived(this.sagaAssociationId, event::class.java.canonicalName), bindValuesToFlow()))

    }

    protected abstract fun bindValuesToFlow(): Map<String, Any>
}

data class FlowCommandIssued(val sagaAssociationId: String, val flowAssociationId: String, val command: String)
data class FlowCommandSucceeded(val flowAssociationId: String, val variables: Map<String, Any?>)
data class FlowCommandFailed(val flowAssociationId: String, val errorCode: String, val errorMessage: String?)
data class FlowEventRaised(val sagaAssociationId: String, val flowAssociationId: String, val event: String)
data class FlowEventReceived(val sagaAssociationId: String, val event: String)
data class FlowQueryRequested(val sagaAssociationId: String, val flowAssociationId: String, val query: String)
data class FlowQueryResponded(val flowAssociationId: String, val variables: Map<String, Any?>)

@Component
internal class MessageFactory {

    private val factories: MutableMap<KClass<*>, SagaMessageFactory> = mutableMapOf()

    fun create(messageName: String, saga: Any): Any {
        return factory(saga).create(messageName, saga)
    }

    private fun factory(saga: Any): SagaMessageFactory {
        var factory = factories[saga::class]
        if (factory == null) {
            factory = SagaMessageFactory(saga::class)
            factories[saga::class] = SagaMessageFactory(saga::class)
        }
        return factory
    }

}

@Component
internal class ResponseHandler {

    private val handlers: MutableMap<KClass<*>, SagaResponseHandler> = mutableMapOf()

    fun responseClass(queryClass: Any, saga: Any): KClass<*> {
        return handler(saga).responseClass(queryClass)
    }

    fun handle(queryResponse: Any, saga: Any) {
        handler(saga).handle(queryResponse, saga)
    }

    private fun handler(saga: Any): SagaResponseHandler {
        var handler = handlers[saga::class]
        if (handler == null) {
            handler = SagaResponseHandler(saga::class)
            handlers[saga::class] = SagaResponseHandler(saga::class)
        }
        return handler
    }

}

internal class SagaMessageFactory(sagaClass: KClass<*>): Serializable {

    private val factories: Map<KClass<*>, KFunction<*>>
            = sagaClass.functions.filter {
        it.findAnnotation<FlowCommandFactory>() != null
            || it.findAnnotation<FlowEventFactory>() != null
            || it.findAnnotation<FlowQueryFactory>() != null
    }.associateBy {
        it.returnType.jvmErasure
    }

    fun create(messageName: String, saga: Any): Any {
        val messageClass = Class.forName(messageName).kotlin
        val factoryMethod = factories[messageClass]
                ?: throw IllegalStateException("No factory method defined for message class ${messageName}!")
        return factoryMethod.call(saga)!!
    }

}

internal class SagaResponseHandler(sagaClass: KClass<*>) {

    private val responseHandlers: Map<KClass<*>, KFunction<*>>
            = sagaClass.functions.filter {
        it.findAnnotation<FlowResponseHandler>() != null
    }.associateBy {
        if (it.parameters.size == 2) it.parameters[1].type.jvmErasure
        else throw IllegalStateException("Flow Response Handler ${it.name} must have a single parameter!")
    }

    private val responseTypes: Map<KClass<*>, KClass<*>>
            = sagaClass.functions.filter {
        it.findAnnotation<FlowQueryFactory>() != null
    }.associate {
        it.returnType.jvmErasure to it.findAnnotation<FlowQueryFactory>()!!.responseType
    }

    fun responseClass(queryClass: Any): KClass<*> {
        return responseTypes[queryClass]!!
    }

    fun handle(queryResponse: Any, saga: Any) {
        val handlerMethod = responseHandlers[queryResponse::class]
            ?: throw IllegalStateException("No handler method defined for query response class ${queryResponse::class}!")
        handlerMethod.call(saga, queryResponse)
    }

}
