package de.bluewhale.captcha.service;

import java.util.*;

/**
 * Maintainer of captcha tokens. The Main idea is that a token is valid only once and
 * has a short TTL. So we do not need to persist them. It is sufficient to keep them
 * in memory. Also we limit the size of parallel active codes to avoid DoS scenarios.
 *
 * @author Stefan Schubert
 */
public class ValidationCache {
// ------------------------------ FIELDS ------------------------------

    static Map<String,Date> tokenCache = new HashMap<String, Date>(100);


    static long TTL = 45 * 1000; // 45 seconds (in millis)

// -------------------------- STATIC METHODS --------------------------

    /**
     * Possibility to set the TTL for junit tests.
     * @param pTTL Time to live in millisecs.
     */
    public static void setTTL(long pTTL) {
        ValidationCache.TTL = pTTL;
    }

    public static boolean knowsCode(final String pCaptchaChoice) {
        // TODO STS (21.06.16): Impl me
        throw new UnsupportedOperationException("boolean knowsCode([pCaptchaChoice])");
    }

    public static void invalidateCode(final String pCaptchaChoice) {
        // TODO STS (21.06.16): Impl Me
        throw new UnsupportedOperationException("void invalidateCode([pCaptchaChoice])");
    }

    /**
     * Add the provided Token to the valid set.
     * However take into account, that each token has only a valid time to live.
     *
     * @param pToken
     */
    public static void registerToken(final String pToken) {
        cleanupTokenBase();
        Date date = new Date();
        synchronized (tokenCache) {
            tokenCache.put(pToken, date);
        }
    }

    // remove stale tokens from cache to avoid memory leaks
    private static void cleanupTokenBase() {
        int size = tokenCache.size();
        List<String> expiredTokens = new ArrayList<>();
        if (size > 100) {
            long now = new Date().getTime();
            synchronized (tokenCache) {
                for (Map.Entry<String, Date> entry : tokenCache.entrySet()) {
                    long tokenCreationTime = entry.getValue().getTime();
                    if ((now - tokenCreationTime) > TTL) {
                        expiredTokens.add(entry.getKey());
                    }
                }
                for (String expiredToken : expiredTokens) {
                    tokenCache.remove(expiredToken);
                }
            }
        }
    }
}
