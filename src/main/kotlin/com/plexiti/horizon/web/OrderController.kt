package com.plexiti.horizon.web

import com.plexiti.horizon.model.api.AccountId
import com.plexiti.horizon.model.api.PlaceOrder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Controller
@RequestMapping("/orders")
class OrderController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun payments(@RequestParam(value = "account", required = true) account: String, @RequestParam(value = "sum", required = true) sum: Float): ResponseEntity<PlaceOrder> {
        val command = PlaceOrder(AccountId(account), sum)
        commandGateway.send<PlaceOrder>(command)
        return ResponseEntity(HttpStatus.ACCEPTED)
    }

}