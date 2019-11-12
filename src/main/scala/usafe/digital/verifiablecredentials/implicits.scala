package usafe.digital.verifiablecredentials

import cats.{Applicative, Show}
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.all._
import io.circe.{Decoder, Encoder, JsonObject, Printer}
import io.circe.syntax._
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.http4s.circe.{decodeUri, encodeUri, jsonEncoderWithPrinterOf, jsonOf}
import usafe.digital.did.types.Did
import usafe.digital.proof.types.Proof
import usafe.digital.verifiablecredentials.accounts.types.AccountId
import usafe.digital.verifiablecredentials.types._
import usafe.digital.verifiablecredentials.types.Claim._

object implicits {

  import usafe.digital.proof.implicits.{ encodeProof, decodeProof }
  import usafe.digital.did.implicits.{ didEncoder, didDecoder }

  implicit val showCredential: Show[Claim] = Show.show {
    case NameClaim => "name"
    case AddressClaim => "address"
    case PhoneNumberClaim => "phoneNumber"
    case EmailClaim => "email"
  }

  implicit val encodeAccountId: Encoder[AccountId] = Encoder.encodeString.contramap { _.id }

  implicit val encodeCredential: Encoder[Claim] = Encoder.encodeString.contramap { _.show }

  implicit val encodeRequestedParameters: Encoder[RequestedParameters] = Encoder.instance { rps =>
    rps.params.asJson
  }

  implicit val encodeCredentialSubject: Encoder[RequestCredentialSubject] = Encoder.encodeJsonObject.contramapObject { cs =>
    JsonObject(
      "accountId" := cs.accountId,
      "requestedParameters" := cs.requestedParameters
    )
  }

  implicit val encodeVerifiableCredentialsRequest: Encoder[VerifiableCredentialsRequest] =
    Encoder.encodeJsonObject.contramapObject { vcr =>
      JsonObject(
        "credentialSubject" := vcr.credentialSubject,
        "proof" := vcr.proof
      )
    }

  implicit val decodeClaim: Decoder[Claim] = Decoder.decodeString.emap {
    case "name" => NameClaim.asRight
    case "address" => AddressClaim.asRight
    case "phoneNumber" => PhoneNumberClaim.asRight
    case "email" => EmailClaim.asRight
    case x => s"Unsupported credential $x".asLeft
  }

  implicit val decodeAccountId: Decoder[AccountId] = Decoder.decodeString.map(AccountId)

  implicit val decodeRequestedParameters: Decoder[RequestedParameters] = Decoder.instance {
    _.as[List[Claim]]
  }.emap { cs =>
    Either.fromOption(
      NonEmptyList.fromList(cs),
      ifNone = "Empty requested parameters."
    ).map(RequestedParameters)
  }

  implicit val decodeCredentialSubject: Decoder[RequestCredentialSubject] = Decoder.instance { c =>
    for {
      accId <- c.downField("accountId").as[AccountId]
      reqParams <- c.downField("requestedParameters").as[RequestedParameters]
    } yield RequestCredentialSubject(accId, reqParams)
  }

  implicit val decodeVerifiableCredentialsRequest: Decoder[VerifiableCredentialsRequest] = Decoder.instance { c =>
    for {
      credSubj <- c.downField("credentialSubject").as[RequestCredentialSubject]
      proof <- c.downField("proof").as[Proof]
    } yield VerifiableCredentialsRequest(credSubj, proof)
  }

  implicit def decodeVerifiableCredentialsRequestEntity[F[_]: Sync]: EntityDecoder[F, VerifiableCredentialsRequest] =
    jsonOf

  implicit val encodeExpirationDate: Encoder[ExpirationDate] = Encoder.encodeZonedDateTime.contramap { _.value }
  implicit val encodeIssuanceDate: Encoder[IssuanceDate] = Encoder.encodeZonedDateTime.contramap { _.value }

  implicit val encodeCredentialsIssuer: Encoder.AsObject[CredentialsIssuer] = Encoder.encodeJsonObject.contramapObject { ci =>
    JsonObject(
      "id" := ci.id,
      "name" := ci.name
    )
  }

