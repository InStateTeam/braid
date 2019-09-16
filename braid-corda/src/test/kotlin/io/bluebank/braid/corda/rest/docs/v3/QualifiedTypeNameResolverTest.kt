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
package io.bluebank.braid.corda.rest.docs.v3

import com.fasterxml.jackson.databind.type.TypeFactory
import net.corda.core.contracts.Amount
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

import org.junit.Assert.*
import java.util.*

class QualifiedTypeNameResolverTest {

  private val  resolver =    QualifiedTypeNameResolver();

  @Test
  fun `should use simeple name for java types`() {
      assertThat(resolver.nameForType(javatype(Currency::class.java)), equalTo("Currency"))
  }


  @Test
  fun `should be dot separated type for user types`() {
      assertThat(resolver.nameForType(javatype(QualifiedTypeNameResolverTest::class.java)), equalTo("io.bluebank.braid.corda.rest.docs.v3.QualifiedTypeNameResolverTest"))
  }


  @Test
  fun `should be underscore separated type for Inner user types`() {
      assertThat(resolver.nameForType(javatype(Inner::class.java)), equalTo("io.bluebank.braid.corda.rest.docs.v3.QualifiedTypeNameResolverTest_Inner"))
  }


  @Test
  fun `should be underscore separated type for Generic user types`() {
      assertThat(resolver.nameForType(javatype(Amount::class.java)), equalTo("net.corda.core.contracts.Amount"))
  }



  private fun javatype(clazz: Class<*>) = TypeFactory.defaultInstance().constructType(clazz)

  private class Inner


}