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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.maven_nar.JniSignature.Value;

/**
 * Resolves the fixed-arity calls emitted by JavacHeaders, without invoking a
 * compiler.
 * Arguments are typed defaults or null, never poly expressions. We require a
 * strict
 * invocation match (JLS 15.12.2.2), so boxing and variable-arity expansion
 * cannot affect
 * the answer. A varargs constructor participates with its final array
 * parameter.
 */
final class JniConstructorResolver {
  interface Types {
    List<Value> parents(Value type) throws IOException;

    List<List<Value>> parameterBounds(Value type) throws IOException;

    boolean isInterface(String name) throws IOException;

    boolean isFinal(String name) throws IOException;
  }

  static final class Candidate {
    final String descriptor;
    final List<Value> parameters;
    final Map<String, List<Value>> bounds;
    final List<Value> exceptions;

    Candidate(String descriptor, List<Value> parameters, Map<String, List<Value>> bounds) {
      this(descriptor, parameters, bounds, Collections.<Value> emptyList());
    }

    Candidate(String descriptor, List<Value> parameters, Map<String, List<Value>> bounds, List<Value> exceptions) {
      this.descriptor = descriptor;
      this.parameters = parameters;
      this.bounds = bounds;
      this.exceptions = exceptions;
    }

    Candidate rename(String prefix) {
      Map<String, String> names = new HashMap<String, String>();
      for (String name : bounds.keySet()) {
        names.put(name, prefix + names.size());
      }
      List<Value> parameters = new ArrayList<Value>();
      for (Value value : this.parameters) {
        parameters.add(value == null ? null : value.rename(names));
      }
      Map<String, List<Value>> bounds = new LinkedHashMap<String, List<Value>>();
      for (Map.Entry<String, List<Value>> entry : this.bounds.entrySet()) {
        List<Value> values = new ArrayList<Value>();
        for (Value value : entry.getValue()) {
          values.add(value.rename(names));
        }
        bounds.put(names.get(entry.getKey()), values);
      }
      List<Value> exceptions = new ArrayList<Value>();
      for (Value value : this.exceptions) {
        exceptions.add(value.rename(names));
      }
      return new Candidate(descriptor, parameters, bounds, exceptions);
    }
  }

  private final Types types;

  JniConstructorResolver(Types types) {
    this.types = types;
  }

  /**
   * Whether two upper bounds can describe a common subtype, including generic
   * ancestors.
   */
  boolean overlappingBounds(Value left, Value right, Map<String, List<Value>> bounds) throws IOException {
    Inference inference = new Inference(bounds, Collections.<String, List<Value>> emptyMap(), false);
    inference.add('~', left, right);
    return inference.solve();
  }

  Candidate resolve(List<Value> arguments, Map<String, List<Value>> bounds, List<Candidate> candidates)
      throws IOException {
    Candidate call = new Candidate("arguments", arguments, bounds);
    List<Candidate> applicable = new ArrayList<Candidate>();
    for (Candidate candidate : candidates) {
      if (matches(call, candidate, true)) {
        applicable.add(candidate);
      }
    }
    if (applicable.isEmpty()) {
      throw new IOException("no constructor applicable by strict invocation");
    }
    List<Candidate> maximal = new ArrayList<Candidate>();
    for (Candidate candidate : applicable) {
      boolean dominated = false;
      for (Candidate other : applicable) {
        // Most-specific inference uses the first declaration's symbolic formals,
        // not their instantiations inferred from this particular call (18.5.4).
        if (other != candidate && matches(other, candidate, false) && !matches(candidate, other, false)) {
          dominated = true;
          break;
        }
      }
      if (!dominated) {
        maximal.add(candidate);
      }
    }
    if (maximal.size() == 1) {
      return maximal.get(0);
    }
    List<String> descriptions = new ArrayList<String>();
    for (Candidate candidate : maximal) {
      descriptions.add(candidate.descriptor);
    }
    // Constructors are concrete declarations in one class; the abstract-method
    // tie breakers in 15.12.2.5 do not apply.
    throw new IOException("ambiguous constructor invocation among " + descriptions);
  }

