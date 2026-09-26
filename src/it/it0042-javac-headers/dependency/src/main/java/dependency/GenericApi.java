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
package dependency;

public enum GenericApi implements Text, Ordered<GenericApi>, Container.Bound<Payload>, Container.Contract<String> {
  VALUE {
    public TextList<?> boundedItems() {
      return null;
    }

    public java.util.List<String> items() {
      return null;
    }

    public String get() {
      return null;
    }

    public <U extends String> U value(U input) {
      return null;
    }
  };

  public native void call();
}
