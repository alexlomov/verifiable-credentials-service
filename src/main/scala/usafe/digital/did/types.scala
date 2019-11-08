package usafe.digital.did

import cats.data.NonEmptyList
import cats.syntax.all._

import scala.util.control.NoStackTrace

object types {

  type DocOptionalArray[+A] = Option[NonEmptyList[A]]

  type ParsedDid = Either[DidValidationFault, Did]

  final case class RawDid(did: String)

  final case class MethodName(value: String)

  final case class MethodSpecificId(value: String)

  final case class Path(value: String)

  final case class Fragment(value: String)

  final case class Parameter(key: String, value: String)

  final case class Parameters(private val ps: NonEmptyList[Parameter]) {
    def asList: List[Parameter] = ps.toList
    def asMultiMap: Map[String, scala.collection.immutable.Seq[String]] = ps.toList.groupMap(_.key)(_.value)
  }


  final case class Did (
    methodName: MethodName,
    methodSpecificId: MethodSpecificId,
    methodParameters: DocOptionalArray[Parameter] = none,
    path: Option[Path] = none,
    queryParameters: DocOptionalArray[Parameter] = none,
    fragment: Option[Fragment] = none,
  )


  final case class DidValidationFault(reason: String) extends NoStackTrace {
    override val getMessage: String = reason
  }


  object Did {
    def fromString(input: String): Either[DidValidationFault, Did] =
      Either.fromTry {
        new DidParser(input).InputLine.run()
      }.leftMap(e => DidValidationFault(e.getMessage))

    def fromStringUnsafe(input: String): Did = fromString(input).getOrElse(throw new IllegalArgumentException(input))

    def fromRawDid(rawDid: RawDid): Either[DidValidationFault, Did] = fromString(rawDid.did)
  }

}
