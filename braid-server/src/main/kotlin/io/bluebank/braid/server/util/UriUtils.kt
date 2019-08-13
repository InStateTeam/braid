package io.bluebank.braid.server.util

import java.net.URLEncoder

fun URLEncoder.utf8(value:String):String{
    return URLEncoder.encode(value,"UTF-8")
}