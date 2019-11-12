package usafe.digital.verifiablecredentials.endpoint

import usafe.digital.did.types.Did

import scala.util.control.NoStackTrace

object types {

  final case class UnknownCreator(creator: Did) extends NoStackTrace

  final case class DuplicateKeys(creator: Did, quantity: Int) extends NoStackTrace

  final case class CoreError(
    errorCode: Int,
    errorDescription: String
  )

}
