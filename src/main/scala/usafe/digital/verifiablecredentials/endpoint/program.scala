package usafe.digital.verifiablecredentials.endpoint


import cats.data.{Kleisli, NonEmptyList}
import cats.effect.Sync
import cats.syntax.all._
import io.circe.{Decoder, Encoder}
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}
import usafe.digital.did.types.{Did, DidDocument, PublicKey}
import usafe.digital.proof.types.{Proof, SanitizedProof}
import usafe.digital.verifiablecredentials.accounts.AccountsClient
import usafe.digital.verifiablecredentials.accounts.types.{AccountId, UserAccount}
import usafe.digital.verifiablecredentials.endpoint.types.{DuplicateKeys, UnknownCreator}
import usafe.digital.verifiablecredentials.types.VerifiableCredentialsRequest

object program {

  def getAccountCredentials[F[_]: Sync](
    accountId: AccountId
  )(
    implicit accClient: AccountsClient[F, (Client[F], Uri), UserAccount]
  ): Kleisli[F, (Client[F], Uri), UserAccount] = Kleisli { ctx =>
      accClient.getAccount(accountId).run(ctx)
  }

  def getCreatorPublicKey[F[_]: Sync](
    publicKeyId: Did
  ) (
    implicit docsDecoder: EntityDecoder[F, List[DidDocument]]
  ): Kleisli[F, (Client[F], Uri), PublicKey] = Kleisli { ctx =>
    import usafe.digital.verifiablecredentials.didregistry.registryClient
    val maybePks = for {
      docs <- registryClient.getDocumentByKeyId(publicKeyId).run(ctx)
      doc <- docs match {
        case d :: Nil =>
          d.pure
        case _ =>
          UnknownCreator(publicKeyId).raiseError[F, DidDocument]
      }
      pks = doc.publicKey
    } yield pks

    maybePks.flatMap {
      case Some(NonEmptyList(head, Nil)) =>
        head.pure
      case Some(nel) =>
        DuplicateKeys(publicKeyId, nel.size).raiseError[F, PublicKey]
      case _ =>
        UnknownCreator(publicKeyId).raiseError[F, PublicKey]
    }
  }

  def verifyCredentialsRequest[F[_]: Sync](
    verifiableCredentialsRequest: VerifiableCredentialsRequest,
    publicKey: PublicKey
  )(
    implicit verifiableCredentialsRequestEncoder: Encoder[VerifiableCredentialsRequest],
    proofEncoder: Encoder[SanitizedProof],
    proofDecoder: Decoder[Proof]
  ): F[Boolean] = {
    import usafe.digital.proof.ops._
    for {
      keyBytes <- publicKey.publicKeyValue.value.base64Bytes
      keySpec = keyBytes.publicKeySpec
      r <- verifyProof(verifiableCredentialsRequest, keySpec)
    } yield r
  }



}