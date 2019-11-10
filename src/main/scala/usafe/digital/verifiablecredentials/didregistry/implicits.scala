package usafe.digital.verifiablecredentials.didregistry

import cats.effect.Sync
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import usafe.digital.did.types.DidDocument

object implicits {
  import usafe.digital.did.implicits.DidDocumentDecoder

  implicit def didDocumentEntityDecoder[F[_]: Sync]: EntityDecoder[F, DidDocument] = jsonOf
  implicit def didDocumentListEntityDecoder[F[_]: Sync]: EntityDecoder[F, List[DidDocument]] = jsonOf

}
