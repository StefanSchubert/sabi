/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.configs;

import de.bluewhale.captcha.service.ChallengeRequestThrottle;
import de.bluewhale.captcha.service.ValidationCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;

/**
 * Little trick that enables us to inject properties into static util classes through
 * some sort of lazy initialization. The static class is created by the class loader,
 * the static fields will then be set from the outside in Spring ApplicationContext construction time.
 *
 * @author Stefan Schubert
 */

@Configuration
public class StaticFieldInjectionConfiguration {

    @Autowired
    Environment env;

    @PostConstruct
    private void lazyInit() {
        String property = env.getProperty("cachesize.cleanup.threshold");
        long threshold = Long.parseLong(property);

        property = env.getProperty("token.TTL");
        long ttl = Long.parseLong(property);

        property = env.getProperty("challenge.throttle.per.minute");
        int maxApiThroughputThreshold = Integer.parseInt(property);

        ValidationCache.setCleanupThreshold(threshold);
        ValidationCache.setTTL(ttl);
        ChallengeRequestThrottle.setThrottleThreshold(maxApiThroughputThreshold);
    }

}
