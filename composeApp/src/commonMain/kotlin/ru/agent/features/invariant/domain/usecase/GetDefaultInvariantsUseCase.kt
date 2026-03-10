package ru.agent.features.invariant.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantCategory
import ru.agent.features.invariant.domain.model.InvariantPriority
import ru.agent.features.invariant.domain.model.InvariantSource

/**
 * UseCase для получения дефолтных системных инвариантов проекта.
 *
 * Возвращает базовые правила, которые определены для проекта.
 */
class GetDefaultInvariantsUseCase {

    /**
     * Получить список дефолтных инвариантов.
     *
     * @return Список системных инвариантов
     */
    operator fun invoke(): List<Invariant> {
        return listOf(
            Invariant(
                id = "sys_arch_mvvm",
                description = "Использовать только MVVM и MVI паттерны, никаких MVP и MVC",
                category = InvariantCategory.ARCHITECTURE,
                priority = InvariantPriority.CRITICAL,
                isActive = true,
                source = InvariantSource.SYSTEM,
                createdAt = 0,
                updatedAt = 0
            ),
            Invariant(
                id = "sys_tech_cli",
                description = "Код генерируется для CLI режима, никакого UI на Compose Desktop",
                category = InvariantCategory.TECHNOLOGY,
                priority = InvariantPriority.CRITICAL,
                isActive = true,
                source = InvariantSource.SYSTEM,
                createdAt = 0,
                updatedAt = 0
            ),
            Invariant(
                id = "sys_stack_modern",
                description = "Использовать современные технологии (Coroutines, Flow, Koin, Room)",
                category = InvariantCategory.STACK,
                priority = InvariantPriority.HIGH,
                isActive = true,
                source = InvariantSource.SYSTEM,
                createdAt = 0,
                updatedAt = 0
            ),
            Invariant(
                id = "sys_business_confirm",
                description = "Все изменения кода требуют подтверждения пользователя",
                category = InvariantCategory.BUSINESS_RULE,
                priority = InvariantPriority.HIGH,
                isActive = true,
                source = InvariantSource.SYSTEM,
                createdAt = 0,
                updatedAt = 0
            )
        )
    }
}
