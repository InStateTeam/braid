package io.bluebank.braid.corda.example

import io.bluebank.braid.shaded.io.vertx.core.AsyncResult
import io.bluebank.braid.shaded.io.vertx.core.Future
import io.bluebank.braid.shaded.io.vertx.core.Handler
import io.bluebank.braid.shaded.io.vertx.core.json.JsonObject
import io.bluebank.braid.shaded.io.vertx.ext.auth.AbstractUser
import io.bluebank.braid.shaded.io.vertx.ext.auth.AuthProvider
import io.bluebank.braid.shaded.io.vertx.ext.auth.User

class MySimpleAuthProvider : AuthProvider {
  override fun authenticate(authInfo: JsonObject, callback: Handler<AsyncResult<User>>) {
    try {
      val username = authInfo.getString("username") ?: throw RuntimeException("no username found")
      callback.handle(Future.succeededFuture(MySimpleUser(username)))
    } catch(err: Throwable) {
      callback.handle(Future.failedFuture(err))
    }
  }
}

class MySimpleUser(private val userName: String): AbstractUser() {
  override fun doIsPermitted(permission: String, callback: Handler<AsyncResult<Boolean>>) {
    callback.handle(Future.succeededFuture(true))
  }

  override fun principal(): JsonObject {
    val result = JsonObject()
    result.put("username", userName)
    return result
  }

  override fun setAuthProvider(provider: AuthProvider) {}
}
