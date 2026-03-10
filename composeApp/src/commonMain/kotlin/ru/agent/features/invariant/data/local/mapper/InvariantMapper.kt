package ru.agent.features.invariant.data.local.mapper

import ru.agent.features.invariant.data.local.entity.InvariantEntity
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantCategory
import ru.agent.features.invariant.domain.model.InvariantPriority
import ru.agent.features.invariant.domain.model.InvariantSource

/**
 * Маппер для преобразования между InvariantEntity и Invariant domain model.
 */
object InvariantMapper {

    /**
     * Преобразовать Entity в Domain модель.
     */
    fun InvariantEntity.toDomain(): Invariant {
        return Invariant(
            id = this.id,
            description = this.description,
            category = InvariantCategory.valueOf(this.category),
            priority = InvariantPriority.valueOf(this.priority),
            isActive = this.isActive,
            source = InvariantSource.valueOf(this.source),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    /**
     * Преобразовать Domain модель в Entity.
     */
    fun Invariant.toEntity(): InvariantEntity {
        return InvariantEntity(
            id = this.id,
            description = this.description,
            category = this.category.name,
            priority = this.priority.name,
            isActive = this.isActive,
            source = this.source.name,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    /**
     * Преобразовать список Entity в список Domain моделей.
     */
    fun List<InvariantEntity>.toDomain(): List<Invariant> {
        return this.map { it.toDomain() }
    }

    /**
     * Преобразовать список Domain моделей в список Entity.
     */
    fun List<Invariant>.toEntity(): List<InvariantEntity> {
        return this.map { it.toEntity() }
    }
}
