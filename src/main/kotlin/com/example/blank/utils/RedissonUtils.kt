package com.example.blank.utils

import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val zeroSec = 0.seconds.toJavaDuration()

internal inline fun <reified K, reified V> RedissonClient.localCachedMap(
    name: String,
    options: LocalCachedMapOptions<K, V>.() -> Unit = {}
) = getLocalCachedMap<K, V>(
    LocalCachedMapOptions.name<K, V>(name)
        .cacheSize(0).timeToLive(zeroSec).maxIdle(zeroSec)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.NONE)
        .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.NONE)
        .cacheProvider(LocalCachedMapOptions.CacheProvider.REDISSON)
        .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
        .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE).storeCacheMiss(false)
        .useObjectAsCacheKey(false).useTopicPattern(false)
        .expirationEventPolicy(LocalCachedMapOptions.ExpirationEventPolicy.SUBSCRIBE_WITH_KEYEVENT_PATTERN)
        .apply(options)
)
