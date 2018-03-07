package com.plexiti.horizon.model

import com.plexiti.horizon.model.api.*
import com.plexiti.integration.*
import org.axonframework.eventhandling.saga.EndSaga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga

@Saga
class PaymentSaga: CamundaSaga() {

    private lateinit var accountId: AccountId
    private var paymentAmount: Float = 0F
    private var creditAvailable = 0F
    private var amountWithdrawn: Float = 0F
    private var creditCardExpired = false

    @StartSaga
    @SagaEventHandler(associationProperty = "accountId")
    fun on(event: PaymentCreated) {
        logger.debug(event.toString())
        businessProcessKey = event.paymentId
        accountId = event.accountId
        paymentAmount = event.amount
        creditCardExpired = accountId.id == "kermit"
        attachProcessInstance("PaymentSaga")
    }

    @SagaQueryFactory(responseType = AccountSummary::class)
    fun checkBalance(): DocumentAccountSummary {
        val query = DocumentAccountSummary(accountId)
        logger.debug(query.toString())
        return query
    }

    @SagaCommandFactory
    fun chargeCreditCard(): ChargeCreditCard {
        val command = ChargeCreditCard(accountId, paymentAmount - amountWithdrawn, creditCardExpired)
        logger.debug(command.toString())
        return command
    }

    @SagaCommandFactory
    fun withdrawAmount(): WithdrawAmount {
        val command = WithdrawAmount(accountId, amountWithdrawn)
        logger.debug(command.toString())
        return command
    }

    @SagaCommandFactory
    fun creditAmount(): CreditAmount {
        val command = CreditAmount(accountId, amountWithdrawn)
        logger.debug(command.toString())
        return command
    }

    @SagaEventFactory
    fun paymentReceived(): PaymentReceived {
        val event = PaymentReceived(businessProcessKey, accountId, paymentAmount)
        logger.debug(event.toString())
        return event
    }

    @SagaEventFactory
    fun paymentNotReceived(): PaymentNotReceived {
        val event = PaymentNotReceived(businessProcessKey, accountId, paymentAmount - amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @SagaEventFactory
    fun paymentFullyCoveredByAccount(): PaymentFullyCoveredByAccount {
        val event = PaymentFullyCoveredByAccount(businessProcessKey, accountId, amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @SagaEventFactory
    fun paymentPartlyCoveredByAccount(): PaymentPartlyCoveredByAccount {
        val event = PaymentPartlyCoveredByAccount(businessProcessKey, accountId, amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @SagaEventFactory
    fun updateCreditCardReminded(): UpdateCreditCardReminded {
        val event = UpdateCreditCardReminded(accountId)
        logger.debug(event.toString())
        return event
    }

    @SagaResponseHandler
    fun handle(accountSummary: AccountSummary) {
        logger.debug(accountSummary.toString())
        creditAvailable = accountSummary.balance
        amountWithdrawn = if (creditAvailable > paymentAmount) paymentAmount else creditAvailable
    }

    @SagaEventHandler(associationProperty = "accountId")
    fun on(event: CreditCardDetailsUpdated) {
        logger.debug(event.toString())
        creditCardExpired = false
        messageProcessInstance(event::class.java.name)
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "accountId")
    fun on(event: PaymentReceived) {
        logger.debug(event.toString())
    }

    override fun variables(): Map<String, Any> {
        return mapOf (
                "creditAvailable" to (creditAvailable > 0F),
                "creditFullyCovering" to (creditAvailable >= paymentAmount)
        )
    }

}