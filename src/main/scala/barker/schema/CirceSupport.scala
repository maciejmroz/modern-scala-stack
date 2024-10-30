package barker.schema

import io.circe.{Codec, HCursor, Json}
import barker.entities.{From, Into}
import io.circe.Decoder.Result
import java.util.UUID

/** Circe codecs for basic domain types - might want to move it elsewhere in future, just did not want dependency on
  * Circe in domain code, as JSON encoding/decoding is more of an infrastructure thing
  */
object CirceSupport:
  private type FromUUID[T] = From[UUID, T]
  private type IntoUUID[T] = Into[UUID, T]

  given uuidCodec[T: FromUUID: IntoUUID]: Codec[T] with
    def apply(c: HCursor): Result[T] = c.as[String].map(s => From[UUID, T].from(UUID.fromString(s)))

    def apply(a: T): Json = Json.fromString(Into[UUID, T].into(a).toString)
