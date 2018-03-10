package com.plexiti.horizon.model

import com.plexiti.horizon.model.api.*
import com.plexiti.integration.*
import org.axonframework.eventhandling.saga.EndSaga
import org.axonframework.eventhandling.saga.SagaEventHandler
import org.axonframework.eventhandling.saga.SagaLifecycle
import org.axonframework.eventhandling.saga.StartSaga
import org.axonframework.spring.stereotype.Saga
import org.slf4j.LoggerFactory

// TODO inheritance: replace abstract class with something better

@Saga
class PaymentSaga: Flow() {

    protected val logger = LoggerFactory.getLogger(PaymentSaga::class.java)

    private lateinit var paymentId: PaymentId
    private lateinit var accountId: AccountId
    private var paymentAmount: Float = 0F
    private var creditAvailable = 0F
    private var amountWithdrawn: Float = 0F
    private var creditCardExpired = false

    @StartSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentCreated) {
        logger.debug(event.toString())
        paymentId = event.paymentId
        accountId = event.accountId
        paymentAmount = event.amount
        creditCardExpired = accountId.id == "kermit" // Kermit's card is always expired :-)
        SagaLifecycle.associateWith("accountId", accountId.id)
        correlateEventToFlow(event, paymentId.id)
    }

    @SagaEventHandler(associationProperty = "accountId")
    fun on(event: CreditCardDetailsUpdated) {
        logger.debug(event.toString())
        creditCardExpired = false
        correlateEventToFlow(event)
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentReceived) {
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "paymentId")
    fun on(event: PaymentNotReceived) {
    }

    // TODO alternatives: e.g. mirror all member properties

    override fun bindValuesToFlow(): Map<String, Any> {
        return mapOf (
                "creditAvailable" to (creditAvailable > 0F),
                "creditFullyCovering" to (creditAvailable >= paymentAmount)
        )
    }

    // Construct Messages triggered by Flow Engine
    // TODO alternatives: e.g. message class constructors

    @FlowQueryFactory(responseType = AccountSummary::class)
    fun checkBalance(): DocumentAccountSummary {
        val query = DocumentAccountSummary(accountId)
        logger.debug(query.toString())
        return query
    }

    @FlowResponseHandler
    fun handle(accountSummary: AccountSummary) {
        logger.debug(accountSummary.toString())
        creditAvailable = accountSummary.balance
        amountWithdrawn = if (creditAvailable > paymentAmount) paymentAmount else creditAvailable
    }

    @FlowCommandFactory
    fun chargeCreditCard(): ChargeCreditCard {
        val command = ChargeCreditCard(accountId, paymentAmount - amountWithdrawn, creditCardExpired)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun withdrawAmount(): WithdrawAmount {
        val command = WithdrawAmount(accountId, amountWithdrawn)
        logger.debug(command.toString())
        return command
    }

    @FlowCommandFactory
    fun creditAmount(): CreditAmount {
        val command = CreditAmount(accountId, amountWithdrawn)
        logger.debug(command.toString())
        return command
    }

    @FlowEventFactory
    fun paymentReceived(): PaymentReceived {
        val event = PaymentReceived(paymentId, accountId, paymentAmount)
        logger.debug(event.toString())
        return event
    }

    @FlowEventFactory
    fun paymentNotReceived(): PaymentNotReceived {
        val event = PaymentNotReceived(paymentId, accountId, paymentAmount - amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @FlowEventFactory
    fun paymentFullyCoveredByAccount(): PaymentFullyCoveredByAccount {
        val event = PaymentFullyCoveredByAccount(paymentId, accountId, amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @FlowEventFactory
    fun paymentPartlyCoveredByAccount(): PaymentPartlyCoveredByAccount {
        val event = PaymentPartlyCoveredByAccount(paymentId, accountId, amountWithdrawn)
        logger.debug(event.toString())
        return event
    }

    @FlowEventFactory
    fun updateCreditCardReminded(): UpdateCreditCardReminded {
        val event = UpdateCreditCardReminded(accountId)
        logger.debug(event.toString())
        return event
    }

}