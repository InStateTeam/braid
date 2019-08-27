package io.bluebank.braid.core.synth;

import java.io.IOException;

import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to dump out the class, field and methodd declaration bytecode descriptions
 */
public class ClassReaderWriter extends ClassVisitor {
  private static Logger logger = LoggerFactory.getLogger(ClassReaderWriter.class);

  public ClassReaderWriter() {
    super(Opcodes.ASM5);
  }
  public static void readAndPrint(Class clazz) throws IOException {
    new ClassReader(clazz.getName()).accept(new ClassReaderWriter(), ClassReader.EXPAND_FRAMES);
  }
  public static void readAndPrint(byte[] bytes) {
    new ClassReader(bytes).accept(new ClassReaderWriter(), ClassReader.EXPAND_FRAMES);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    logger.info("class access {} name {} signature {}, superName {} interfaces {}", access, name, signature, superName, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    logger.info("method access {} name {} desc {} signature {}, exceptions {}", access, name, desc, signature, exceptions);
    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    logger.info("field access {} name {} desc {} signature {}, value {}", access, name, desc, signature, value);
    return super.visitField(access, name, desc, signature, value);
  }
}
