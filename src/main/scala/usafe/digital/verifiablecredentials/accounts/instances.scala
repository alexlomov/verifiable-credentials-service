package usafe.digital.verifiablecredentials.accounts

import cats.data.Kleisli
import org.http4s.client.Client
import usafe.digital.verifiablecredentials.accounts.types.{AccountId, UserAccount}

object instances {

  implicit def demoAccountClient[F[_]](accountId: AccountId): Kleisli[F, Client[F], UserAccount] = Kleisli { c =>

  }

}