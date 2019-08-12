package io.bluebank.braid.server.util

import io.vertx.ext.unit.TestContext
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.StringDescription


fun <T> TestContext.assertThat(actual: T, matcher: Matcher<in T>,reason: String=""){

     if(!matcher.matches(actual)){
         val description = StringDescription()
                 .appendText(reason)
                 .appendDescriptionOf(matcher)
                 .appendText(" but was:")
         matcher.describeMismatch(actual,description)
         this.fail(description.toString())
     }
}