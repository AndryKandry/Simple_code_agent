package ru.agent.common.extensions

import ru.agent.common.wrappers.ResultWrapper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

suspend fun <T> safeCall(
    call: suspend () -> T,
    callError: (Throwable) -> ResultWrapper<Nothing>,
): ResultWrapper<T> {
    return try {
        ResultWrapper.Success(call.invoke())
    } catch (throwable: Throwable) {
        callError(throwable)
    }
}

/**
 * safeCall example:
 *
 *    suspend fun fetchArtists(
 *         uuid: String,
 *         position: Int,
 *         title: String,
 *         ids: List<Int>,
 *         content: String?,
 *         onSuccess: suspend (ArtistsShelfInfo) -> Unit,
 *         onError: suspend (Throwable) -> Unit
 *     ) {
 *         val result = safeCall(
 *             call = {
 *                 fetchArtistsShelfUseCase(
 *                     uuid = uuid,
 *                     id = position,
 *                     title = title,
 *                     ids = ids,
 *                     content = content
 *                 )
 *             },
 *             callError = networkErrorHandling::transformToResultWrapper
 *         )
 *         when (result) {
 *             is ResultWrapper.Success -> {
 *                 onSuccess(result.value)
 *             }
 *
 *             is ResultWrapper.Error -> {
 *                 onError(result.throwable)
 *             }
 *         }
 *     }
 *
 * class FetchArtistsShelfUseCase(
 *     private val application: Application,
 *     private val artistRepository: ArtistRepository,
 *     private val artistsInfoFromShortArtistMapper: ArtistsInfoFromShortArtistMapper,
 * ) {
 *
 *     suspend operator fun invoke(
 *         uuid: String,
 *         id: Int,
 *         title: String,
 *         ids: List<Int>,
 *         content: String?
 *     ): ArtistsShelfInfo {
 *         val artists = artistRepository.fetchArtists(ids)
 *             .distinctBy { it.id }
 *         val artistsInfo = artistsInfoFromShortArtistMapper.map(artists)
 *         return ArtistsShelfInfo(
 *             uuid = uuid,
 *             position = id,
 *             title = title,
 *             artists = artistsInfo.take(MAX_ARTISTS_ON_SHELF).toImmutableList(),
 *             allArtistIds = artistsInfo.map { artist -> artist.id }.toImmutableList(),
 *             showAll = true,
 *             allArtistsButton = application.getString(R.string.all_artists),
 *             content = content
 *         )
 *     }
 *
 * }
 */

@OptIn(ExperimentalContracts::class)
inline fun <T> ResultWrapper<T>.onFailure(
    action: (error: ResultWrapper.Error) -> Unit
): ResultWrapper<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    this.exceptionOrNull()?.let(action)
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> ResultWrapper<T>.onSuccess(
    action: (value: T) -> Unit
): ResultWrapper<T> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }
    resultOrNull()?.let(action)
    return this
}