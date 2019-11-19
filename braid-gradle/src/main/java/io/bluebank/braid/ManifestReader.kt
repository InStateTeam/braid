package io.bluebank.braid

import java.net.URL

class ManifestReader(val manifestUrl: String){

  fun latest():String{
    val readText = URL(manifestUrl).readText()
    return readText.substring(readText.indexOf("<latest>") +8,readText.indexOf("</latest>") )
  }
}