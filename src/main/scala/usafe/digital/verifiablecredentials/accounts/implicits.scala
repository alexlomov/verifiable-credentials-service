package usafe.digital.verifiablecredentials.accounts

import cats.effect.Sync
import io.circe.Decoder
import org.http4s.circe.jsonOf
import org.http4s.EntityDecoder
import usafe.digital.verifiablecredentials.accounts.types._

object implicits {

  implicit val accountIdDecoder: Decoder[AccountId] = Decoder.decodeString.map(AccountId)
  implicit val emailDecoder: Decoder[Email] = Decoder.decodeString.map(Email)
  implicit val nameDecoder: Decoder[Name] = Decoder.decodeString.map(Name)
  implicit val addressDecoder: Decoder[Address] = Decoder.decodeString.map(Address)
  implicit val ageDecoder: Decoder[Age] = Decoder.decodeInt.map(Age)

  implicit def accountIdEntityDecoder[F[_]: Sync]: EntityDecoder[F, AccountId] = jsonOf
  implicit def emailEntityDecoder[F[_]: Sync]: EntityDecoder[F, Email] = jsonOf
  implicit def nameEntityDecoder[F[_]: Sync]: EntityDecoder[F, Name] = jsonOf
  implicit def addressEntityDecoder[F[_]: Sync]: EntityDecoder[F, Address] = jsonOf
  implicit def ageEntityDecoder[F[_]: Sync]: EntityDecoder[F, Age] = jsonOf



}
