package usafe.digital.verifiablecredentials

import java.time.Instant

import cats.data.NonEmptyList
import org.http4s.Uri
import usafe.digital.did.types.Did
import usafe.digital.proof.types.Proof
import usafe.digital.verifiablecredentials.accounts.types.AccountId

import scala.util.control.NoStackTrace

object types {

  sealed trait Claim extends Product with Serializable

  object Claim {
    case object NameClaim extends Claim
    case object AddressClaim extends Claim
    case object PhoneClaim extends Claim
    case object EmailClaim extends Claim
    case object AgeClaim extends Claim
  }

  final case class VerifiableCredentialsRequest(
    credentialSubject: RequestCredentialSubject,
    proof: Proof
  )

  final case class RequestCredentialSubject(
    accountId: AccountId,
    requestedParameters: RequestedParameters
  )

  final case class RequestedParameters(params: NonEmptyList[Claim])


  final case class VerifiableCredentials(
    context: NonEmptyList[Uri],
    id: VerifiableCredentialsId,
    `type`: VerifiableCredentialType,
    credentialSubject: ResponseCredentialSubject,
    issuer: CredentialsIssuer,
    issuanceDate: IssuanceDate,
    expirationDate: Option[ExpirationDate],
    proof: Option[Proof]
  )

  final case class VerifiableCredentialsId(id: String)
  object VerifiableCredentialsId {
    private val r = new scala.util.Random(42)
    def newId: VerifiableCredentialsId = VerifiableCredentialsId(r.alphanumeric.take(10).mkString)
  }

  sealed trait VerifiableCredentialType extends Product with Serializable

  object VerifiableCredentialType {
    case object VerifiableCredential extends VerifiableCredentialType
    case object VerifiableCredentialEncrypted extends VerifiableCredentialType
  }


  //TODO: Cutting the edge, value has to be polymorphic
  final case class VerifiableClaim(
    name: Claim,
    value: String
  )

  final case class ResponseCredentialSubject(
    id: Option[Did],
    claims: List[VerifiableClaim]
  )

  final case class CredentialsIssuer(
    id: Did,
    name: String
  )

  final case class IssuanceDate(value: Instant)

  final case class ExpirationDate(value: Instant)


  //TODO: Nice, but later
  trait CredentialValueEncoder[A] {
    def encode(value: A): String
  }

  trait CredentialValueDecoder[A] {
    def decode(repr: String): Either[CredentialDecodingFailure, A]
  }

  final case class CredentialDecodingFailure(message: String) extends NoStackTrace


}
