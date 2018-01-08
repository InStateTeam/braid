package io.bluebank.braid.client

import java.net.URI

data class BraidClientConfig(val serviceURI: URI,
                             val tls: Boolean = true,
                             val trustAll: Boolean = false,
                             val verifyHost: Boolean = false)