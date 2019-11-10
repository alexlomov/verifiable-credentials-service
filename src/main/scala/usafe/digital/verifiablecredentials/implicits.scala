package usafe.digital.verifiablecredentials

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.all._
import io.circe.{ Decoder, Encoder, JsonObject }
import io.circe.syntax._
import usafe.digital.proof.types.Proof
import usafe.digital.verifiablecredentials.accounts.types.AccountId
import usafe.digital.verifiablecredentials.types.{Credential, RequestCredentialSubject, RequestedParameters, VerifiableCredentialsRequest}
import usafe.digital.verifiablecredentials.types.Credential._

object implicits {

  import usafe.digital.proof.implicits.{ encodeProof, decodeProof }

  implicit val showCredential: Show[Credential] = Show.show {
    case NameCredential => "name"
    case AddressCredential => "address"
    case PhoneNumberCredential => "phoneNumber"
    case EmailCredential => "email"
  }

  implicit val encodeAccountId: Encoder[AccountId] = Encoder.encodeString.contramap { _.id }

  implicit val encodeCredential: Encoder[Credential] = Encoder.encodeString.contramap { _.show }

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

  implicit val decodeCredential: Decoder[Credential] = Decoder.decodeString.emap {
    case "name" => NameCredential.asRight
    case "address" => AddressCredential.asRight
    case "phoneNumber" => PhoneNumberCredential.asRight
    case "email" => EmailCredential.asRight
    case x => s"Unsupported credential $x".asLeft
  }

  implicit val decodeAccountId: Decoder[AccountId] = Decoder.decodeString.map(AccountId)

  implicit val decodeRequestedParameters: Decoder[RequestedParameters] = Decoder.instance {
    _.as[List[Credential]]
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

}