  List<String> exceptions(List<Value> arguments, Map<String, List<Value>> bounds, Candidate target) throws IOException {
    Candidate left = new Candidate("arguments", arguments, bounds).rename("#source");
    Candidate right = target.rename("#infer");
    Inference inference = invocation(left, right, true);
    if (!inference.solve()) {
      throw new IOException("constructor exception inference is not applicable");
    }
    Set<String> thrown = new HashSet<String>();
    for (Value value : right.exceptions) {
      if (value.variable != null) {
        thrown.add(value.variable);
      }
    }
    List<String> result = new ArrayList<String>();
    for (Value value : right.exceptions) {
      Value resolved = inference.exceptionBound(value, thrown, new HashSet<String>());
      String name = inference.classBound(resolved, new HashSet<String>());
      if (name == null) {
        throw new IOException("Cannot infer constructor exception " + value);
      }
      if (!result.contains(name)) {
        result.add(name);
      }
    }
    return result;
  }

  private boolean matches(Candidate arguments, Candidate target, boolean invocation) throws IOException {
    if (arguments.parameters.size() != target.parameters.size()) {
      return false;
    }
    Candidate left = arguments.rename("#source");
    Candidate right = target.rename("#infer");
    return invocation(left, right, invocation).solve();
  }

  private Inference invocation(Candidate left, Candidate right, boolean invocation) throws IOException {
    Inference inference = new Inference(left.bounds, right.bounds, invocation);
    for (int i = 0; i < left.parameters.size(); i++) {
      Value argument = left.parameters.get(i);
      if (invocation) {
        argument = inference.capture(argument);
      }
      inference.add(invocation ? 'c' : '<', argument, right.parameters.get(i));
    }
    return inference;
  }

  private static boolean primitive(Value value) {
    return value != null && value.name == null && value.variable == null && value.component == null;
  }

  private static boolean widens(Value from, Value to) {
    if (!primitive(from) || !primitive(to)) {
      return false;
    }
    int a = from.erase().getSort(), b = to.erase().getSort();
    if (a == b) {
      return true;
    }
    switch (a) {
      case Type.BYTE:
        if (b == Type.SHORT) {
          return true;
        } // fall through
      case Type.SHORT:
      case Type.CHAR:
        if (b == Type.INT) {
          return true;
        } // fall through
      case Type.INT:
        if (b == Type.LONG) {
          return true;
        } // fall through
      case Type.LONG:
        if (b == Type.FLOAT) {
          return true;
        } // fall through
      case Type.FLOAT:
        return b == Type.DOUBLE;
      default:
        return false;
    }
  }

  private static Value symbol(String name) {
    Value result = new Value();
    result.variable = name;
    return result;
  }

  private static final class Bounds {
    final Map<String, Value> lower = new LinkedHashMap<String, Value>();
    final Map<String, Value> upper = new LinkedHashMap<String, Value>();
  }

  private static final class Constraint {
    final char relation;
    final Value left, right;

    Constraint(char relation, Value left, Value right) {
      this.relation = relation;
      this.left = left;
      this.right = right;
    }

    String key() {
      return relation + ":" + left + ":" + right;
    }
  }

  /**
   * Reduces and incorporates subtype/equality/containment constraints (JLS
   * 18.2-18.3).
   */
  private final class Inference {
    private final Map<String, List<Value>> variables;
    private final Map<String, Value> captureLower = new HashMap<String, Value>();
    private final Map<String, Bounds> inferred = new LinkedHashMap<String, Bounds>();
    private final List<Constraint> pending = new ArrayList<Constraint>();
    private final Set<String> seen = new HashSet<String>();
    private final char boundRelation;
    private boolean valid = true;
    private int captures;

