package io.bluebank.braid.client.invocations.impl

import io.vertx.core.Future
import rx.Observable

// this test uses this interface for reflection of the method signature
interface TestInterface {
  fun testFuture(): Future<String>
  fun testObservable(): Observable<String>
}
