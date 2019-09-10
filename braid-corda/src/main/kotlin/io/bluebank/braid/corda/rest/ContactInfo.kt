package io.bluebank.braid.corda.rest

data class ContactInfo (
    var name : String? = null,
    var url: String? = null,
    var email: String? = null
)    {
  fun name(name:String):ContactInfo{
    return copy(name = name)
  }
  fun url(url:String):ContactInfo{
    return copy(url = url)
  }
  fun email(email:String):ContactInfo{
    return copy(email = email)
  }

}