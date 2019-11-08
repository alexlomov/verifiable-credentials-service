package usafe.digital.verifiablecredentials.config

import org.http4s.Uri
import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint

object types {

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase), allowUnknownKeys = true)

  final case class HttpPort(value: Int) extends AnyVal

  final case class AppConfig(
    httpPort: HttpPort,
    credentialsProviderHost: Uri,
    didRegistryHost: Uri
  )


}
