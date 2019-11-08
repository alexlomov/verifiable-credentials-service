package usafe.digital.did

import cats.data.NonEmptyList
import org.parboiled2._

class DidParser(val input: ParserInput) extends Parser {

  def DidScheme: Rule0 = rule { "did" }
  def SubDelims: Rule0 = rule { "!" | "$" | "&" | "'" | "(" | ")" | "*" | "+" | "," | ";" | "=" }
  def Unreserved: Rule0 = rule { CharPredicate.Alpha | CharPredicate.Digit | "-" | "." | "_" | "~" }
  def PctEncoded: Rule0 = rule { "%" ~ 2.times(CharPredicate.HexDigit) }
  def Pchar: Rule0 = rule { Unreserved | PctEncoded | ":" | "@" }
  def MethodChars: Rule1[String] = rule { capture(oneOrMore(CharPredicate.LowerAlpha | CharPredicate.Digit)) }
  def MethodName: Rule1[types.MethodName] = rule { MethodChars ~> types.MethodName }
  def MethodSpecificIdChars: Rule1[String] = rule { capture(oneOrMore(CharPredicate.AlphaNum | '_' | '-' | '.')) }
  def MethodSpecificId: Rule1[types.MethodSpecificId] = rule { MethodSpecificIdChars ~> types.MethodSpecificId }
  def PChars: Rule1[String] = rule { capture(oneOrMore(Pchar)) }
  //def ParamKV: Rule2[String, String] = rule { PChars ~ '=' ~ PChars }
  def Param: Rule1[types.Parameter] = rule { PChars ~ '=' ~ PChars ~> { (pk: String, pv: String) => types.Parameter(pk, pv) } }
  def MethodParams: Rule1[Seq[types.Parameter]] = rule { oneOrMore(Param).separatedBy(';') }
  def QueryParams: Rule1[Seq[types.Parameter]] = rule { oneOrMore(Param).separatedBy('&') }
  def FragmentChars: Rule1[String] = rule { capture(oneOrMore(Pchar)) }
  def Fragment: Rule1[types.Fragment] = rule { FragmentChars ~> types.Fragment }
  def Segment = rule { zeroOrMore(Pchar | SubDelims) }
  def PathAbempty: Rule1[types.Path] = rule { capture(oneOrMore("/" ~ Segment)) ~> types.Path }


  def Did: Rule1[types.Did] = rule {
    (DidScheme ~ ':'~ MethodName ~ ':' ~ MethodSpecificId ~ (optional(';' ~ MethodParams) ~ optional(PathAbempty) ~
    optional('?' ~ QueryParams) ~ optional('#' ~ Fragment))) ~> {
      (
      mn: types.MethodName,
      mId: types.MethodSpecificId,
      mps: Option[Seq[types.Parameter]],
      path: Option[types.Path],
      qps: Option[Seq[types.Parameter]],
      f: Option[types.Fragment]
      ) => types.Did(mn, mId, mps.flatMap( l => NonEmptyList.fromList(l.toList)), path, qps.flatMap( l => NonEmptyList.fromList(l.toList)), f)
    }

  }

  def InputLine: Rule1[types.Did] = rule { Did ~ EOI }

}
