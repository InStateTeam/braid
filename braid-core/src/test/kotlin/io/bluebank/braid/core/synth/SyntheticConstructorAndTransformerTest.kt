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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
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
  private val l: Long = 42,
  private val progressTracker: ProgressTracker
) : FlowLogic<Long> {

  override fun call(): Long {
    progressTracker.ping()
    return i.toLong() + l
  }
}

class UnboundParameterFooFlow<OldState>(
  private val i: Int,
  private val l: Map<String,OldState>
) : FlowLogic<OldState> {

  override fun call(): OldState {
    return l.get("key")!!;
  }
}

class UnboundFooFlow<OldState>(
  private val i: Int,
  private val l: OldState
) : FlowLogic<OldState> {

  override fun call(): OldState {
    return l;
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
      // it.call()
      constructor.newInstance(*it).call()
      // println(it)
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

  @Test
  fun shouldNotIncludeSyntheticConstructors() {
    val constructor = FooFlow::class.java.preferredConstructor()
    assertThat(constructor.isSynthetic, `is`(false))
  }

  // This seems possible to have unbounded types.
  @Test
  fun `that we treat unbound parameters as their base class`() {
    val constructor = UnboundFooFlow::class.java.preferredConstructor()
    val fn = trampoline(constructor, emptyMap(), "MyUnboundPayloadName") {
      constructor.newInstance(*it).call()
    }

    val json = """
    {                           
      "i": 100,
      "l": { "key":"value" }
    }
  """

    val payload = Json.prettyMapper.decodeValue(json, fn)
    val result = fn.call(payload)
    assertEquals(mapOf("key" to "value"), result)
  }

  // not sure if this is possible. It is derived from the failure to synthesise
  // ContractUpgradeFlow.Initiate
  @Test
  @Ignore
  fun `that we treat parameterised types from their parent class`() {
    val constructor = UnboundParameterFooFlow::class.java.preferredConstructor()
    val fn = trampoline(constructor, emptyMap(), "MyUnboundParameterPayloadName") {
      constructor.newInstance(*it).call()
    }

    val json = """
    {
      "i": 100,
      "l": { "key":"value" }
    }
  """

    val payload = Json.prettyMapper.decodeValue(json, fn)
    val result = fn.call(payload)
    assertEquals("value", result)
  }


}