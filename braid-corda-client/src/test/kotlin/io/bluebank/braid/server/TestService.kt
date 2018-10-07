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
package io.bluebank.braid.server

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluebank.braid.core.annotation.ServiceDescription
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import rx.Observable
import java.math.BigDecimal

interface MyService {
  fun add(lhs: Double, rhs: Double): Double
  fun noArgs(): Int
  fun noResult()
  fun longRunning() : Future<Int>
  fun stream() : Observable<Int>
  fun largelyNotStream(): Observable<Int>
  fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject
  fun stuffedJsonObject(): JsonStuffedObject
  fun blowUp()
  fun exposeParameterListTypeIssue(str: String, md: ModelData): ModelData
//  fun functionWithTheSameNameAndNumberOfParameters(amount: BigDecimal, accountId: String): Boolean
  fun functionWithTheSameNameAndNumberOfParameters(amount: String, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: BigDecimal, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: BigDecimal, accountId: BigDecimal): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: Long, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: String, accountId: Int): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: Float, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: Double, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: ComplexObject, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: List<String>, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: Map<String, String>, accountId: String): Int
  fun functionWithTheSameNameAndNumberOfParameters(amount: Array<String>, accountId: String): Int
}

data class ComplexObject(val a: String, val b: Int, val c: Double)

data class JsonStuffedObject(val a: String) {
  val b: String
    get() = a
}

@ServiceDescription("my-service", "a simple service")
class MyServiceImpl(private val vertx: Vertx) : MyService {
  override fun add(lhs: Double, rhs: Double): Double {
    return lhs + rhs
  }

  override fun noArgs(): Int {
    return 5
  }

  override fun noResult() {
  }

  override fun longRunning(): Future<Int> {
    val result = future<Int>()
    vertx.setTimer(100) {
      result.complete(5)
    }
    return result
  }

  override fun stream(): Observable<Int> {
    return Observable.from(0 .. 10)
  }

  override fun largelyNotStream(): Observable<Int> {
    return Observable.error(RuntimeException("stream error"))
  }

  override fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject {
    return inComplexObject
  }

  override fun blowUp() {
    throw RuntimeException("expected exception")
  }

  override fun stuffedJsonObject(): JsonStuffedObject {
    return JsonStuffedObject("this is hosed")
  }

  override fun exposeParameterListTypeIssue(str: String, md: ModelData): ModelData {
    return md
  }

  override fun functionWithTheSameNameAndNumberOfParameters(amount: BigDecimal, accountId: String) = 1

  override fun functionWithTheSameNameAndNumberOfParameters(amount: String, accountId: String) = 2

  override fun functionWithTheSameNameAndNumberOfParameters(amount: BigDecimal, accountId: BigDecimal) = 3

  override fun functionWithTheSameNameAndNumberOfParameters(amount: Long, accountId: String) = 4

  override fun functionWithTheSameNameAndNumberOfParameters(amount: String, accountId: Int) = 5

  override fun functionWithTheSameNameAndNumberOfParameters(amount: Float, accountId: String) = 6

  override fun functionWithTheSameNameAndNumberOfParameters(amount: Double, accountId: String) = 7

  override fun functionWithTheSameNameAndNumberOfParameters(amount: ComplexObject, accountId: String) = 8

  override fun functionWithTheSameNameAndNumberOfParameters(amount: List<String>, accountId: String) = 9

  override fun functionWithTheSameNameAndNumberOfParameters(amount: Map<String, String>, accountId: String) = 10

  override fun functionWithTheSameNameAndNumberOfParameters(amount: Array<String>, accountId: String) = 11
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = MeteringModelData::class, name = "MeteringModelData")
)
interface ModelData

data class MeteringModelData(val someString: String) : ModelData
