/*
 * Original implementation (C) 2009-2011 Debasish Ghosh
 * Adapted and extended in 2011 by Mathias Doenitz
 *
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
 */

package sjsonnew

/**
  * Provides additional JsonFormats and helpers
 */
trait AdditionalFormats {

  /**
   * Constructs a JsonFormat from its two parts, JsonReader and JsonWriter.
   */
  def jsonFormat[A](reader: JsonReader[A], writer: JsonWriter[A]) = new JsonFormat[A] {
    def write[J](obj: A, builder: Builder[J]): Unit = writer.write(obj, builder)
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = reader.read(jsOpt, unbuilder)
  }

  /**
   * Constructs a RootJsonFormat from its two parts, RootJsonReader and RootJsonWriter.
   */
  def rootJsonFormat[T](reader: RootJsonReader[T], writer: RootJsonWriter[T]) =
    rootFormat(jsonFormat(reader, writer))

  /**
   * Turns a JsonWriter into a JsonFormat that throws an UnsupportedOperationException for reads.
   */
  def lift[A](writer: JsonWriter[A]) = new JsonFormat[A] {
    def write[J](obj: A, builder: Builder[J]): Unit = writer.write(obj, builder)
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A =
      throw new UnsupportedOperationException("JsonReader implementation missing")
  }

  /**
   * Turns a RootJsonWriter into a RootJsonFormat that throws an UnsupportedOperationException for reads.
   */
  def lift[A](writer: RootJsonWriter[A]): RootJsonFormat[A] =
    rootFormat(lift(writer: JsonWriter[A]))

  /**
   * Turns a JsonReader into a JsonFormat that throws an UnsupportedOperationException for writes.
   */
  def lift[A <: AnyRef](reader: JsonReader[A]) = new JsonFormat[A] {
    def write[J](obj: A, builder: Builder[J]): Unit =
      throw new UnsupportedOperationException("No JsonWriter[" + obj.getClass + "] available")
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = reader.read(jsOpt, unbuilder)
  }

  /**
   * Turns a RootJsonReader into a RootJsonFormat that throws an UnsupportedOperationException for writes.
   */
  def lift[A <: AnyRef](reader: RootJsonReader[A]): RootJsonFormat[A] =
    rootFormat(lift(reader: JsonReader[A]))

  /**
   * Lazy wrapper around serialization. Useful when you want to serialize (mutually) recursive structures.
   */
  def lazyFormat[A](format: => JsonFormat[A]) = new JsonFormat[A] {
    lazy val delegate = format
    def write[J](obj: A, builder: Builder[J]): Unit = delegate.write(obj, builder)
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = delegate.read(jsOpt, unbuilder)
  }

  /**
   * Explicitly turns a JsonFormat into a RootJsonFormat.
   */
  def rootFormat[A](format: JsonFormat[A]) = new RootJsonFormat[A] {
    def write[J](obj: A, builder: Builder[J]): Unit = format.write(obj, builder)
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = format.read(jsOpt, unbuilder)
  }

  /**
   * Wraps an existing JsonReader with Exception protection.
   */
  def safeReader[A: JsonReader] = new JsonReader[Either[Exception, A]] {
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Either[Exception, A] = {
      val reader = implicitly[JsonReader[A]]
      try {
        Right(reader.read(jsOpt, unbuilder))
      } catch {
        case e: Exception => Left(e)
      }
    }
  }

  /**
   * A `JsonFormat` that doesn't write and always return this instance of `a`.
   */
  def asSingleton[A](a: A): JsonFormat[A] = new JsonFormat[A] {
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): A = a
    def write[J](obj: A, builder: Builder[J]): Unit = ()
  }

  /**
   * A `JsonFormat` that can read and write an instance of `T` by using a `JsonFormat` for `U`.
   */
  def project[T, U](f1: T => U, f2: U => T)(implicit fu: JsonFormat[U]): JsonFormat[T] = new JsonFormat[T] {
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): T = f2(fu.read(jsOpt, unbuilder))
    def write[J](obj: T, builder: Builder[J]): Unit = fu.write(f1(obj), builder)
  }

}
