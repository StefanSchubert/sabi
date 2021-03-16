/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>A hazelcastInstance will be only created, if the libs are on the classpath (see maven dependencies) and
 * if a configuration will be found. So this configuration here triggers the cache creation at bootstrapping
 * (as long as you have @EnableCaching above of @SpringBootApplication).</p>
 * <p>
 * <p>As for further info about hazelcast java config see e.g.:
 * <a href="https://memorynotfound.com/spring-boot-hazelcast-caching-example-configuration/">spring-boot-hazelcast-caching-example-configuration</a>
 * </p>
 * <p>
 * <p>
 * <p>Within sabi this case is used for:
 * <ul>
 * <li>Password forgotten tokens</li>
 * </ul>
 * </p>
 *
 * @author Stefan Schubert
 */
@Configuration
public class HazelcastConfig {

    final static int TOKEN_TTL_10_MIN = 10 * 60;

    // todo refactore this to node configuration, as the instances within a cluster needs separate names.
    public final static String HZ_INSTANCE_NAME = "sabi-hzCache-Instance";

    @Bean
    public Config hazelCastConfig() {

        Config config = new Config()
                .setInstanceName(HZ_INSTANCE_NAME)
                .addMapConfig(
                        new MapConfig()
                                .setName("pwfToken")
                                .setTimeToLiveSeconds(TOKEN_TTL_10_MIN));
        // TODO STS (16.03.21): Elaborate MapConfig to suite possible DoS.

        // Cluster Communication via TCP
        NetworkConfig network = config.getNetworkConfig();
        network.setPort(5701).setPortCount(10); // Reserving a portrange of 10 for the backend nodes.
        network.setPortAutoIncrement(true);

        // Who is allowed to Join and disable multicast (as it is often blocked)
        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getTcpIpConfig()
                // .addMember("sabiBE1") // add your backend machines here (can be an IP address too).
                .addMember("localhost").setEnabled(true);

        return config;
    }
}
