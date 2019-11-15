package usafe.digital.verifiablecredentials.endpoint

import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZonedDateTime

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all._
import org.http4s.client.Client
import org.http4s.{HttpRoutes, MediaType}
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import usafe.digital.did.types.{Did, Fragment}
import usafe.digital.proof.types.SanitizedProof
import usafe.digital.verifiablecredentials.config.types.AppConfig
import usafe.digital.verifiablecredentials.endpoint.types.CoreError
import usafe.digital.verifiablecredentials.types._
import usafe.digital.verifiablecredentials.types.Claim._

object http {

  def postVerifiableCredentialsRequest[F[_]: Sync](
    cfg: AppConfig,
    privateKey: PKCS8EncodedKeySpec,
    client: Client[F]
  ): HttpRoutes[F] = {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
    import usafe.digital.proof.{ ops => proof }
    import proof.{ asBytesOps, asStringOps }
    import usafe.digital.verifiablecredentials.implicits.decodeVerifiableCredentialsRequestEntity
    import usafe.digital.verifiablecredentials.didregistry.implicits.didDocumentListEntityDecoder
    import usafe.digital.verifiablecredentials.endpoint.implicits.encodeCoreErrorEntity
    import usafe.digital.verifiablecredentials.accounts.instances.demo._
    import usafe.digital.verifiablecredentials.implicits.{
      encodeVerifiableCredentials,
      encodeVerifiableCredentialsEntity,
      encodeVerifiableCredentialsRequest,
      decodeVerifiableCredentials
    }
    import usafe.digital.proof.implicits.{ decodeProof, encodeSanitizedProof, encodeProof }

    HttpRoutes.of {
      case post @ POST -> Root / "verifiable-credentials"
        if post.contentType.fold(false) { _.mediaType == MediaType.application.json } =>

        for {
          vcsDid <- Sync[F].fromEither(
            Did.fromString("did:corporation:vcs")
          )
          vcr <- post.as[VerifiableCredentialsRequest]
          creKey <- program.getCreatorPublicKey(vcr.proof.creator).run((client, cfg.didRegistryHost))
          creKeyBytes <- usafe.digital.verifiablecredentials.ops.sanitizePemString(
            creKey.publicKeyValue.value
          ).base64Bytes // Typeclasses against the public key encoding must rock here
          creKeyX509 = creKeyBytes.publicKeySpec
          isValid <- proof.verifyProof(vcr, creKeyX509)
          _ <- if (isValid) {
            Sync[F].unit
          } else {
            Forbidden(CoreError(403, "Wrong proof"))
          }
          acc <- program.getAccountCredentials(vcr.credentialSubject.accountId)
            .run((client, cfg.credentialsProviderHost))

          accDid <- Sync[F].fromEither(
            Did.fromString(s"did:corporation:${vcr.credentialSubject.accountId}")
          )

          vc = VerifiableCredentials(
            context = NonEmptyList.one(uri"https://www.w3.org/2018/credentials/v1"),
            id = VerifiableCredentialsId.newId,
            `type` = VerifiableCredentialType.VerifiableCredential,
            credentialSubject = ResponseCredentialSubject(
              id = accDid.some,
              claims = List(
                VerifiableClaim(EmailClaim, acc.email.value),
                VerifiableClaim(NameClaim, acc.name.value),
                VerifiableClaim(AddressClaim, acc.address.value),
                VerifiableClaim(PhoneNumberClaim, acc.phoneNumber.value)
              )
            ),
            issuer = CredentialsIssuer(vcsDid, "Evilware"),
            issuanceDate = IssuanceDate(ZonedDateTime.now),
            expirationDate = None,
            proof = None
          )

          vcProven <- proof.signDocument(
            vc,
            SanitizedProof(vcsDid.copy(fragment = Fragment("key-1").some), ZonedDateTime.now),
            privateKey
          )
          resp <- Created(vcProven)
        } yield resp

    }
  }

}
