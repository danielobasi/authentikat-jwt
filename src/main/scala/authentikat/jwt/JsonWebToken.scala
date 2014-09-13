package authentikat.jwt

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.apache.commons.codec.binary.Base64.encodeBase64String
import org.apache.commons.codec.binary.Base64.decodeBase64

object JsonWebToken {
  import JsonWebSignature.HexToString._

  /**
   * Produces a JWT.
   * @param header
   * @param claims
   * @param key
   * @return
   */

  def apply(header: JwtHeader, claims: JwtClaimsSet, key: String): String = {
    val encodedHeader = encodeBase64String(header.asJsonString.getBytes("UTF-8"))
    val encodedClaims = encodeBase64String(claims.asJsonString.getBytes("UTF-8"))
    
    val signingInput = encodedHeader + "." + encodedClaims

    val encodedSignature: String = JsonWebSignature(header.algorithm.getOrElse("none"), signingInput, key)

    signingInput + "." + encodedSignature
  }

  /**
   * Extractor method
   * @param jwt
   * @return
   */

  def unapply(jwt: String): Option[(JwtHeader, JwtClaimsSetJValue, String)] = {
    val sections = jwt.split("\\.")

    sections.length match {
      case 3 =>
        import org.json4s.DefaultFormats
        implicit val formats = DefaultFormats

        val headerJsonString = new String(decodeBase64(sections(0)), "UTF-8")
        val header = JwtHeader.fromJsonString(headerJsonString)

        val claimsSet = JwtClaimsSetJValue(parse(new String(decodeBase64(sections(1)), "UTF-8")))

        val signature = sections(2)

        Some(header, claimsSet, signature)
      case _ =>
        None
    }
  }

  /**
   * Validate a JWT claims set against a secret key.
   * Validates an un-parsed jwt as parsing it before validating it is probably un-necessary.
   * Note this does NOT validate exp or other validation claims - it only validates the claims against the hash.
   * @param jwt
   * @param key
   * @return
   */

  def validate(jwt: String, key: String): Boolean = {

    import org.json4s.DefaultFormats
    implicit val formats = DefaultFormats

    val sections = jwt.split("\\.")

    val headerJsonString = new String(decodeBase64(sections(0)), "UTF-8")
    val header = JwtHeader.fromJsonString(headerJsonString)

    val signature = JsonWebSignature(header.algorithm.getOrElse("none"), sections(1), key)

    sections(2).contentEquals(signature)
  }

}
