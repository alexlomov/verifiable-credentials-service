package usafe.digital.verifiablecredentials.config

import cats.syntax.either._
import org.http4s.Uri
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader}
import pureconfig.error.CannotConvert
import pureconfig.generic.ProductHint

object implicits {

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), allowUnknownKeys = true)

  implicit val uriReader: ConfigReader[Uri] = ConfigReader[String].emap { s =>
    Uri.fromString(s).leftMap { e =>
      CannotConvert(s, "org.http4s.Uri", e.sanitized)
    }
  }

}
