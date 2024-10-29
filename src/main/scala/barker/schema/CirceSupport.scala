package barker.schema

import io.circe.{Codec, HCursor, Json}
import barker.entities.UserId
import io.circe.Decoder.Result
import java.util.UUID

/** Circe codecs for basic domain types
  *
  * TODO: not sure if they really belong here, it would be massively easier to just do this inside entities package ...
  */
object CirceSupport:
  given Codec[UserId] = new Codec[UserId]:
    override def apply(c: HCursor): Result[UserId] = c.as[String].map(s => UserId(UUID.fromString(s)))

    override def apply(a: UserId): Json = Json.fromString(a.value.toString)
