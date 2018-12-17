/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.client

import io.bluebank.braid.client.invocations.Invocations
import io.bluebank.braid.core.async.getOrThrow
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import org.slf4j.Logger
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicLong

/**
 * Deprecated. Used [BraidClient]
 */
@Deprecated("please use BraidClient instead - this will be removed in 4.0.0 - see issue #75", replaceWith = ReplaceWith("BraidClient"))
open class BraidProxyClient protected constructor (private val config: BraidClientConfig, val vertx: Vertx) : Closeable, InvocationHandler {
  private val nextId = AtomicLong(1)
  private lateinit var invokes : Invocations

  companion object {
    private val log: Logger = loggerFor<BraidProxyClient>()

    fun createProxyClient(config: BraidClientConfig, vertx: Vertx = Vertx.vertx()): BraidProxyClient {
      return BraidProxyClient(config, vertx)
    }
  }

  fun activeRequestsCount(): Int {
    return invokes.activeRequestsCount
  }

  fun <ServiceType : Any> bind(clazz: Class<ServiceType>, exceptionHandler: (Throwable) -> Unit = this::exceptionHandler, closeHandler: (() -> Unit) = this::closeHandler): ServiceType {
    return bindAsync(clazz, exceptionHandler, closeHandler).getOrThrow()
  }

  // TODO: fix the obvious lunacy of only having one handler per socket...
  @Suppress("UNCHECKED_CAST")
  fun <ServiceType : Any> bindAsync(clazz: Class<ServiceType>, exceptionHandler: (Throwable) -> Unit = this::exceptionHandler, closeHandler: (() -> Unit) = this::closeHandler): Future<ServiceType> {
    val result = future<ServiceType>()
    try {
      invokes = Invocations.create(vertx, config, exceptionHandler, closeHandler)
      val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), this) as ServiceType
      result.complete(proxy)
    } catch (err: Throwable) {
      log.error("failed during connection", err)
      result.fail(err)
    }
    return result
  }

  override fun close() {
    invokes.close()
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
    return invokes.invoke(method.name, method.genericReturnType, args ?: arrayOfNulls<Any>(0))
  }

  private fun closeHandler() {
    log.info("closing proxy to {}", config.serviceURI)
  }

  private fun exceptionHandler(error: Throwable) {
    log.error("exception from socket", error)
    // TODO: handle retries?
    // TODO: handle error!
  }
}
