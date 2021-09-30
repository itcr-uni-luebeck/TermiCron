package de.uzl.itcr.termicron.authentication

import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpHeaders
import java.io.IOException
import java.net.http.HttpRequest

/**
 * an abstract MdrAuthenticationDriver that makes authentication to the respective MDR transparent
 *
 * @property authenticationConfiguration the configuration of the MDR Authentication
 */
abstract class MdrAuthenticationDriver(
    val authenticationConfiguration: AuthenticationConfiguration
) {

    private val log = LogFactory.getLog(MdrAuthenticationDriver::class.java)

    /**
     * the actual credential
     */
    protected var mdrCredential: MdrCredential? = null

    /**
     * get the current credential, and log in/reauthenticate first if needed
     *
     * @return the valid credential
     */
    fun currentCredential(): MdrCredential {
        if (needLoginToMdr()) {
            loginToMdr()
            log.debug("Logged in to MDR")
        }
        return mdrCredential ?: throw IOException("missing mdr credential")
    }

    @Throws(IOException::class, InterruptedException::class)
    /**
     * log in to the MDR
     */
    abstract fun loginToMdr()

    /**
     * check if a login to the MDR is required, perhaps because the credential is expired
     *
     * @return True if login is needed
     */
    abstract fun needLoginToMdr(): Boolean
}

class NoOpAuthenticationDriver(noOpAuthenticationConfiguration: NoOpAuthenticationConfiguration) :
    MdrAuthenticationDriver(noOpAuthenticationConfiguration) {
    override fun loginToMdr() {
        return //no implementation needed
    }

    override fun needLoginToMdr(): Boolean = false //never required
}

fun HttpRequest.Builder.addAuthorizationHeader(authenticationDriver: MdrAuthenticationDriver): HttpRequest.Builder =
    this.apply {
        when (authenticationDriver) {
            is NoOpAuthenticationDriver -> return@apply //no header is added
            else -> this.header(
                HttpHeaders.AUTHORIZATION,
                authenticationDriver.currentCredential().encodeCredentialToAuthorizationHeader()
            )
        }
    }