    Inference(Map<String, List<Value>> variables, Map<String, List<Value>> formals, boolean invocation) {
      boundRelation = invocation ? 'c' : '<';
      this.variables = new HashMap<String, List<Value>>(variables);
      for (String name : formals.keySet()) {
        inferred.put(name, new Bounds());
      }
      for (Map.Entry<String, List<Value>> entry : formals.entrySet()) {
        add('<', symbol(entry.getKey()), Value.object("java/lang/Object"));
        for (Value bound : entry.getValue()) {
          add('<', symbol(entry.getKey()), bound);
        }
      }
    }

    void add(char relation, Value left, Value right) {
      Constraint constraint = new Constraint(relation, left, right);
      if (seen.add(constraint.key())) {
        pending.add(constraint);
      }
    }

    boolean solve() throws IOException {
      for (int index = 0; valid && index < pending.size(); index++) {
        Constraint constraint = pending.get(index);
        switch (constraint.relation) {
          case '=':
            equal(constraint.left, constraint.right);
            break;
          case '~':
            overlap(constraint.left, constraint.right);
            break;
          case '?':
            contains(constraint.left, constraint.right);
            break;
          default:
            subtype(constraint.left, constraint.right, constraint.relation == 'c');
            break;
        }
      }
      return valid;
    }

    /**
     * Resolve the Throwable class part of the selected invocation's throws types.
     * Throwable subclasses cannot be generic. Thus a throws clause needs only
     * the class part of a lower-bound lub or upper-bound glb, not synthesized
     * generic intersection source. Keep caller/capture variables symbolic until
     * that projection: their bounds must not trigger the throws-only preference
     * for RuntimeException (JLS 18.4).
     */
    Value exceptionBound(Value value, Set<String> thrown, Set<String> visiting) throws IOException {
      Bounds bounds = inferred.get(value.variable);
      if (bounds == null) {
        return value;
      }
      List<Value> lower = new ArrayList<Value>(), upper = new ArrayList<Value>();
      boolean entered = visiting.add(value.variable);
      boolean independent = true;
      for (Value bound : bounds.upper.values()) {
        if (!inferred.containsKey(bound.variable)) {
          upper.add(bound);
        } else {
          independent = false;
          if (entered && !visiting.contains(bound.variable)) {
            upper.add(exceptionBound(bound, thrown, visiting));
          }
        }
      }
      if (entered) {
        for (Value bound : bounds.lower.values()) {
          if (!inferred.containsKey(bound.variable)) {
            lower.add(bound);
          } else if (!visiting.contains(bound.variable)) {
            lower.add(exceptionBound(bound, thrown, visiting));
          }
        }
        visiting.remove(value.variable);
      }
      if (!lower.isEmpty()) {
        if (lower.size() == 1) {
          return lower.get(0);
        }
        String common = classBound(lower.get(0), new HashSet<String>());
        for (int i = 1; common != null && i < lower.size(); i++) {
          String next = classBound(lower.get(i), new HashSet<String>());
          while (common != null && next != null && !classSubtype(next, common)) {
            common = superclass(common);
          }
        }
        if (common == null) {
          throw new IOException("Cannot resolve constructor exception lower bounds " + lower);
        }
        Value result = symbol("#exception-lub:" + value.variable);
        result.limits.add(Value.object(common));
        return result;
      }
      // JDK 8 retains checked coverage for E extends T even where later javac
      // versions infer RuntimeException. Preserve that coverage for dependent
      // formals; the direct throws-only case is consistent across these JDKs.
      boolean unchecked = entered && independent && thrown.contains(value.variable) && !upper.isEmpty();
      for (Value bound : upper) {
        unchecked &= "java/lang/Exception".equals(bound.name) || "java/lang/Throwable".equals(bound.name)
            || "java/lang/Object".equals(bound.name);
      }
      if (unchecked) {
        return Value.object("java/lang/RuntimeException");
      }
      // A fresh variable also represents an intersection or recursive group for
      // exception coverage. Do not discard interface bounds when another formal
      // depends on this one: Exception & Runnable cannot infer RuntimeException.
      Value result = symbol("#exception:" + value.variable);
      result.limits.addAll(upper);
      return result;
    }

