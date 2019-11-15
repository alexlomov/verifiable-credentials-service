package usafe.digital.verifiablecredentials

import java.time.{ZoneId, ZonedDateTime}

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.all._
import io.circe.syntax._
import org.scalatest.{FunSpecLike, Matchers}
import org.http4s.implicits._
import usafe.digital.did.types.Did
import usafe.digital.proof.types.SanitizedProof
import usafe.digital.verifiablecredentials.types._

class CryptoSuite extends FunSpecLike with Matchers {

  describe("Verifiable Credential cryptography") {
    it("verifies own proof") {

      import usafe.digital.proof.{ ops => proof }
      import usafe.digital.proof.implicits._
      import usafe.digital.verifiablecredentials.implicits._

      val created = ZonedDateTime.now
      val vc = VerifiableCredentials(
        context = NonEmptyList.one(uri"https://w3id.org/did/v1"),
        id = VerifiableCredentialsId("the_most_unique_identifier"),
       `type` = VerifiableCredentialType.VerifiableCredential,
        credentialSubject = ResponseCredentialSubject(
          Did.fromStringUnsafe("did:usafe:credential-subject-id").some,
          List(
            VerifiableClaim(Claim.NameClaim, "John Doe"),
            VerifiableClaim(Claim.EmailClaim, "john@doe.digital"),
            VerifiableClaim(Claim.AddressClaim, "Hollywood 90210"),
            VerifiableClaim(Claim.PhoneNumberClaim, "223-322")
          )
        ),
        issuer = CredentialsIssuer(
          id = Did.fromStringUnsafe("did:usafe:evilware"),
          name = "Evilware Inc."
        ),
        issuanceDate = IssuanceDate(created),
        expirationDate = ExpirationDate(ZonedDateTime.of(2021, 12, 1, 0, 0, 59, 0, ZoneId.of("UTC"))).some,
        none
      )

      val sproof = SanitizedProof(
        Did.fromStringUnsafe("did:usafe:evilware"),
        created
      )

      val verified = for {
        ks <- ops.loadKeys[IO]
        (privK, pubK) = ks
        signed <- proof.signDocument[IO, VerifiableCredentials](vc, sproof, privK)
        _ <- IO.delay {
          info(signed.asJson.spaces2)
        }
        verified <- proof.verifyProof[IO, VerifiableCredentials](signed, pubK)
      } yield verified

      verified.unsafeRunSync() shouldBe true

    }
  }

}
