package usafe.digital.verifiablecredentials.accounts

import cats.data.Kleisli
import usafe.digital.verifiablecredentials.accounts.types.AccountId

trait AccountsClient[F[_], Client, Credentials] {

  def getAccount(accountId: AccountId): Kleisli[F, Client, Credentials]

}
