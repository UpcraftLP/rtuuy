package dev.upcraft.rtuuy.model

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer

class SnowflakeTransformer : ColumnTransformer<ULong, Snowflake> {
	override fun unwrap(value: Snowflake): ULong {
		return value.value
	}

	override fun wrap(value: ULong): Snowflake {
		return Snowflake(value)
	}
}

open class SnowflakeIdTable(name: String = "", columnName: String = "id") : IdTable<Snowflake>(name) {
	final override val id: Column<EntityID<Snowflake>> =
		ulong(columnName).autoIncrement().transform(SnowflakeTransformer()).entityId()
	final override val primaryKey = PrimaryKey(id)
}

abstract class SnowflakeEntity(id: EntityID<Snowflake>) : Entity<Snowflake>(id)

abstract class SnowflakeEntityClass<out E : SnowflakeEntity>(
	table: IdTable<Snowflake>,
	entityType: Class<E>? = null,
	entityCtor: ((EntityID<Snowflake>) -> E)? = null
) : EntityClass<Snowflake, E>(table, entityType, entityCtor)
