package usafe.digital.verifiablecredentials

import cats.data.NonEmptyList

object types {

  sealed trait Credential extends Product with Serializable

  object Credential {
    case object NameCredential extends Credential
    case object AddressCredential extends Credential
    case object PhoneNumberCredential extends Credential
    case object EmailCredential extends Credential
  }

  final case class VerifiableCredentialsRequest(
    credentialSubject: CredentialSubject
  )

  final case class CredentialSubject(
    requestedParameters: RequestedParameters
  )

  final case class RequestedParameters(params: NonEmptyList[Credential])

}
