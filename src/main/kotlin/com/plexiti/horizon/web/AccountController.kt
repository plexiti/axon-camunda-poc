package com.plexiti.horizon.web

import com.plexiti.horizon.model.api.*
import org.springframework.stereotype.Controller
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.GenericQueryMessage
import org.axonframework.queryhandling.QueryBus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Controller
@RequestMapping("/accounts")
class AccountController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @Autowired
    private lateinit var queryBus: QueryBus

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun accounts(@RequestParam(value = "name", required = true) name: String): ResponseEntity<CreateAccount> {
        val command = CreateAccount(name)
        try {
            commandGateway.send<CreateAccount>(command)
            return ResponseEntity(HttpStatus.OK)
        } catch (e: RuntimeException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    @RequestMapping("/{name}/credit", method = arrayOf(RequestMethod.PUT)) @ResponseBody
    fun credit(@PathVariable(value = "name", required = true) name: String, @RequestParam(value = "amount", required = true) amount: Float): ResponseEntity<CreditAmount> {
        val command = CreditAmount(AccountId(name), amount)
        try {
            commandGateway.send<CreditAmount>(command)
            return ResponseEntity(HttpStatus.OK)
        } catch (e: RuntimeException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}