/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.device.pi.PisCPUTemperatureProbe;
import de.bluewhale.sabi.services.StatsMeter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Used for adding Custom Actuator metrics.
 *
 * @author Stefan Schubert
 */

@Lazy
@Component
public class ActuatorConfig {

/*
    @Autowired
    MeterRegistry meterRegistry;
*/

    @Bean
    PisCPUTemperatureProbe pisCPUTemperatureProbe() {
        return new PisCPUTemperatureProbe();
    }

    @Bean
    StatsMeter statsMeter() { return new StatsMeter(); }


}