    String classBound(Value value, Set<String> visiting) throws IOException {
      if (value.name != null) {
        return types.isInterface(value.name) ? null : value.name;
      }
      if (value.variable == null || !visiting.add(value.variable)) {
        return null;
      }
      String result = null;
      for (Value bound : uppers(value)) {
        String next = classBound(bound, visiting);
        if (next != null && (result == null || classSubtype(next, result))) {
          result = next;
        }
      }
      visiting.remove(value.variable);
      return result;
    }

    private boolean classSubtype(String child, String parent) throws IOException {
      for (String current = child; current != null; current = superclass(current)) {
        if (current.equals(parent)) {
          return true;
        }
      }
      return false;
    }

    private String superclass(String name) throws IOException {
      // Superclasses precede interfaces. Stop there rather than resolving
      // unrelated marker interfaces implemented by an exception class.
      for (Value parent : types.parents(Value.object(name))) {
        if (!types.isInterface(parent.name)) {
          return parent.name;
        }
      }
      return null;
    }

    Value capture(Value type) throws IOException {
      if (type == null || type.name == null) {
        return type;
      }
      Value captured = type.withWildcard('=');
      // A non-generic member can still have a parameterized enclosing type.
      // Capture its qualifier even when there are no local type arguments.
      captured.owner = capture(type.owner);
      captured.arguments.clear();
      boolean changed = false;
      for (Value argument : type.arguments) {
        if (argument.wildcard == '=') {
          captured.arguments.add(argument);
        } else {
          captured.arguments.add(symbol("#capture" + captures++));
          changed = true;
        }
      }
      if (!changed) {
        return captured.owner == type.owner ? type : captured;
      }
      List<List<Value>> bounds = types.parameterBounds(captured);
      for (int i = 0; i < type.arguments.size(); i++) {
        Value argument = type.arguments.get(i), value = captured.arguments.get(i);
        if (argument.wildcard == '=') {
          continue;
        }
        List<Value> upper = new ArrayList<Value>(bounds.get(i));
        if (argument.wildcard == '+') {
          upper.add(argument.withWildcard('='));
        }
        if (upper.isEmpty()) {
          upper.add(Value.object("java/lang/Object"));
        }
        variables.put(value.variable, upper);
        if (argument.wildcard == '-') {
          captureLower.put(value.variable, argument.withWildcard('='));
        }
      }
      return captured;
    }

    private List<Value> uppers(Value type) {
      List<Value> result = variables.get(type.variable);
      if (result != null) {
        return result;
      }
      if (type.wildcard != '=') {
        return type.upperBounds();
      }
      if (!type.limits.isEmpty()) {
        return type.limits;
      }
      return Collections.singletonList(Value.object("java/lang/Object"));
    }

    private Value project(Value type, String name, Set<String> visited) throws IOException {
      if (type == null || !visited.add(type.toString())) {
        return null;
      }
      if (name.equals(type.name)) {
        return type;
      }
      if (type.component != null && ("java/lang/Object".equals(name) || "java/lang/Cloneable".equals(name)
          || "java/io/Serializable".equals(name))) {
        return Value.object(name);
      }
      if (type.variable != null || type.wildcard != '=') {
        for (Value bound : uppers(type)) {
          Value found = project(bound, name, visited);
          if (found != null) {
            return found;
          }
        }
      } else if (type.name != null) {
        if ("java/lang/Object".equals(name)) {
          return Value.object(name);
        }
        for (Value parent : types.parents(type)) {
          Value found = project(parent, name, visited);
          if (found != null) {
            return found;
          }
        }
      }
      return null;
    }

    private Value arrayBound(Value type, Set<String> visited) {
      if (type.component != null) {
        return type;
      }
      if (type.variable == null || !visited.add(type.variable)) {
        return null;
      }
      for (Value upper : uppers(type)) {
        Value result = arrayBound(upper, visited);
        if (result != null) {
          return result;
        }
      }
      return null;
    }

