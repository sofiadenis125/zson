/*
 * Copyright (C) 2020 ZeoFlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeoflow.zson.functional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.zeoflow.zson.Zson;
import com.zeoflow.zson.ZsonBuilder;
import com.zeoflow.zson.TypeAdapter;
import com.zeoflow.zson.TypeAdapterFactory;
import com.zeoflow.zson.annotations.JsonAdapter;
import com.zeoflow.zson.reflect.TypeToken;
import com.zeoflow.zson.stream.JsonReader;
import com.zeoflow.zson.stream.JsonWriter;

import junit.framework.TestCase;

/**
 * Functional tests for the {@link JsonAdapter} annotation on fields.
 */
public final class JsonAdapterAnnotationOnFieldsTest extends TestCase {
  public void testClassAnnotationAdapterTakesPrecedenceOverDefault() {
    Zson zson = new Zson();
    String json = zson.toJson(new Computer(new User("Inderjeet Singh")));
    assertEquals("{\"user\":\"UserClassAnnotationAdapter\"}", json);
    Computer computer = zson.fromJson("{'user':'Inderjeet Singh'}", Computer.class);
    assertEquals("UserClassAnnotationAdapter", computer.user.name);
  }

  public void testClassAnnotationAdapterFactoryTakesPrecedenceOverDefault() {
    Zson zson = new Zson();
    String json = zson.toJson(new Gizmo(new Part("Part")));
    assertEquals("{\"part\":\"GizmoPartTypeAdapterFactory\"}", json);
    Gizmo computer = zson.fromJson("{'part':'Part'}", Gizmo.class);
    assertEquals("GizmoPartTypeAdapterFactory", computer.part.name);
  }

  public void testRegisteredTypeAdapterTakesPrecedenceOverClassAnnotationAdapter() {
    Zson zson = new ZsonBuilder()
        .registerTypeAdapter(User.class, new RegisteredUserAdapter())
        .create();
    String json = zson.toJson(new Computer(new User("Inderjeet Singh")));
    assertEquals("{\"user\":\"RegisteredUserAdapter\"}", json);
    Computer computer = zson.fromJson("{'user':'Inderjeet Singh'}", Computer.class);
    assertEquals("RegisteredUserAdapter", computer.user.name);
  }

  public void testFieldAnnotationTakesPrecedenceOverRegisteredTypeAdapter() {
    Zson zson = new ZsonBuilder()
      .registerTypeAdapter(Part.class, new TypeAdapter<Part>() {
        @Override public void write(JsonWriter out, Part part) throws IOException {
          throw new AssertionError();
        }
        @Override public Part read(JsonReader in) throws IOException {
          throw new AssertionError();
        }
      }).create();
    String json = zson.toJson(new Gadget(new Part("screen")));
    assertEquals("{\"part\":\"PartJsonFieldAnnotationAdapter\"}", json);
    Gadget gadget = zson.fromJson("{'part':'screen'}", Gadget.class);
    assertEquals("PartJsonFieldAnnotationAdapter", gadget.part.name);
  }

  public void testFieldAnnotationTakesPrecedenceOverClassAnnotation() {
    Zson zson = new Zson();
    String json = zson.toJson(new Computer2(new User("Inderjeet Singh")));
    assertEquals("{\"user\":\"UserFieldAnnotationAdapter\"}", json);
    Computer2 target = zson.fromJson("{'user':'Interjeet Singh'}", Computer2.class);
    assertEquals("UserFieldAnnotationAdapter", target.user.name);
  }

  private static final class Gadget {
    @JsonAdapter(PartJsonFieldAnnotationAdapter.class)
    final Part part;
    Gadget(Part part) {
      this.part = part;
    }
  }

  private static final class Gizmo {
    @JsonAdapter(GizmoPartTypeAdapterFactory.class)
    final Part part;
    Gizmo(Part part) {
      this.part = part;
    }
  }

  private static final class Part {
    final String name;
    public Part(String name) {
      this.name = name;
    }
  }

  private static class PartJsonFieldAnnotationAdapter extends TypeAdapter<Part> {
    @Override public void write(JsonWriter out, Part part) throws IOException {
      out.value("PartJsonFieldAnnotationAdapter");
    }
    @Override public Part read(JsonReader in) throws IOException {
      in.nextString();
      return new Part("PartJsonFieldAnnotationAdapter");
    }
  }

  private static class GizmoPartTypeAdapterFactory implements TypeAdapterFactory {
    @Override public <T> TypeAdapter<T> create(Zson zson, final TypeToken<T> type) {
      return new TypeAdapter<T>() {
        @Override public void write(JsonWriter out, T value) throws IOException {
          out.value("GizmoPartTypeAdapterFactory");
        }
        @SuppressWarnings("unchecked")
        @Override public T read(JsonReader in) throws IOException {
          in.nextString();
          return (T) new Part("GizmoPartTypeAdapterFactory");
        }
      };
    }
  }

  private static final class Computer {
    final User user;
    Computer(User user) {
      this.user = user;
    }
  }

  @JsonAdapter(UserClassAnnotationAdapter.class)
  private static class User {
    public final String name;
    private User(String name) {
      this.name = name;
    }
  }

  private static class UserClassAnnotationAdapter extends TypeAdapter<User> {
    @Override public void write(JsonWriter out, User user) throws IOException {
      out.value("UserClassAnnotationAdapter");
    }
    @Override public User read(JsonReader in) throws IOException {
      in.nextString();
      return new User("UserClassAnnotationAdapter");
    }
  }

  private static final class Computer2 {
    // overrides the JsonAdapter annotation of User with this
    @JsonAdapter(UserFieldAnnotationAdapter.class)
    final User user;
    Computer2(User user) {
      this.user = user;
    }
  }

