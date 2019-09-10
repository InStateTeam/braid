package io.bluebank.braid.corda.rest

data class SwaggerInfo (
    val version :String? = "1.0.0",
    val serviceName :String ="",
    val description : String ="",
    val contact :ContactInfo = ContactInfo()
){
  fun withVersion(version:String):SwaggerInfo{
    return copy(version = version)
  }
  
  fun withServiceName(serviceName:String):SwaggerInfo{
    return copy(serviceName = serviceName)
  }
  
  fun withDescription(description:String):SwaggerInfo{
    return copy(description = description)
  }

  fun withContact(contact:ContactInfo):SwaggerInfo{
    return copy(contact = contact)
  }
  
}