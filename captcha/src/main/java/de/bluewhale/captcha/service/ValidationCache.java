package de.bluewhale.captcha.service;

/**
 * Maintainer of captcha tokens. The Main idea is that a token is valid only once and
 * has a short TTL. So we do not need to persist them. It is sufficient to keep them
 * in memory. Also we limit the size of parallel active codes to avoid DoS scenarios.
 *
 * @author Stefan Schubert
 */
public class ValidationCache {
    public static boolean knowsCode(final String pCaptchaChoice) {
        // TODO STS (21.06.16): Impl me
        throw new UnsupportedOperationException("boolean knowsCode([pCaptchaChoice])");
    }


    public static void invalidateCode(final String pCaptchaChoice) {
        // TODO STS (21.06.16): Impl Me
        throw new UnsupportedOperationException("void invalidateCode([pCaptchaChoice])");
    }
}
