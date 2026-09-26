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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Substitutes retained generic contracts without loading their signature types.
 */
final class JniSignature extends SignatureVisitor {
  final Map<String, List<Value>> bounds = new LinkedHashMap<String, List<Value>>();
  final List<Value> parents = new ArrayList<Value>();
  final List<Value> parameters = new ArrayList<Value>();
  final List<Value> exceptions = new ArrayList<Value>();
  Value result;
  private String formal;

  private JniSignature() {
    super(Opcodes.ASM9);
  }

  static JniSignature read(String signature) {
    JniSignature result = new JniSignature();
    if (signature != null) {
      new SignatureReader(signature).accept(result);
    }
    return result;
  }

  @Override
  public void visitFormalTypeParameter(String name) {
    formal = name;
    bounds.put(name, new ArrayList<Value>());
  }

  @Override
  public SignatureVisitor visitClassBound() {
    return add(bounds.get(formal));
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    return add(bounds.get(formal));
  }

  @Override
  public SignatureVisitor visitSuperclass() {
    return add(parents);
  }

  @Override
  public SignatureVisitor visitInterface() {
    return add(parents);
  }

  @Override
  public SignatureVisitor visitParameterType() {
    return add(parameters);
  }

  @Override
  public SignatureVisitor visitReturnType() {
    result = new Value();
    return result;
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    return add(exceptions);
  }

  private static Value add(List<Value> values) {
    Value value = new Value();
    values.add(value);
    return value;
  }

  Map<String, Value> bind(List<Value> arguments, Map<String, Value> enclosing) {
    Map<String, Value> result = new LinkedHashMap<String, Value>();
    int index = 0;
    for (String variable : bounds.keySet()) {
      Value argument = arguments.get(index++);
      if (argument.wildcard != '=') {
        Value captured = argument.withWildcard(argument.wildcard);
        captured.limits = new ArrayList<Value>(argument.limits);
        argument = captured;
      }
      result.put(variable, argument);
    }
    Map<String, Value> scope = new HashMap<String, Value>(enclosing);
    scope.putAll(result);
    // Wildcard upper bounds include the declaration's bounds, substituted
    // simultaneously. Keep them while projecting through subsequent supertypes.
    for (String variable : bounds.keySet()) {
      Value argument = result.get(variable);
      if (argument.wildcard != '=') {
        for (Value bound : bounds.get(variable)) {
          argument.limits.add(bound.substitute(scope));
        }
      }
    }
    return result;
  }

  Map<String, Value> variables() {
    return variables(java.util.Collections.<String, Value> emptyMap());
  }

  private Map<String, Value> variables(Map<String, Value> scope) {
    return variables(scope, "");
  }

  Map<String, Value> variables(Map<String, Value> scope, String prefix) {
    Map<String, Value> result = new LinkedHashMap<String, Value>();
    for (String name : bounds.keySet()) {
      Value value = new Value();
      value.variable = name;
      value.erasure = value.substitute(scope, bounds, new HashSet<String>()).erase();
      value.variable = prefix + name;
      result.put(name, value);
    }
    Map<String, Value> symbols = new HashMap<String, Value>(scope);
    symbols.putAll(result);
    for (String name : bounds.keySet()) {
      for (Value bound : bounds.get(name)) {
        result.get(name).limits.add(bound.substitute(symbols));
      }
    }
    return result;
  }

  JniClass.Method method(JniClass.Method method, Map<String, Value> arguments) {
    Map<String, Value> scope = new HashMap<String, Value>(arguments);
    if (bounds.isEmpty()) {
      return specializeMethod(method, scope);
    }
    // A method type parameter shadows a class/interface parameter of the same name.
    for (String variable : bounds.keySet()) {
      scope.remove(variable);
    }
    // Receiver arguments may contain a caller variable with the same name as
    // an unrelated method formal. Rename the formal before substitution, while
    // its occurrences can still be distinguished from those caller variables.
    Set<String> symbols = new HashSet<String>();
    for (Value argument : scope.values()) {
      argument.scopeVariables(symbols);
    }
    Set<String> reserved = new HashSet<String>(symbols);
    reserved.addAll(scope.keySet());
    reserved.addAll(bounds.keySet());
    Map<String, String> names = new HashMap<String, String>();
    int index = 0;
    for (String variable : bounds.keySet()) {
      if (symbols.contains(variable)) {
        String fresh;
        do {
          fresh = "_NarFormal" + index++;
        } while (!reserved.add(fresh));
        names.put(variable, fresh);
      }
    }
    return (names.isEmpty() ? this : rename(names)).specializeMethod(method, scope);
  }

