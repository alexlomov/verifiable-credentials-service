package usafe.digital.did

import cats.Show
import cats.syntax.all._
import io.circe.{Decoder, DecodingFailure, Encoder}
import usafe.digital.did.types._

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

  implicit val showDid: Show[Did] = Show.show { d =>
    s"did:${ d.methodName.show }:${ d.methodSpecificId.show }${ d.path.fold("") { _.show } }${ s"${ d.queryParameters.fold("") { ps => s"?${ ps.show }" } }" }${ d.fragment.fold("") { f => s"#${ f.show }" } }"
  }

  implicit val didEncoder: Encoder[Did] = Encoder.encodeString.contramap(_.show)

  implicit val didDecoder: Decoder[Did] = Decoder.instance { c =>
    c.as[String].flatMap { raw =>
      Did.fromString(raw).leftMap(df => DecodingFailure(df.reason, c.history))
    }
  }

}
