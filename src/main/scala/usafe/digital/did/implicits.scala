package usafe.digital.did

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.all._
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, JsonObject}
import io.circe.syntax._
import org.http4s.Uri
import org.http4s.circe.{ encodeUri, decodeUri }
import usafe.digital.did.types.Authentication._
import usafe.digital.did.types.PublicKeyEncoding._
import usafe.digital.did.types._
import usafe.digital.did.types.PublicKeyType._

object implicits {

  implicit val showMethodName: Show[MethodName] = Show.show(_.value)
  implicit val showMethodSpecificId: Show[MethodSpecificId] = Show.show(_.value)
  implicit val showPath: Show[Path] = Show.show(_.value)
  implicit val showParameter: Show[Parameter] = Show.show { p => s"${ p.key }=${ p.value }" }
  implicit val showParameters: Show[Parameters] = Show.show { ps =>
    ps.asMultiMap.map { case (k, vs) =>
      vs.map(v => s"$k=$v")
    }.mkString("&")
  }
  implicit val showFragment: Show[Fragment] = Show.show(_.value)

  implicit val showPKEncopdeing: Show[PublicKeyEncoding] = Show.show {
    case PublicKeyPem => "publicKeyPem"
    case PublicKeyJwk => "publicKeyJwk"
    case PublicKeyHex => "publicKeyHex"
    case PublicKeyBase64 => "publicKeyBase64"
    case PublicKeyBase58 => "publicKeyBase58"
    case PublicKeyMultibase => "publicKeyMultibase"
    case EthereumAddress => "ethereumAddress"
  }

  implicit val showPublicKeyType: Show[PublicKeyType] = Show.show {
    case Ed25519VerificationKey2018 => "Ed25519VerificationKey2018"
    case RsaVerificationKey2018 => "RsaVerificationKey2018"
    case EcdsaKoblitzSignature2016 => "RsaVerificationKey2018"
    case EcdsaSecp256k1VerificationKey2019 => "RsaVerificationKey2018"
  }

  implicit val showDid: Show[Did] = Show.show { d =>
    s"did:${ d.methodName.show }:${ d.methodSpecificId.show }${ d.path.fold("") { _.show } }${ s"${ d.queryParameters.fold("") { ps => s"?${ ps.show }" } }" }${ d.fragment.fold("") { f => s"#${ f.show }" } }"
  }

  implicit val didEncoder: Encoder[Did] = Encoder.encodeString.contramap(_.show)

  implicit val publicKeyTypeEncoder: Encoder[PublicKeyType] = Encoder.encodeString.contramap(_.show)

  implicit val publicKeyEncoder: Encoder[PublicKey] = Encoder.encodeJsonObject.contramapObject { pk =>
    JsonObject(
      "id" := pk.id,
      "type" := pk.`type`,
      "controller" := pk.controller,
      pk.publicKeyValue.publicKeyEncoding.show := pk.publicKeyValue.value
    )
  }

  implicit val serviceEncoder: Encoder[Service] = Encoder.encodeJsonObject.contramapObject { s =>
    JsonObject(
      "id" := s.id,
      "type" := s.`type`,
      "serviceEndpoint" := s.serviceEndpoint
    )
  }

  implicit val authenticationEncoder: Encoder[Authentication] = Encoder.instance {
    case Referenced(did) => did.asJson
    case Embedded(pk) => pk.asJson
  }

  implicit val didDocumentEncoder: Encoder.AsObject[DidDocument] = Encoder.encodeJsonObject.contramapObject { dd =>
    JsonObject(
      "@context" := dd.context,
      "id" := dd.id,
      "publicKey" := dd.publicKey,
      "authentication" := dd.authentication,
      "controller" := dd.controller,
      "service" := dd.service
    )
  }

  implicit val didDecoder: Decoder[Did] = Decoder.instance { c =>
    c.as[String].flatMap { raw =>
      Did.fromString(raw).leftMap(df => DecodingFailure(df.reason, c.history))
    }
  }

  implicit val publicKeyDecoder: Decoder[PublicKey] = Decoder.instance { c =>
    for {
      id <- c.downField("id").as[Did]
      tp <- c.downField("type").as[String].flatMap(PublicKeyType.fromString).leftMap(e => DecodingFailure(e.getMessage, c.history))
      ctrl <- c.downField("controller").as[Did]
      pkAttr <- pkAttr(c)
      pkEnc <- PublicKeyEncoding.fromString(pkAttr).leftMap(e => DecodingFailure(e.getMessage, c.history))
      pkVal  <- c.downField(pkAttr).as[String]
    } yield PublicKey(id, tp, ctrl, PublicKeyValue(pkVal, pkEnc))
  }

  implicit val embeddedAuthDecoder: Decoder[Embedded] = Decoder.instance { _.as[PublicKey].map(Embedded) }
  implicit val referencedAuthDecoder: Decoder[Referenced] = Decoder.instance { _.as[Did].map(Referenced) }

  implicit val authenticationDecoder: Decoder[Authentication] = List[Decoder[Authentication]](
    Decoder[Embedded].widen,
    Decoder[Referenced].widen
  ).reduceLeft(_ or _)

  implicit val serviceDecoder: Decoder[Service] = Decoder.instance { c =>
    for {
      id <- c.downField("id").as[Did]
      tp <- c.downField("type").as[String]
      ep <- c.downField("serviceEndpoint").as[Uri]
    } yield Service(id, tp, ep)
  }

  private val singleContextElementDecoder: Decoder[NonEmptyList[Uri]] = Decoder.instance {
    _.as[Uri].map(uri => NonEmptyList(uri, Nil))
  }

  private val multiContextElementDecoder: Decoder[NonEmptyList[Uri]] = Decoder.instance { c =>
    c.as[List[Uri]].flatMap { l =>
      Either.fromOption(NonEmptyList.fromList(l), DecodingFailure("@context is mandatory", c.history))
    }
  }

  implicit val ctxDecoder: Decoder[NonEmptyList[Uri]] = singleContextElementDecoder or multiContextElementDecoder


  implicit val DidDocumentDecoder: Decoder[DidDocument] = Decoder.instance { c =>
    for {
      ctx <- c.downField("@context").as[NonEmptyList[Uri]]
      id <- c.downField("id").as[Did]
      pk <- c.downField("publicKey").as[Option[List[PublicKey]]].map(_.flatMap(NonEmptyList.fromList)) //TODO: enforce non-empty
      auth <- c.downField("authentication").as[Option[List[Authentication]]].map(_.flatMap(NonEmptyList.fromList)) //TODO: enforce non-empty
      ctrl <- c.downField("controller").as[Option[Did]]
      svc <- c.downField("service").as[Option[List[Service]]].map(_.flatMap(NonEmptyList.fromList)) //TODO: enforce non-empty
    } yield DidDocument(ctx, id, pk, auth, ctrl, svc)
  }

  private def pkAttr(c: ACursor): Either[DecodingFailure, String] =
    c.keys.fold[Either[DecodingFailure, String]](DecodingFailure("No JSON keys at all.", c.history).asLeft) { ki =>
      Either.fromOption(
        ki.find { k => k.startsWith("publicKey") || k.startsWith("ehtereum") },
        DecodingFailure("No supported public key encodings found", c.history)
      )
    }


}
