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
  implicit val phoneDecoder: Decoder[Phone] = Decoder.decodeString.map(Phone)
  implicit val userAccountDecoder: Decoder[UserAccount] = Decoder.instance { c =>
    for {
      id <- c.downField("accountId").as[AccountId]
      email <- c.downField("email").as[Email]
      name <- c.downField("name").as[Name]
      address <- c.downField("address").as[Address]
      age <- c.downField("age").as[Age]
      phoneNumber <- c.downField("phone").as[Phone]
    } yield UserAccount(id, email, name, address, age, phoneNumber)
  }


  implicit def userAccountEntityDecoder[F[_]: Sync]: EntityDecoder[F, UserAccount] = jsonOf



}
