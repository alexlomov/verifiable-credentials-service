package usafe.digital.verifiablecredentials.didregistry

import cats.data.Kleisli
import cats.syntax.show._
import cats.effect.Sync
import org.http4s.{EntityDecoder, Uri}
import org.http4s.client.Client
import usafe.digital.did.types.{Did, DidDocument}

object registryClient {

  import usafe.digital.did.implicits.showDid

  def getDocumentByKeyId[F[_] : Sync](pkId: Did)
    (implicit didDocumentEntityDecoder: EntityDecoder[F, List[DidDocument]]): Kleisli[F, (Client[F], Uri), List[DidDocument]] =
    Kleisli { case (client, uri) =>
      val reqUri = (uri / "did-documents").withQueryParam("creator", pkId.show.replace("#", "%23"))
      client.expect[List[DidDocument]](reqUri)
    }

}
