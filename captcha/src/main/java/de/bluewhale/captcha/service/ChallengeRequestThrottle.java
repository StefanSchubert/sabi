/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Used to limit the throughput of requesting new challenges.
 * The idea is to stop DoS attack vectors by requesting challenges and therefore flooding the cache
 * by having jmeter jobs running against the captcha service..
 *
 * @author Stefan Schubert
 */
@Service
public class ChallengeRequestThrottle {
// ------------------------------ FIELDS ------------------------------

    static LocalDateTime lastRequest = LocalDateTime.now();

    static int requestCount;

    @Value("${challenge.throttle.per.minute}") // does not work for static, value is injected lazy through setter.
    static long MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE;

// -------------------------- STATIC METHODS --------------------------

    /**
     * By including this check you are able to throttle your API based upon {@link ChallengeRequestThrottle#MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE}
     * which will be set through the application property 'challenge.throttle.per.minute'.
     * @return true if request is within throughput limit, otherwise false.
     */
    public static boolean requestAllowed() {

        boolean allowed;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastMinute = now.minusSeconds(60);

        if (lastRequest.isBefore(lastMinute))  {
            resetAPICounter();
            allowed = true;
        } else {
            requestCount++;
            allowed = (requestCount > MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE ? false : true);
        }

        lastRequest = now;

        return allowed;
    }

    public static void setThrottleThreshold(int throttleThreshold) {
        MAX_CHALLENGE_REQUEST_THROUGHPUT_PER_MINUTE = throttleThreshold;
    }

    public static void resetAPICounter() {
        requestCount = 1;
    }
}
