package com.plexiti.horizon.model

import com.plexiti.horizon.model.api.*
import com.plexiti.integration.*
import org.axonframework.eventhandling.saga.EndSaga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import org.slf4j.LoggerFactory

@Saga
class OrderSaga: Flow() {

    protected val logger = LoggerFactory.getLogger(OrderSaga::class.java)

    private lateinit var orderId: OrderId
    private lateinit var customer: AccountId
    private var orderSum: Float = 0F

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    fun on(event: OrderPlaced) {
        logger.debug(event.toString())
        orderId = event.orderId
        customer = event.customer
        orderSum = event.sum
        correlateEventToFlow(event, orderId.id)
    }

    @SagaEventHandler(associationProperty = "orderId")
    fun on(event: PaymentCreated) {
        SagaLifecycle.associateWith("paymentId", event.paymentId.id)
    }

    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentReceived) {
        logger.debug(event .toString())
        correlateEventToFlow(event)
    }

    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentNotReceived) {
        logger.debug(event.toString())
        correlateEventToFlow(event)
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    fun on (event: OrderFulfilled) {
        logger.debug(event.toString())
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    fun on (event: OrderNotFulfilled) {
        logger.debug(event.toString())
    }

    @FlowCommandFactory
    fun retrievePayment(): RetrievePayment {
        val command = RetrievePayment(customer, orderId, orderSum)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun verifyOrCreateAccount(): VerifyOrCreateAccount {
        val command = VerifyOrCreateAccount(customer.id)
        logger.debug(command.toString())
        return command
    }

    @FlowEventFactory
    fun orderFulfilled(): OrderFulfilled {
        return OrderFulfilled(orderId)
    }

    @FlowEventFactory
    fun orderNotFulfilled(): OrderNotFulfilled {
        return OrderNotFulfilled(orderId)
    }

}