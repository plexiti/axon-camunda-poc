package com.plexiti.horizon.model.write

import com.plexiti.horizon.model.api.Identifier
import org.axonframework.commandhandling.model.AggregateIdentifier

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
abstract class AggregateIdentifiedBy<ID: Any>(): Identifier<ID>() {

    @AggregateIdentifier
    override lateinit var id: ID

    protected constructor(id: ID): this() {
        this.id = id
    }

    override fun toString(): String {
        return "${this::class.qualifiedName}(id=${id})"
    }

}