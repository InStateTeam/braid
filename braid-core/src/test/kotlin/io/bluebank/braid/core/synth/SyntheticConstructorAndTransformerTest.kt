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
package io.bluebank.braid.core.synth

import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class ProgressTracker {
  companion object {
    private val log = loggerFor<ProgressTracker>()
  }
  fun ping() {
    log.info("ping")
  }
}

interface FlowLogic<T> {
  fun call(): T
}

class FooFlow(
  private val i: Int,
  private val l: Long,
  private val progressTracker: ProgressTracker
) : FlowLogic<Long> {
  override fun call(): Long {
    progressTracker.ping()
    return i.toLong() + l
  }
}

class SyntheticConstructorAndTransformerTest {
  @Test
  fun `that we can create a wrapper around a flow and inject context parameters and custom transformer`() {
    val boundTypes = createBoundParameterTypes()
    val constructor = FooFlow::class.java.preferredConstructor()
    val fn = trampoline(constructor, boundTypes, "MyPayloadName") {
      // do what you want here ...
      // e.g. call the flow directly
      // obviously, we will be invoking the flow via an interface to CordaRPCOps or ServiceHub
      // and return a Future
      it.call()
    }

    val json = """
    {
      "i": 100,
      "l": 1000
    }
  """

    val payload = Json.prettyMapper.decodeValue(json, fn)
    val result = fn.call(payload)
    assertEquals(1100L, result)
  }

  private fun createBoundParameterTypes(): Map<Class<*>, Any> {
    return mapOf<Class<*>, Any>(ProgressTracker::class.java to ProgressTracker())
  }
}