  private static final class UserFieldAnnotationAdapter extends TypeAdapter<User> {
    @Override public void write(JsonWriter out, User user) throws IOException {
      out.value("UserFieldAnnotationAdapter");
    }
    @Override public User read(JsonReader in) throws IOException {
      in.nextString();
      return new User("UserFieldAnnotationAdapter");
    }
  }

  private static final class RegisteredUserAdapter extends TypeAdapter<User> {
    @Override public void write(JsonWriter out, User user) throws IOException {
      out.value("RegisteredUserAdapter");
    }
    @Override public User read(JsonReader in) throws IOException {
      in.nextString();
      return new User("RegisteredUserAdapter");
    }
  }

  public void testJsonAdapterInvokedOnlyForAnnotatedFields() {
    Zson zson = new Zson();
    String json = "{'part1':'name','part2':{'name':'name2'}}";
    GadgetWithTwoParts gadget = zson.fromJson(json, GadgetWithTwoParts.class);
    assertEquals("PartJsonFieldAnnotationAdapter", gadget.part1.name);
    assertEquals("name2", gadget.part2.name);
  }

  private static final class GadgetWithTwoParts {
    @JsonAdapter(PartJsonFieldAnnotationAdapter.class) final Part part1;
    final Part part2; // Doesn't have the JsonAdapter annotation
    @SuppressWarnings("unused") GadgetWithTwoParts(Part part1, Part part2) {
      this.part1 = part1;
      this.part2 = part2;
    }
  }

  public void testJsonAdapterWrappedInNullSafeAsRequested() {
    Zson zson = new Zson();
    String fromJson = "{'part':null}";

    GadgetWithOptionalPart gadget = zson.fromJson(fromJson, GadgetWithOptionalPart.class);
    assertNull(gadget.part);

    String toJson = zson.toJson(gadget);
    assertFalse(toJson.contains("PartJsonFieldAnnotationAdapter"));
  }

  private static final class GadgetWithOptionalPart {
    @JsonAdapter(value = PartJsonFieldAnnotationAdapter.class)
    final Part part;

    private GadgetWithOptionalPart(Part part) {
      this.part = part;
    }
  }

  /** Regression test contributed through https://github.com/google/Zson/issues/831 */
  public void testNonPrimitiveFieldAnnotationTakesPrecedenceOverDefault() {
    Zson zson = new Zson();
    String json = zson.toJson(new GadgetWithOptionalPart(new Part("foo")));
    assertEquals("{\"part\":\"PartJsonFieldAnnotationAdapter\"}", json);
    GadgetWithOptionalPart gadget = zson.fromJson("{'part':'foo'}", GadgetWithOptionalPart.class);
    assertEquals("PartJsonFieldAnnotationAdapter", gadget.part.name);
  }

  /** Regression test contributed through https://github.com/google/Zson/issues/831 */
  public void testPrimitiveFieldAnnotationTakesPrecedenceOverDefault() {
    Zson zson = new Zson();
    String json = zson.toJson(new GadgetWithPrimitivePart(42));
    assertEquals("{\"part\":\"42\"}", json);
    GadgetWithPrimitivePart gadget = zson.fromJson(json, GadgetWithPrimitivePart.class);
    assertEquals(42, gadget.part);
  }

  private static final class GadgetWithPrimitivePart {
    @JsonAdapter(LongToStringTypeAdapterFactory.class)
    final long part;

    private GadgetWithPrimitivePart(long part) {
      this.part = part;
    }
  }

  private static final class LongToStringTypeAdapterFactory implements TypeAdapterFactory {
    static final TypeAdapter<Long> ADAPTER = new TypeAdapter<Long>() {
      @Override public void write(JsonWriter out, Long value) throws IOException {
        out.value(value.toString());
      }
      @Override public Long read(JsonReader in) throws IOException {
        return in.nextLong();
      }
    };
    @SuppressWarnings("unchecked")
    @Override public <T> TypeAdapter<T> create(Zson zson, final TypeToken<T> type) {
      Class<?> cls = type.getRawType();
      if (Long.class.isAssignableFrom(cls)) {
        return (TypeAdapter<T>) ADAPTER;
      } else if (long.class.isAssignableFrom(cls)) {
        return (TypeAdapter<T>) ADAPTER;
      }
      throw new IllegalStateException("Non-long field of type " + type
          + " annotated with @JsonAdapter(LongToStringTypeAdapterFactory.class)");
    }
  }

  public void testFieldAnnotationWorksForParameterizedType() {
    Zson zson = new Zson();
    String json = zson.toJson(new Gizmo2(Arrays.asList(new Part("Part"))));
    assertEquals("{\"part\":\"GizmoPartTypeAdapterFactory\"}", json);
    Gizmo2 computer = zson.fromJson("{'part':'Part'}", Gizmo2.class);
    assertEquals("GizmoPartTypeAdapterFactory", computer.part.get(0).name);
  }

  private static final class Gizmo2 {
    @JsonAdapter(Gizmo2PartTypeAdapterFactory.class)
    List<Part> part;
    Gizmo2(List<Part> part) {
      this.part = part;
    }
  }

  private static class Gizmo2PartTypeAdapterFactory implements TypeAdapterFactory {
    @Override public <T> TypeAdapter<T> create(Zson zson, final TypeToken<T> type) {
      return new TypeAdapter<T>() {
        @Override public void write(JsonWriter out, T value) throws IOException {
          out.value("GizmoPartTypeAdapterFactory");
        }
        @SuppressWarnings("unchecked")
        @Override public T read(JsonReader in) throws IOException {
          in.nextString();
          return (T) Arrays.asList(new Part("GizmoPartTypeAdapterFactory"));
        }
      };
    }
  }
}
