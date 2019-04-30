/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.engine.js

import io.ktor.client.engine.*

/**
 * [HttpClientEngineFactory] using a fetch API to execute requests.
 */
object Js : HttpClientEngineFactory<HttpClientEngineConfig> {
    override fun create(block: HttpClientEngineConfig.() -> Unit): HttpClientEngine =
        JsClientEngine(HttpClientEngineConfig().apply(block))
}

@JsName("JsClient")
fun JsClient(): HttpClientEngineFactory<HttpClientEngineConfig> = Js
