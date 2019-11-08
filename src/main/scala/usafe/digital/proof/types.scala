package usafe.digital.proof

import java.security.Security
import java.time.ZonedDateTime

import org.bouncycastle.jce.provider.BouncyCastleProvider
import usafe.digital.did.types.Did

object types {

  sealed trait ProofSuiteType extends Product with Serializable

  case object RsaSignature2018 extends ProofSuiteType

  final case class ProofValue(value: String)

  final case class Proof(
    `type`: ProofSuiteType,
    creator: Did,
    created: ZonedDateTime,
    proofValue: ProofValue
  )

  final case class SanitizedProof(
    creator: Did,
    created: ZonedDateTime
  )

  trait SecurityProviderEvidence {
    def providerCode: String
  }

  object SecurityProviderEvidence {
    implicit val ev: SecurityProviderEvidence = {
      if (Security.getProvider("BC") == null)
        Security.addProvider(new BouncyCastleProvider())
      new SecurityProviderEvidence {
        override def providerCode: String = valueOf["BC"]
      }
    }
  }

}
