package usafe.digital.proof

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, MessageDigest, Signature}

import cats.MonadError
import cats.syntax.flatMap._
import cats.syntax.functor._
import usafe.digital.proof.types.SecurityProviderEvidence

class Sha256WithRsa[F[_] : MonadError[*[_], Throwable]] private [proof](implicit SPE: SecurityProviderEvidence)
  extends ProofAlgebra[F, PKCS8EncodedKeySpec, X509EncodedKeySpec] {

  final val SHA256 = valueOf["SHA-256"]
  final val RSA = valueOf["RSA"]
  final val SHA256withRSAPSS = valueOf["SHA256withRSA"]


  private val F = MonadError[F, Throwable]
  private val kf: F[KeyFactory] = F.catchNonFatal(KeyFactory.getInstance(RSA))

  override def hash(data: Array[Byte]): F[Array[Byte]] = for {
    d <- F.catchNonFatal(MessageDigest.getInstance(SHA256, SPE.providerCode))
    h = d.digest(data)
  } yield h

  override def sign(privateKey: PrivateKeyType, data: Array[Byte]): F[Array[Byte]] = for {
    s <- signature
    f <- kf
    k = f.generatePrivate(privateKey)
    _ = s.initSign(k)
    _ = s.update(data)
    res = s.sign()
  } yield res


  override def verify(publicKey: PublicKeyType, data: Array[Byte], signatureToVerify: Array[Byte]): F[Boolean] = for {
    s <- signature
    f <- kf
    k = f.generatePublic(publicKey)
    _ = s.initVerify(k)
    _ = s.update(data)
    eq = s.verify(signatureToVerify)
  } yield eq

  private def signature: F[Signature] = for {
    s <- F.catchNonFatal(Signature.getInstance(SHA256withRSAPSS, SPE.providerCode))
  } yield s

}

object Sha256WithRsa {
  def apply[F[_]: MonadError[*[_], Throwable]](implicit SPE: SecurityProviderEvidence): Sha256WithRsa[F] = new Sha256WithRsa
}



