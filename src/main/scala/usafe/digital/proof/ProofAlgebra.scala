package usafe.digital.proof

trait ProofAlgebra[F[_], PrivateKey, PublicKey] {

  type PrivateKeyType = PrivateKey
  type PublicKeyType = PublicKey

  def hash(data: Array[Byte]): F[Array[Byte]]

  def sign(privateKey: PrivateKeyType, data: Array[Byte]): F[Array[Byte]]

  def verify(publicKey: PublicKeyType, data: Array[Byte], signatureToVerify: Array[Byte]): F[Boolean]

}