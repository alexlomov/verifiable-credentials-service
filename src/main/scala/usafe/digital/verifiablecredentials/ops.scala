package usafe.digital.verifiablecredentials

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}

import cats.effect.{Resource, Sync}
import cats.syntax.all._
import usafe.digital.proof.ops.{asBytesOps, asStringOps}

object ops {

  def loadKeys[F[_]: Sync]: F[(PKCS8EncodedKeySpec, X509EncodedKeySpec)] = for {
    priv <- getKeyBytes("key")
    pub <- getKeyBytes("key.pub")
    privK = priv.privateKeySpec
    pubK = pub.publicKeySpec
  } yield(privK, pubK)

  private def loadResource[F[_]: Sync](name: String): F[Array[Byte]] =
    Resource.fromAutoCloseable(
      Sync[F].delay(getClass.getClassLoader.getResourceAsStream(name))
    )
      .use { is =>
      if (is eq null)
        Sync[F].raiseError[Array[Byte]](new RuntimeException(s"Not available: $name"))
      else
      Sync[F].delay(
        LazyList.continually(is.read()).takeWhile(_ != -1).map(_.toByte).toArray
      )
    }


  private def getKeyBytes[F[_]: Sync](keyName: String): F[Array[Byte]] = for {
    bs <- loadResource(keyName)
    base64Bs <- sanitizePemBytes(bs)
  } yield base64Bs

  def sanitizePemBytes[F[_]: Sync](pemKey: Array[Byte]): F[Array[Byte]] =
    sanitizePemString(new String(pemKey)).base64Bytes


  def sanitizePemString(pemKey: String): String =
    pemKey.replaceAll("-----((BEGIN)|(END))[A-Z\\s]+-----", "").replace("\n", "")

}
