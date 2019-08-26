package io.bluebank.braid.corda.rest.docs

import java.lang.reflect.Type
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType


fun KType.javaTypeIncludingSynthetics(): Type
{
    try {
        return this.javaType
    } catch (e: Throwable) {
        this.classifier
        val toString = this.classifier.toString()
        //   return (this.classifier as KClassImpl).jClass
        return Class.forName(toString.replace("class ",""))     //todo better way of getting the java class
        // return (this as KTypeSynthetic).clazz
    }
}