  implicit val encodeResponseCredentialSubject: Encoder.AsObject[ResponseCredentialSubject] =
    Encoder.encodeJsonObject.contramapObject { rcs =>
      JsonObject.fromFoldable(
        rcs.claims.map { vc =>
          vc.name.show := vc.value
        }
      ) .+:  { "id" := rcs.id }
    }

  implicit val encodeVerifiableCredentialType: Encoder[VerifiableCredentialType] =
    Encoder.encodeString.contramap {
      case VerifiableCredentialType.VerifiableCredential => "VerifiableCredential"
      case VerifiableCredentialType.VerifiableCredentialEncrypted => "VerifiableCredentialEncrypted"
    }

  implicit val encodeVerifiableCredentialsId: Encoder[VerifiableCredentialsId] = Encoder.encodeString.contramap { _.id }

  implicit val encodeVerifiableCredentials: Encoder.AsObject[VerifiableCredentials] =
    Encoder.encodeJsonObject.contramapObject { vc =>
      JsonObject(
        "@context" := vc.context,
        "id" := vc.id,
        "type" := vc.`type`,
        "credentialSubject" := vc.credentialSubject,
        "issuer" := vc.issuer,
        "issuanceDate" := vc.issuanceDate,
        "expirationDate" := vc.expirationDate,
        "proof" := vc.proof
      )
    }

  implicit def encodeVerifiableCredentialsEntity[F[_]: Applicative]: EntityEncoder[F, VerifiableCredentials] =
    jsonEncoderWithPrinterOf(Printer.noSpaces.copy(dropNullValues = true))

  implicit val decodeVerifiableCredentials: Decoder[VerifiableCredentials] = Decoder.instance { c =>
    for {
      ctx <- c.downField("@context").as[NonEmptyList[Uri]]
      id <- c.downField("id").as[VerifiableCredentialsId]
      tp <- c.downField("type").as[VerifiableCredentialType]
      credSubj <- c.downField("credentialSubject").as[ResponseCredentialSubject]
      issuer <- c.downField("issuer").as[CredentialsIssuer]
      issuanceDate <- c.downField("issuanceDate").as[IssuanceDate]
      expirationDate <- c.downField("expirationDate").as[Option[ExpirationDate]]
      proof <- c.downField("proof").as[Option[Proof]]
    } yield VerifiableCredentials(ctx, id, tp, credSubj, issuer, issuanceDate, expirationDate, proof)
  }

  implicit val decodeVerifiableCredentialsId: Decoder[VerifiableCredentialsId] =
    Decoder.decodeString.map(VerifiableCredentialsId.apply)

  implicit val decodeResponseCredentialSubject: Decoder[ResponseCredentialSubject] = Decoder.instance { c =>
    val claims = for {
      email <- c.downField("email").as[Option[String]].map(_.map(EmailClaim -> _))
      phone <- c.downField("phoneNumber").as[Option[String]].map(_.map(PhoneNumberClaim -> _))
      name <- c.downField("name").as[Option[String]].map(_.map(NameClaim -> _))
      address <- c.downField("address").as[Option[String]].map(_.map(AddressClaim -> _))
    } yield List(email, phone, name, address)

    val mClaims = claims.map {
      _.flatten.map {  case (c, v) => VerifiableClaim(c, v) }
    }

    for {
      id <- c.downField("id").as[Option[Did]]
      mcs <- mClaims
    } yield ResponseCredentialSubject(id, mcs)
  }

  implicit val decodeVerifiableCredentialType: Decoder[VerifiableCredentialType] =
    Decoder.decodeString.emap {
      case "VerifiableCredential" => VerifiableCredentialType.VerifiableCredential.asRight
      case "VerifiableCredentialEncrypted" => VerifiableCredentialType.VerifiableCredentialEncrypted.asRight
      case x => s"Unsupported credential type $x".asLeft
    }

  implicit val decodeCredentialsIssuer: Decoder[CredentialsIssuer] = Decoder.instance { c =>
    for {
      id <- c.downField("id").as[Did]
      name <- c.downField("name").as[String]
    } yield CredentialsIssuer(id, name)
  }

  implicit val decodeIssuanceDate: Decoder[IssuanceDate] = Decoder.decodeZonedDateTime.map(IssuanceDate)

  implicit val decodeExpirationDate: Decoder[ExpirationDate] = Decoder.decodeZonedDateTime.map(ExpirationDate)

}
