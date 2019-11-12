package usafe.digital.verifiablecredentials.endpoint

import cats.Applicative
import io.circe.{Encoder, JsonObject}
import io.circe.syntax._
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf
import usafe.digital.verifiablecredentials.endpoint.types.CoreError

object implicits {

  implicit val encoderCoreError: Encoder[CoreError] = Encoder.encodeJsonObject.contramapObject { ce =>
    JsonObject(
      "errorCode" := ce.errorCode,
      "errorDescription" := ce.errorDescription
    )
  }

  implicit def encodeCoreErrorEntity[F[_]: Applicative]: EntityEncoder[F, CoreError] = jsonEncoderOf

}
