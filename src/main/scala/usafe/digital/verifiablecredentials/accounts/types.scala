package usafe.digital.verifiablecredentials.accounts

object types {

  final case class Email(value: String) extends AnyVal
  final case class Name(value: String) extends AnyVal
  final case class Address(value: String) extends AnyVal
  final case class Age(value: Int) extends AnyVal
  final case class AccountId(id: String) extends AnyVal

  final case class UserAccount(
    id: AccountId,
    email: Email,
    name: Name,
    address: Address,
    age: Age
  )

}