  private JniClass.Method specializeMethod(JniClass.Method method, Map<String, Value> scope) {
    // Keep source variables symbolic, with their specialized erasures recorded
    // separately for method matching. Erasing <U extends T> to T in the source
    // does not necessarily override the original generic method.
    scope.putAll(variables(scope));
    Type[] args = new Type[parameters.size()];
    StringBuilder source = new StringBuilder();
    JniSignature resolved = new JniSignature();
    if (!bounds.isEmpty()) {
      source.append('<');
      for (Map.Entry<String, List<Value>> formal : bounds.entrySet()) {
        source.append(formal.getKey());
        List<Value> limits = new ArrayList<Value>();
        for (Value bound : formal.getValue()) {
          Value value = bound.substitute(scope);
          limits.add(value);
          source.append(':').append(value.methodSignature());
        }
        resolved.bounds.put(formal.getKey(), limits);
      }
      source.append('>');
    }
    source.append('(');
    for (int i = 0; i < args.length; i++) {
      Value value = parameters.get(i).substitute(scope);
      args[i] = value.erase();
      resolved.parameters.add(value);
      source.append(value.methodSignature());
    }
    Value returns = result.substitute(scope);
    source.append(')').append(returns.methodSignature());
    resolved.result = returns;
    List<String> thrown = new ArrayList<String>();
    // javac can omit ordinary throws clauses from Signature; Exceptions then
    // remains authoritative. When present, generic throws need the same receiver
    // substitution as parameters and returns, before interface compatibility
    // checks.
    if (exceptions.isEmpty()) {
      thrown.addAll(method.exceptions);
    }
    for (Value exception : exceptions) {
      Value value = exception.substitute(scope);
      resolved.exceptions.add(value);
      thrown.add(value.erase().getInternalName());
      source.append('^').append(value.methodSignature());
    }
    return new JniClass.Method(method.access, method.name, Type.getMethodDescriptor(returns.erase(), args),
        source.toString(), resolved, thrown);
  }

  JniSignature renameConstructor(Set<String> reserved) {
    Map<String, String> names = new HashMap<String, String>();
    int index = 0;
    for (String name : bounds.keySet()) {
      String fresh;
      do {
        fresh = "_NarConstructor" + index++;
      } while (!reserved.add(fresh));
      names.put(name, fresh);
    }
    return rename(names);
  }

  JniSignature rename(Map<String, String> names) {
    JniSignature renamed = new JniSignature();
    for (Map.Entry<String, List<Value>> formal : bounds.entrySet()) {
      List<Value> values = new ArrayList<Value>();
      for (Value bound : formal.getValue()) {
        values.add(bound.rename(names));
      }
      String name = names.containsKey(formal.getKey()) ? names.get(formal.getKey()) : formal.getKey();
      renamed.bounds.put(name, values);
    }
    for (Value parent : parents) {
      renamed.parents.add(parent.rename(names));
    }
    for (Value parameter : parameters) {
      renamed.parameters.add(parameter.rename(names));
    }
    for (Value exception : exceptions) {
      renamed.exceptions.add(exception.rename(names));
    }
    renamed.result = result == null ? null : result.rename(names);
    return renamed;
  }

  JniSignature constructorFormals() {
    return constructorFormals(parameters);
  }

  JniSignature constructorFormals(List<Value> arguments) {
    JniSignature result = new JniSignature();
    Set<String> needed = new HashSet<String>();
    for (Value argument : arguments) {
      if (argument != null) {
        argument.variables(needed);
      }
    }
    int previous;
    do {
      previous = needed.size();
      for (String name : new HashSet<String>(needed)) {
        if (bounds.containsKey(name)) {
          for (Value bound : bounds.get(name)) {
            bound.variables(needed);
          }
        }
      }
    } while (needed.size() != previous);
    for (String name : bounds.keySet()) {
      if (needed.contains(name)) {
        result.bounds.put(name, bounds.get(name));
      }
    }
    return result;
  }

