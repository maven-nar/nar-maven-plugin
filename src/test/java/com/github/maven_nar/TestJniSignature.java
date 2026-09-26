/*
 * #%L
 * Native ARchive plugin for Maven
 * %%
 * Copyright (C) 2002 - 2014 NAR Maven Plugin developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.maven_nar;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

import com.github.maven_nar.JniSignature.Value;

import junit.framework.TestCase;

/** Binding identity must survive specialization and signature serialization. */
public class TestJniSignature extends TestCase {
  public void testSpecializationKeepsCallerAndMethodVariablesDistinct() {
    Map<String, Value> arguments = new HashMap<String, Value>();
    arguments.put("A", JniSignature.read("<T:Ljava/lang/Number;>Ljava/lang/Object;").variables().get("T"));
    JniClass.Method method = specialize("<T::Ljava/lang/CharSequence;>(TA;TT;)V", arguments);
    assertEquals("(Ljava/lang/Number;Ljava/lang/CharSequence;)V", method.descriptor);
    assertDistinctParameters(method.source, "T");
    assertDistinctParameters(JniSignature.read(method.signature), "T");
  }

  public void testSpecializationReservesNestedReceiverVariables() {
    Map<String, Value> caller = JniSignature.read("<T:Ljava/lang/Number;>Ljava/lang/Object;").variables();
    Value owner = JniSignature.read("(Lp/Owner<TT;>.Member<[TT;>;)V").parameters.get(0).substitute(caller);
    Map<String, Value> arguments = new HashMap<String, Value>();
    arguments.put("A", owner);
    JniClass.Method method = specialize("<T::Ljava/lang/CharSequence;>(TA;TT;)V", arguments);
    for (JniSignature source : new JniSignature[] {
        method.source, JniSignature.read(method.signature)
    }) {
      Value parameter = source.parameters.get(0);
      assertEquals("T", parameter.owner.arguments.get(0).variable);
      assertEquals("T", parameter.arguments.get(0).component.variable);
      assertFalse("T".equals(source.parameters.get(1).variable));
    }
  }

  public void testSpecializationReservesVariablesInRecursiveReceiverBounds() {
    Map<String, Value> caller = JniSignature
        .read("<X::Ljava/lang/Comparable<TT;>;" + "T:Ljava/lang/Number;:Ljava/lang/Comparable<TX;>;>Ljava/lang/Object;")
        .variables();
    Map<String, Value> arguments = new HashMap<String, Value>();
    arguments.put("A", caller.get("X"));
    JniClass.Method method = specialize("<T::Ljava/lang/CharSequence;>(TA;TT;)V", arguments);
    assertDistinctParameters(method.source, "X");
    assertFalse("T".equals(method.source.parameters.get(1).variable));
    assertEquals("T", method.source.parameters.get(0).limits.get(0).arguments.get(0).variable);
    assertEquals("X", caller.get("T").limits.get(1).arguments.get(0).variable);
    assertFalse("T".equals(JniSignature.read(method.signature).parameters.get(1).variable));
  }

  public void testSpecializationPreservesRecursiveDependentAndThrownFormals() {
    Map<String, Value> arguments = new HashMap<String, Value>();
    arguments.put("A", JniSignature.read("<T:Ljava/lang/Number;>Ljava/lang/Object;").variables().get("T"));
    JniClass.Method method = specialize(
        "<T:Ljava/lang/Exception;:Ljava/lang/Comparable<TT;>;U:TT;>" + "(TA;TT;TU;)TT;^TU;", arguments);
    assertEquals("(Ljava/lang/Number;Ljava/lang/Exception;Ljava/lang/Exception;)Ljava/lang/Exception;",
        method.descriptor);
    for (JniSignature source : new JniSignature[] {
        method.source, JniSignature.read(method.signature)
    }) {
      assertDistinctParameters(source, "T");
      String formal = source.parameters.get(1).variable;
      assertEquals(formal, source.bounds.get(formal).get(1).arguments.get(0).variable);
      assertEquals(formal, source.bounds.get("U").get(0).variable);
      assertEquals(formal, source.result.variable);
      assertEquals("U", source.exceptions.get(0).variable);
    }
  }

  public void testSpecializationPreservesShadowingAndExistingAliasNames() {
    Map<String, Value> arguments = new HashMap<String, Value>();
    arguments.put("A", JniSignature.read("<T:Ljava/lang/Number;>Ljava/lang/Object;").variables().get("T"));
    arguments.put("T", Value.object("java/lang/Number"));
    arguments.put("_NarFormal0", Value.object("java/lang/Number"));
    JniClass.Method method = specialize("<T::Ljava/lang/CharSequence;>(TA;TT;T_NarFormal0;)V", arguments);
    assertEquals("(Ljava/lang/Number;Ljava/lang/CharSequence;Ljava/lang/Number;)V", method.descriptor);
    assertDistinctParameters(method.source, "T");
    assertFalse("_NarFormal0".equals(method.source.parameters.get(1).variable));
    JniClass.Method shadowed = specialize("<T::Ljava/lang/CharSequence;>(TT;)V", arguments);
    assertEquals("(Ljava/lang/CharSequence;)V", shadowed.descriptor);
  }

  private static JniClass.Method specialize(String signature, Map<String, Value> arguments) {
    JniClass.Method method = new JniClass.Method(Opcodes.ACC_PUBLIC, "call", "()V", signature);
    return JniSignature.read(signature).method(method, arguments);
  }

  private static void assertDistinctParameters(JniSignature source, String caller) {
    assertEquals(caller, source.parameters.get(0).variable);
    String formal = source.parameters.get(1).variable;
    assertNotNull(formal);
    assertFalse(caller.equals(formal));
    assertTrue(source.bounds.containsKey(formal));
    assertFalse(source.bounds.containsKey(caller));
  }
}
