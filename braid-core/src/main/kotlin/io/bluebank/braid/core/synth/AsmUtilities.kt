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
package io.bluebank.braid.core.synth

import com.fasterxml.jackson.annotation.JsonProperty
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import java.lang.reflect.*
import javax.validation.constraints.NotNull
import org.objectweb.asm.Type as AsmType

private val objectClassRef = Object::class.java.canonicalName.replace('.', '/')

/**
 * Writes out to the bytecode a default constructor
 * This is usually done by the Java / Kotlin compiler, but here using ASM we need to be explicit`
 *
 *  From https://asm.ow2.io/asm4-guide.pdf
 * The methods of the ClassVisitor class must be called in the following order,
specified in the Javadoc of this class:
visit visitSource?
visitOuterClass? ( visitAnnotation | visitAttribute )*
( visitInnerClass | visitField | visitMethod )*
visitEnd
 
 */
fun ClassWriter.writeDefaultConstructor() {
  val constructorMethod = visitMethod(
    Opcodes.ACC_PUBLIC, // public method
    "<init>",   // method name
    "()V",      // JVM description for this method - it's a default constructor
    null,       // signature (null means not generic)
    null        // no checked exceptions
  )
  constructorMethod.visitCode()
  constructorMethod.visitVarInsn(Opcodes.ALOAD, 0)
  // call super constructor
  constructorMethod.visitMethodInsn(
    Opcodes.INVOKESPECIAL,
    "java/lang/Object",
    "<init>",
    "()V",
    false
  )

  constructorMethod.visitInsn(Opcodes.RETURN)
  constructorMethod.visitMaxs(1, 1)
  constructorMethod.visitEnd()
}

/**
 * Works out the JVM byte code "signature" for a given type
 * The JVM signature denotes the specialisation of generic type
 * @return null if the type is not generic
 * @return a JVM bytecode signature for the type parameterisation
 */
fun Type.genericSignature(): String? {
  return when (this) {
    is ParameterizedType -> {
      SignatureWriter().let {
        it.writeSignature(this)
        it.toString()
      }
    }
    else -> null
  }
}

/**
 * add all parameters of the constructor as fields in the class writer
 * this method is used to build a payload type representing the parameters of a constructor
 */
fun ClassWriter.addFields(constructor: Constructor<*>) = addFields(constructor.parameters)

/**
 * add all parameters of the method as fields in the class writer
 * this method is used to build a payload type representing the parameters of a method
 */
fun ClassWriter.addFields(method: Method) = addFields(method.parameters)

/**
 * add all parameters as fields in the class writer
 */
fun ClassWriter.addFields(parameters: Array<Parameter>) =
  parameters.forEach { addField(it) }

/**
 * add a parameter as a field to the class being written
 */
fun ClassWriter.addField(it: Parameter) {
  val visitField = visitField(
      Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,   // access permissions
      it.name,                                  // field name
      AsmType.getDescriptor(it.type),           // raw type description
      it.parameterizedType.genericSignature(),  // generic parameterisation
      null                                      // default value is null
  )
  val visitAnnotation = visitField
      .visitAnnotation(AsmType.getDescriptor(NotNull::class.java), true)
  visitAnnotation.visitEnd()
  visitField.visitEnd()
  
}

/**
 * add the declaration of a class
 * NOTE: callers must call [ClassWriter.endVisit] when the class body is complete
 */
internal fun ClassWriter.declareSimplePublicClass(className: String) {
  val jvmByteCodeName = className.replace('.', '/');
  visit(
    Opcodes.V1_8,
    Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
    jvmByteCodeName,
    null, // no generic signature on this type
    objectClassRef, // base type is Object
    emptyArray() // no implemented interfaces
  )
}

/**
 * write the generic parameterisation signature to a [SignatureVisitor]
 */
private fun SignatureVisitor.writeSignature(type: java.lang.reflect.Type) {
  when (type) {
    is ParameterizedType -> writeParameterizedTypeSignature(type)
    is Class<*> -> writeClassTypeSignature(type)
    is WildcardType -> writeWildCardTypeSignature(type)
    is TypeVariable<*> -> writeTypeVariableSignature(type)
    else -> error("unhandled type: $type")
  }
}

private fun <D : GenericDeclaration> SignatureVisitor.writeTypeVariableSignature(type: TypeVariable<D>) {
  visitTypeVariable(type.name)
}


private fun SignatureVisitor.writeWildCardTypeSignature(type: WildcardType) {
  when {
    type.upperBounds.size == 1 -> {
      visitTypeArgument(SignatureVisitor.INSTANCEOF).writeSignature(type.upperBounds[0])
    }
    type.lowerBounds.size == 1 -> {
      visitTypeArgument(SignatureVisitor.INSTANCEOF).writeSignature(type.lowerBounds[0])
    }
    else -> error("unhandled wildcard type of upper bound size ${type.upperBounds.size} and lower bound size ${type.lowerBounds.size}")
  }
}

private fun SignatureVisitor.writeClassTypeSignature(type: Class<*>) {
  visitClassType(AsmType.getInternalName(type))
  visitEnd()
}

private fun SignatureVisitor.writeParameterizedTypeSignature(
  type: ParameterizedType
) {
  visitClassType(AsmType.getInternalName(type.rawType as Class<*>))
  type.actualTypeArguments.forEach { a ->
    val tv = visitTypeArgument(SignatureVisitor.INSTANCEOF)
    tv.writeSignature(a)
  }
  visitEnd()
}