    private void subtype(Value left, Value right, boolean unchecked) throws IOException {
      if (left == null) {
        valid &= !primitive(right);
        return;
      }
      if (left.same(right)) {
        return;
      }
      if (primitive(left) || primitive(right)) {
        valid &= widens(left, right);
        return;
      }
      Bounds lower = inferred.get(right.variable), upper = inferred.get(left.variable);
      if (lower != null || upper != null) {
        // Instantiating a formal with a raw type may satisfy its bound by
        // unchecked conversion, e.g. T extends Comparable<T> with raw Comparable.
        // This does not allow unchecked conversion between overload parameter
        // types during the separate most-specific comparison.
        if (lower != null && lower.lower.put(left.toString(), left) == null) {
          for (Value bound : lower.upper.values()) {
            add(boundRelation, left, bound);
          }
        }
        if (upper != null && upper.upper.put(right.toString(), right) == null) {
          for (Value bound : upper.lower.values()) {
            add(boundRelation, bound, right);
          }
          for (Value bound : upper.upper.values()) {
            if (!bound.same(right)) {
              add('~', bound, right);
            }
          }
        }
        return;
      }
      if (right.variable != null) {
        if (captureLower.containsKey(right.variable)) {
          add('<', left, captureLower.get(right.variable));
          return;
        }
        valid &= variableSubtype(left, right, new HashSet<String>());
        return;
      }
      if (right.component != null) {
        left = arrayBound(left, new HashSet<String>());
        if (left == null) {
          valid = false;
          return;
        }
        add(primitive(left.component) || primitive(right.component) ? '=' : unchecked ? 'c' : '<', left.component,
            right.component);
        return;
      }
      if (left.component != null) {
        valid &= "java/lang/Object".equals(right.name) || "java/lang/Cloneable".equals(right.name)
            || "java/io/Serializable".equals(right.name);
        return;
      }
      Value matching = project(left, right.name, new HashSet<String>());
      if (matching == null) {
        valid = false;
        return;
      }
      if (right.owner != null && matching.owner != null) {
        add(unchecked ? 'c' : '<', matching.owner, right.owner);
      }
      if (right.arguments.isEmpty()) {
        return;
      }
      if (matching.arguments.isEmpty()) {
        valid &= unchecked;
        return;
      }
      if (matching.arguments.size() != right.arguments.size()) {
        valid = false;
        return;
      }
      for (int i = 0; i < matching.arguments.size(); i++) {
        add('?', matching.arguments.get(i), right.arguments.get(i));
      }
    }

    private boolean variableSubtype(Value left, Value right, Set<String> visited) {
      if (left.same(right)) {
        return true;
      }
      if (left.variable == null || !visited.add(left.variable)) {
        return false;
      }
      for (Value upper : uppers(left)) {
        if (variableSubtype(upper, right, visited)) {
          return true;
        }
      }
      return false;
    }

    private void contains(Value left, Value right) {
      if (left.same(right) || right.wildcard == '*') {
        return;
      }
      if (right.wildcard == '=') {
        if (left.wildcard != '=') {
          valid = false;
        } else {
          add('=', left, right);
        }
      } else if (right.wildcard == '+') {
        Value bound = left.wildcard == '*' || left.wildcard == '-' ? Value.object("java/lang/Object")
            : left.withWildcard('=');
        add('<', bound, right.withWildcard('='));
      } else if (left.wildcard == '=' || left.wildcard == '-') {
        add('<', right.withWildcard('='), left.withWildcard('='));
      } else {
        valid = false;
      }
    }

    private void equal(Value left, Value right) {
      if (left.same(right)) {
        return;
      }
      if (inferred.containsKey(left.variable) || inferred.containsKey(right.variable)) {
        Value variable = inferred.containsKey(left.variable) ? left : right;
        Value value = variable == left ? right : left;
        Set<String> mentioned = new HashSet<String>();
        value.variables(mentioned);
        if (mentioned.contains(variable.variable)) {
          valid = false;
          return;
        }
        add('<', left, right);
        add('<', right, left);
        return;
      }
      if (left.component != null && right.component != null) {
        add('=', left.component, right.component);
        return;
      }
      if (left.wildcard != right.wildcard || left.name == null || !left.name.equals(right.name)
          || left.arguments.size() != right.arguments.size()) {
        valid = false;
        return;
      }
      if (left.owner != null && right.owner != null) {
        add('=', left.owner, right.owner);
      }
      for (int i = 0; i < left.arguments.size(); i++) {
        add('=', left.arguments.get(i), right.arguments.get(i));
      }
    }

