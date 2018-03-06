package com.plexiti.horizon.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestMapping
import com.plexiti.generics.web.Resource
import com.plexiti.horizon.domain.AccountId
import com.plexiti.horizon.domain.CreateAccount
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity


/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
@Resource
data class Account(val id: String, val name: String)

@Controller
@RequestMapping("/accounts")
class AccountController {

    @Autowired
    private lateinit var commandGateway: CommandGateway

    @RequestMapping(method = arrayOf(RequestMethod.POST)) @ResponseBody
    fun accounts(@RequestParam(value = "name", required = true) name: String): ResponseEntity<CreateAccount> {
        val command = CreateAccount(name)
        try {
            commandGateway.send<CreateAccount>(command).get()
            return ResponseEntity(HttpStatus.OK)
        } catch (e: RuntimeException) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}