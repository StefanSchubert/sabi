/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.config;

import de.bluewhale.sabi.webclient.device.pi.PisCPUTemperatureProbe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Used for adding Custom Actuator metrics.
 * Here for including cpuTemperature values.
 *
 * @author Stefan Schubert
 */

@Lazy
@Component
public class ActuatorConfig {

//    @Autowired
//    MeterRegistry meterRegistry;

    @Bean
    PisCPUTemperatureProbe pisCPUTemperatureProbe() {
        return new PisCPUTemperatureProbe();
    }

}

