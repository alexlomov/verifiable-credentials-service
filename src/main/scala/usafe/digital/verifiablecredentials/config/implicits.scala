package usafe.digital.verifiablecredentials.config

import cats.syntax.either._
import org.http4s.Uri
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

object implicits {

  implicit val uriReader: ConfigReader[Uri] = ConfigReader[String].emap { s =>
    Uri.fromString(s).leftMap { e =>
      CannotConvert(s, "org.http4s.Uri", e.sanitized)
    }
  }

}
