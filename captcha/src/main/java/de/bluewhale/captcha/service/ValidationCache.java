/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Maintainer of captcha tokens. The Main idea is that a token is valid only once and
 * has a short TTL. So we do not need to persist them. It is sufficient to keep them
 * in memory. Also we limit the size of parallel active codes to avoid DoS scenarios.
 *
 * @author Stefan Schubert
 */
@Service
public class ValidationCache {
// ------------------------------ FIELDS ------------------------------

    @Value("${cachesize.cleanup.threshold}")
    public static long CLEANUP_THRESHOLD;

    static Map<String,Date> tokenCache = new HashMap<String, Date>(100);

    @Value("${token.TTL}")
    static long TTL; // = 45 * 1000; // 45 seconds (in millis)

// -------------------------- STATIC METHODS --------------------------

    /**
     * Possibility to set the TTL for junit tests.
     * @param pTTL Time to live in millisecs.
     */
    public static void setTTL(long pTTL) {
        ValidationCache.TTL = pTTL;
    }

    public static void invalidateCode(final String pCaptchaChoice) {
        if (knowsCode(pCaptchaChoice)) {
            tokenCache.remove(pCaptchaChoice);
        }
    }

    public static boolean knowsCode(final String pCaptchaChoice) {
        return tokenCache.containsKey(pCaptchaChoice);
    }

    public static long getTTL() {
        return ValidationCache.TTL;
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
        if (size > CLEANUP_THRESHOLD) {
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

    public static void setCleanupThreshold(long cleanupThreshold) {
        ValidationCache.CLEANUP_THRESHOLD = cleanupThreshold;
    }
}