  static final class Value extends SignatureVisitor {
    String name;
    Value owner;
    String variable;
    private Type erasure;
    Value component;
    char wildcard = '=';
    // Symbolic bounds survive substitution. Copies share this list so recursive
    // bounds can refer to symbols before all their bounds have been populated.
    List<Value> limits = new ArrayList<Value>();
    final List<Value> arguments = new ArrayList<Value>();

    Value() {
      super(Opcodes.ASM9);
    }

    static Value object(String name) {
      Value value = new Value();
      value.name = name;
      return value;
    }

    static Value type(Type type) {
      Value value = new Value();
      new SignatureReader(type.getDescriptor()).acceptType(value);
      return value;
    }

    boolean same(Value other) {
      return methodSignature().equals(other.methodSignature());
    }

    Value rename(Map<String, String> names) {
      Value value = withWildcard(wildcard);
      if (names.containsKey(variable)) {
        value.variable = names.get(variable);
      }
      if (owner != null) {
        value.owner = owner.rename(names);
      }
      if (component != null) {
        value.component = component.rename(names);
      }
      value.arguments.clear();
      for (Value argument : arguments) {
        value.arguments.add(argument.rename(names));
      }
      return value;
    }

    @Override
    public void visitBaseType(char descriptor) {
      erasure = Type.getType(String.valueOf(descriptor));
    }

    @Override
    public void visitTypeVariable(String name) {
      variable = name;
    }

    @Override
    public SignatureVisitor visitArrayType() {
      component = new Value();
      return component;
    }

    @Override
    public void visitClassType(String name) {
      this.name = name;
    }

    @Override
    public void visitInnerClassType(String name) {
      Value enclosing = object(this.name);
      enclosing.owner = owner;
      enclosing.arguments.addAll(arguments);
      owner = enclosing;
      this.name += "$" + name;
      arguments.clear();
    }

    @Override
    public void visitTypeArgument() {
      Value value = add(arguments);
      value.wildcard = '*';
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
      Value value = add(arguments);
      value.wildcard = wildcard;
      return value;
    }

    Value substitute(Map<String, Value> scope) {
      return substitute(scope, java.util.Collections.<String, List<Value>> emptyMap(), new HashSet<String>());
    }

    private Value substitute(Map<String, Value> scope, Map<String, List<Value>> bounds, Set<String> visiting) {
      if (variable != null) {
        if (scope.containsKey(variable)) {
          Value argument = scope.get(variable);
          // Inheriting Collection<E> from List<? extends T> keeps the wildcard.
          return argument.withWildcard(wildcard == '=' ? argument.wildcard : wildcard);
        }
        List<Value> limits = bounds.get(variable);
        if (limits != null && !limits.isEmpty() && visiting.add(variable)) {
          Value value = limits.get(0).substitute(scope, bounds, visiting);
          visiting.remove(variable);
          return value.withWildcard(wildcard);
        }
        return object("java/lang/Object").withWildcard(wildcard);
      }
      Value value = new Value();
      value.name = name;
      value.erasure = erasure;
      value.wildcard = wildcard;
      if (owner != null) {
        value.owner = owner.substitute(scope, bounds, visiting);
      }
      if (component != null) {
        value.component = component.substitute(scope, bounds, visiting);
      }
      for (Value argument : arguments) {
        value.arguments.add(argument.substitute(scope, bounds, visiting));
      }
      return value;
    }

    Value withWildcard(char wildcard) {
      Value value = new Value();
      value.name = name;
      value.variable = variable;
      value.erasure = erasure;
      value.limits = limits;
      value.component = component;
      value.owner = owner;
      value.arguments.addAll(arguments);
      value.wildcard = wildcard;
      return value;
    }

    Type erase() {
      if (erasure != null) {
        return erasure;
      }
      if (name != null) {
        return Type.getObjectType(name);
      }
      if (component != null) {
        return Type.getType("[" + component.erase().getDescriptor());
      }
      return Type.getObjectType("java/lang/Object");
    }

