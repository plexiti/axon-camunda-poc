package com.plexiti.horizon.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import java.util.concurrent.atomic.AtomicLong
import com.plexiti.generics.web.Resource
import com.plexiti.horizon.domain.PaymentId
import com.plexiti.horizon.domain.RetrievePayment
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import java.util.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Resource
data class Payment(val id: String, val amount: Float)

@Controller
@RequestMapping("/payments")
class PaymentController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @RequestMapping(method = arrayOf(RequestMethod.GET)) @ResponseBody
    fun payments(@RequestParam(value = "amount", required = true) amount: Float): RetrievePayment {
        val command = RetrievePayment(PaymentId(UUID.randomUUID().toString()), amount)
        commandGateway.send<RetrievePayment>(command)
        return command
    }

}