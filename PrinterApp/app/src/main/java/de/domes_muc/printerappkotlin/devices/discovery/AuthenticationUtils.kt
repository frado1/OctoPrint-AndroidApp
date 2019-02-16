package de.domes_muc.printerappkotlin.devices.discovery

import de.domes_muc.printerappkotlin.R
import android.content.Context
import android.util.Base64

import org.spongycastle.asn1.ASN1Integer
import org.spongycastle.asn1.ASN1Sequence

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPrivateKeySpec
import java.util.Enumeration

import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * Class to handle RSA signature to retrieve the valid API key from the server
 * Created by alberto-baeza on 11/21/14.
 */
object AuthenticationUtils {

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        NoSuchPaddingException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        InvalidKeySpecException::class,
        IOException::class
    )
    fun signStuff(context: Context, key: String): String? {
        var key = key

        val appid = "com.bq.octoprint.android"
        val version = "any"
        val unverified_key = key
        val message_to_sign = "$appid:$version:$unverified_key"

        //TODO open key from raw file
        val fis = context.resources.openRawResource(R.raw.key)


        val reader = BufferedReader(InputStreamReader(fis))
        val sb = StringBuilder()
        var line: String? = null
        while (reader.readLine()?.let {sb.append(it).append("\n")} != null);
        reader.close()

        key = sb.toString()

        //Clean the string
        val privKeyPEM = key.replace(
            "-----BEGIN RSA PRIVATE KEY-----\n", ""
        )
            .replace("-----END RSA PRIVATE KEY-----", "").replace("\n", "")

        // Base64 decode the data
        val encodedPrivateKey = Base64.decode(privKeyPEM, Base64.DEFAULT)

        var signed_key: String? = null

        try {

            //Format using ASN1
            val primitive = ASN1Sequence
                .fromByteArray(encodedPrivateKey) as ASN1Sequence
            val e = primitive.objects
            val v = (e.nextElement() as ASN1Integer).value

            val key_version = v.toInt()

            if (key_version != 0 && key_version != 1) {
                throw IllegalArgumentException("wrong version for RSA private key")
            }
            /**
             * In fact only modulus and private exponent are in use.
             * But we need the 3 elements to get a proper exponent.
             */
            val modulus = (e.nextElement() as ASN1Integer).value
            val publicExponent = (e.nextElement() as ASN1Integer).value
            val privateExponent = (e.nextElement() as ASN1Integer).value

            val spec = RSAPrivateKeySpec(modulus, privateExponent)
            val kf = KeyFactory.getInstance("RSA")
            val pk = kf.generatePrivate(spec)

            // Compute signature
            val instance = Signature.getInstance("SHA1withRSA")
            instance.initSign(pk)
            instance.update(message_to_sign.toByteArray())
            val signature = instance.sign()
            signed_key = Base64.encodeToString(signature, Base64.DEFAULT)


        } catch (e2: IOException) {
            throw IllegalStateException()
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException(e)
        } catch (e: InvalidKeySpecException) {
            throw IllegalStateException(e)
        }



        return signed_key

    }

}