    private String methodSignature() {
      if (wildcard == '*') {
        return "*";
      }
      String prefix = wildcard == '=' ? "" : String.valueOf(wildcard);
      if (variable != null) {
        return prefix + "T" + variable + ";";
      }
      if (component != null) {
        return prefix + "[" + component.methodSignature();
      }
      if (name == null) {
        return prefix + erase().getDescriptor();
      }
      StringBuilder text = new StringBuilder(prefix);
      if (owner == null) {
        text.append('L').append(name);
      } else {
        String enclosing = owner.methodSignature();
        text.append(enclosing, 0, enclosing.length() - 1).append('.').append(name.substring(owner.name.length() + 1));
      }
      if (!arguments.isEmpty()) {
        text.append('<');
        for (Value argument : arguments) {
          text.append(argument.methodSignature());
        }
        text.append('>');
      }
      return text.append(';').toString();
    }

    Value eraseArguments(Set<String> rawNames) {
      Value value = new Value();
      value.name = name;
      value.variable = variable;
      value.erasure = erasure;
      value.wildcard = wildcard;
      value.limits = limits;
      if (owner != null) {
        value.owner = owner.eraseArguments(rawNames);
      }
      if (component != null) {
        value.component = component.eraseArguments(rawNames);
      }
      if (!rawNames.contains(name)) {
        for (Value argument : arguments) {
          value.arguments.add(argument.eraseArguments(rawNames));
        }
      }
      return value;
    }

    List<Value> upperBounds() {
      List<Value> result = new ArrayList<Value>();
      upperBounds(result, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<List<Value>, Boolean>()));
      return result;
    }

    private void upperBounds(List<Value> result, Set<List<Value>> visited) {
      if (wildcard == '=') {
        result.add(this);
        return;
      }
      // Dependent formals can link captures with identical printed wildcards;
      // follow their bound identities, and stop recursive bounds without erasing
      // them.
      if (!visited.add(limits)) {
        return;
      }
      if (wildcard == '+') {
        result.add(withWildcard('='));
      }
      if (limits.isEmpty()) {
        result.add(object("java/lang/Object"));
      }
      for (Value bound : limits) {
        bound.upperBounds(result, visited);
      }
    }

    void variables(Set<String> names) {
      if (variable != null) {
        names.add(variable);
      }
      if (owner != null) {
        owner.variables(names);
      }
      if (component != null) {
        component.variables(names);
      }
      for (Value argument : arguments) {
        argument.variables(names);
      }
    }

    void scopeVariables(Set<String> names) {
      scopeVariables(names, java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Value, Boolean>()));
    }

    private void scopeVariables(Set<String> names, Set<Value> visited) {
      if (!visited.add(this)) {
        return;
      }
      if (variable != null) {
        names.add(variable);
      }
      if (owner != null) {
        owner.scopeVariables(names, visited);
      }
      if (component != null) {
        component.scopeVariables(names, visited);
      }
      for (Value argument : arguments) {
        argument.scopeVariables(names, visited);
      }
      // Symbolic and captured bounds can refer back to their own variables.
      for (Value bound : limits) {
        bound.scopeVariables(names, visited);
      }
    }

    void classNames(Set<String> names) {
      if (name != null) {
        names.add(name);
      }
      if (owner != null) {
        owner.classNames(names);
      }
      if (component != null) {
        component.classNames(names);
      }
      for (Value argument : arguments) {
        argument.classNames(names);
      }
    }

    String source(JniClassPath metadata) throws IOException {
      return source(metadata, null);
    }

    String source(JniClassPath metadata, JniSourceNames names) throws IOException {
      if (wildcard == '*') {
        return "?";
      }
      String prefix = wildcard == '+' ? "? extends " : wildcard == '-' ? "? super " : "";
      if (variable != null) {
        return prefix + variable;
      }
      if (component != null) {
        return prefix + component.source(metadata, names) + "[]";
      }
      if (name == null) {
        return prefix + erase().getClassName();
      }
      StringBuilder text = new StringBuilder(prefix);
      text.append(owner == null ? (names == null ? metadata.sourceName(name) : names.name(name))
          : names == null ? owner.source(metadata) + "." + metadata.resolve(name).simple() : names.member(this));
      if (!arguments.isEmpty()) {
        text.append('<');
        for (int i = 0; i < arguments.size(); i++) {
          if (i != 0) {
            text.append(", ");
          }
          text.append(arguments.get(i).source(metadata, names));
        }
        text.append('>');
      }
      return text.toString();
    }

    @Override
    public String toString() {
      return wildcard + ":" + (variable != null ? "T" + variable : name != null ? name : erase().getDescriptor()) + ":"
          + owner + arguments + (component == null ? "" : "[" + component);
    }
  }
}