    private Map<String, Value> ancestors(Value type) throws IOException {
      Map<String, Value> result = new LinkedHashMap<String, Value>();
      ancestors(type, result, new HashSet<String>());
      return result;
    }

    private void ancestors(Value type, Map<String, Value> result, Set<String> visited) throws IOException {
      if (!visited.add(type.toString())) {
        return;
      }
      if (type.variable != null) {
        // Dependencies are incorporated as new bounds arrive; caller variables
        // already have fixed bounds, including recursive and intersection bounds.
        if (!inferred.containsKey(type.variable)) {
          for (Value bound : uppers(type)) {
            ancestors(bound, result, visited);
          }
        }
      } else if (type.name != null) {
        result.put(type.name, type);
        for (Value parent : types.parents(type)) {
          ancestors(parent, result, visited);
        }
      }
    }

    private void overlap(Value left, Value right) throws IOException {
      if (left.same(right) || inferred.containsKey(left.variable) || inferred.containsKey(right.variable)) {
        return;
      }
      if (left.variable != null) {
        for (Value upper : uppers(left)) {
          add('~', upper, right);
        }
        return;
      }
      if (right.variable != null) {
        for (Value upper : uppers(right)) {
          add('~', left, upper);
        }
        return;
      }
      if (left.component != null || right.component != null) {
        if (left.component != null && right.component != null) {
          add('~', left.component, right.component);
        } else {
          Value array = left.component == null ? right : left, other = array == left ? right : left;
          add('<', array, other);
        }
        return;
      }
      if (primitive(left) || primitive(right)) {
        valid &= left.same(right);
        return;
      }
      Map<String, Value> a = ancestors(left), b = ancestors(right);
      // An inferred intersection cannot contain unrelated classes, or a final
      // class that does not implement another upper-bound interface.
      for (Value x : a.values()) {
        if (types.isInterface(x.name)) {
          continue;
        }
        for (Value y : b.values()) {
          if (!types.isInterface(y.name) && !a.containsKey(y.name) && !b.containsKey(x.name)) {
            valid = false;
            return;
          }
          if (types.isFinal(x.name) && !a.containsKey(y.name)) {
            valid = false;
            return;
          }
        }
      }
      for (Value y : b.values()) {
        if (types.isFinal(y.name)) {
          for (String name : a.keySet()) {
            if (!b.containsKey(name)) {
              valid = false;
              return;
            }
          }
        }
      }
      // Common generic ancestors must admit the same instantiation (18.3.1).
      for (Map.Entry<String, Value> entry : a.entrySet()) {
        Value x = entry.getValue(), y = b.get(entry.getKey());
        if (y == null) {
          continue;
        }
        // Non-static members carry enclosing arguments even without their own
        // formals: Outer<String>.Inner and Outer<Integer>.Inner cannot overlap.
        if (x.owner != null && y.owner != null) {
          add('~', x.owner, y.owner);
        }
        if (x.arguments.isEmpty() || y.arguments.isEmpty()) {
          continue;
        }
        for (int i = 0; i < x.arguments.size(); i++) {
          Value u = x.arguments.get(i), v = y.arguments.get(i);
          if (u.wildcard == '=') {
            add('?', u, v);
          } else if (v.wildcard == '=') {
            add('?', v, u);
          } else if (u.wildcard != '*' && v.wildcard != '*') {
            if (u.wildcard == '+' && v.wildcard == '+') {
              add('~', u.withWildcard('='), v.withWildcard('='));
            } else if (u.wildcard != v.wildcard) {
              add('<', (u.wildcard == '-' ? u : v).withWildcard('='), (u.wildcard == '+' ? u : v).withWildcard('='));
            }
          }
        }
      }
    }
  }
}
