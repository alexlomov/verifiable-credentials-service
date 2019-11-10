package usafe.digital.verifiablecredentials

import java.time.ZonedDateTime

import cats.data.NonEmptyList
import org.http4s.Uri
import usafe.digital.did.types.Did
import usafe.digital.proof.types.Proof
import usafe.digital.verifiablecredentials.accounts.types.AccountId

import scala.util.control.NoStackTrace

object types {

  sealed trait Credential extends Product with Serializable

  object Credential {
    case object NameCredential extends Credential
    case object AddressCredential extends Credential
    case object PhoneNumberCredential extends Credential
    case object EmailCredential extends Credential
  }

  final case class VerifiableCredentialsRequest(
    credentialSubject: RequestCredentialSubject,
    proof: Proof
  )

  final case class RequestCredentialSubject(
    accountId: AccountId,
    requestedParameters: RequestedParameters
  )

  final case class RequestedParameters(params: NonEmptyList[Credential])


  final case class VerifiableCredentials(
    context: NonEmptyList[Uri],
    id: VerifiableCredentialsId,
    `type`: VerifiableCredentialType,
    credentialSubject: ResponseCredentialSubject,
    issuer: CredentialsIssuer,
    issuanceDate: IssuanceDate,
    expirationDate: Option[ExpirationDate]
  )

  final case class VerifiableCredentialsId(id: String)

  sealed trait VerifiableCredentialType extends Product with Serializable

  object VerifiableCredentialType {
    case object VerifiableCredential extends VerifiableCredentialType
    case object VerifiableCredentialEncrypted extends VerifiableCredentialType
  }


  //TODO: Cutting the edge, value has to be polymorphic
  final case class VerifiableCredential(
    name: Credential,
    value: String
  )

  final case class ResponseCredentialSubject(
    id: Option[Did],
    credentials: List[VerifiableCredential]
  )

  final case class CredentialsIssuer(
    id: Did,
    name: String
  )

  final case class IssuanceDate(value: ZonedDateTime)

  final case class ExpirationDate(value: ZonedDateTime)


  //TODO: Nice, but later
  trait CredentialValueEncoder[A] {
    def encode(value: A): String
  }

  trait CredentialValueDecoder[A] {
    def decode(repr: String): Either[CredentialDecodingFailure, A]
  }

  final case class CredentialDecodingFailure(message: String) extends NoStackTrace


}
