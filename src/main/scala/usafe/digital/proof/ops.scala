package usafe.digital.proof

import java.io.{DataInputStream, File, FileInputStream}
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.Base64

import cats.{ApplicativeError, MonadError}
import cats.syntax.all._
import io.circe.syntax._
import io.circe._
import usafe.digital.proof.types.{Proof, ProofValue, RsaSignature2018, SanitizedProof}

object ops {
  
  type ME[F[_]] = MonadError[F, Throwable]

  class StringOps private[proof] (s: String) {
    def utf8Bytes: Array[Byte] = s.getBytes(StandardCharsets.UTF_8)
    def base64Bytes[F[_]: ApplicativeError[*[_], Throwable]]: F[Array[Byte]] =
      ApplicativeError[F, Throwable].catchNonFatal(
        Base64.getDecoder.decode(s)
      )
  }
  implicit def asStringOps(s: String): StringOps = new StringOps(s)

  class ByteArrayOps private[proof](bytes: Array[Byte]) {
    def base64String: String = Base64.getEncoder.encodeToString(bytes)
    def base64Bytes[F[_]: ApplicativeError[*[_], Throwable]]: F[Array[Byte]] =
      ApplicativeError[F, Throwable].catchNonFatal(
        Base64.getDecoder.decode(bytes)
      )
    def publicKeySpec: X509EncodedKeySpec = new X509EncodedKeySpec(bytes)
    def privateKeySpec: PKCS8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes)
  }
  implicit def asBytesOps(bytes: Array[Byte]): ByteArrayOps = new ByteArrayOps(bytes)

  def sanitizeProof(proof: Proof): SanitizedProof = SanitizedProof(proof.creator, proof.created)

  def canonicalJsonObject(jobj: JsonObject): JsonObject =
    JsonObject.fromIterable(jobj.toVector.sortBy(_._1).map { case (k,j) => k -> canonicalJson(j) })

  def canonicalJson(j: Json): Json = j.fold[Json](
    jsonNull = Json.Null,
    jsonBoolean = (x: Boolean) => Json.fromBoolean(x),
    jsonNumber = (x: JsonNumber) => Json.fromJsonNumber(x),
    jsonString = (x: String) => Json.fromString(x),
    jsonArray = (x: Vector[Json]) => Json.fromValues(x.map(canonicalJson)),
    jsonObject = (x: JsonObject) => Json.fromJsonObject(canonicalJsonObject(x))
  )

  def sanitizeJson(json: Json): Json =
    json.hcursor.downField("proof").delete.as[Json].fold(
      _ => json,
      identity
    )

  def createProofValue[F[_]: ME, A: Encoder]
  (
    doc: A,
    proof: SanitizedProof,
    privateKey: PKCS8EncodedKeySpec
  ) (
    implicit proofEncoder: Encoder[SanitizedProof]
  ): F[ProofValue] = {
    val cry = Sha256WithRsaPss[F]
    for {
      docHash <- cry.hash(
        canonicalJson(doc.asJson).noSpaces.utf8Bytes
      )
      proofHash <- cry.hash(
        canonicalJson(proof.asJson).noSpaces.utf8Bytes
      )
      signature <- cry.sign(privateKey, proofHash ++ docHash)
      enc = signature.base64String
    } yield ProofValue(enc)

  }

  def signDocument[F[_]: ME, A: Encoder.AsObject: Decoder]
  (
    doc: A,
    proof: SanitizedProof,
    privateKey: PKCS8EncodedKeySpec
  )(
    implicit sanitizedProofEncoder: Encoder[SanitizedProof],
    proofEncoder: Encoder[Proof]
  ): F[A] = for {
    pv <- createProofValue(doc, proof, privateKey: PKCS8EncodedKeySpec)
    p = Proof(RsaSignature2018, proof.creator, proof.created, pv)
    dj = doc.asJsonObject
    pj = p.asJson
    j = dj.add("proof", pj)
    res <- MonadError[F, Throwable].fromEither(j.asJson.as[A])
  } yield res

  def getProof[F[_]: ME](
    json: Json
  )(
    implicit proofDecoder: Decoder[Proof]
  ): F[Proof] = MonadError[F, Throwable].fromEither {
    json.hcursor.downField("proof").as[Proof]
  }

  def verifyProof[F[_]: ME, A: Encoder](
    doc: A,
    publicKey: X509EncodedKeySpec
  )(
    implicit proofDecoder: Decoder[Proof],
    sanitizedProofEncoder: Encoder[SanitizedProof]
  ): F[Boolean] = for {
    j <- MonadError[F, Throwable].pure(
      canonicalJson(
        doc.asJson
      )
    )
    proof <- getProof(j)
    sj = sanitizeJson(j)
    cry = Sha256WithRsaPss[F]
    sig <- proof.proofValue.value.base64Bytes
    jh <- cry.hash(sj.noSpaces.utf8Bytes)
    ph <- cry.hash(
      canonicalJson(
        sanitizeProof(proof).asJson
      ).noSpaces.utf8Bytes
    )
    v <- cry.verify(publicKey, ph ++ jh, sig)
  } yield v

  def loadKeys[F[_]: MonadError[*[_], Throwable]]: F[(PKCS8EncodedKeySpec, X509EncodedKeySpec)] = for {
    priv <- getKeyBytes("key")
    pub <- getKeyBytes("key.pub")
    privK = priv.privateKeySpec
    pubK = pub.publicKeySpec
  } yield(privK, pubK)

  private def loadResource[F[_]: MonadError[*[_], Throwable]](name: String): F[URL] = {
    val r = getClass.getClassLoader.getResource(name)
    if (r == null)
      MonadError[F, Throwable].raiseError(new RuntimeException(s"Not available: $name"))
    else
      MonadError[F, Throwable].pure(r)
  }

  private def bytesFromResource[F[_]: MonadError[*[_], Throwable]](url: URL): F[Array[Byte]] =
    MonadError[F, Throwable].catchNonFatal {
      val f = new File(url.toURI)
      val bytes = new Array[Byte](f.length().asInstanceOf[Int])
      new DataInputStream(new FileInputStream(f)).readFully(bytes)
      bytes
    }

  private def getKeyBytes[F[_]: MonadError[*[_], Throwable]](keyName: String): F[Array[Byte]] = for {
    u <- loadResource(keyName)
    bs <- bytesFromResource(u)
    str = new String(bs).replaceAll("-----((BEGIN)|(END))[A-Z\\s]+-----", "").replace("\n", "")
    base64Bs <- str.base64Bytes
  } yield base64Bs

}
