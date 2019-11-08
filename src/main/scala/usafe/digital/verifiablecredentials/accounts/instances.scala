package usafe.digital.verifiablecredentials.accounts

import cats.data.Kleisli
import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import usafe.digital.verifiablecredentials.accounts.types.{AccountId, UserAccount}

object instances {

  object demo {

    import usafe.digital.verifiablecredentials.accounts.implicits.userAccountEntityDecoder

    implicit def demoAccountClient[F[_]: Sync](accountId: AccountId): Kleisli[F, (Client[F], Uri), UserAccount] =
      Kleisli { case (client, uri) =>
        client.expect[UserAccount](uri / "accounts" / accountId.id)
      }

  }

}