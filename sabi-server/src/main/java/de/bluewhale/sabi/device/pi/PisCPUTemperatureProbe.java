/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.device.pi;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Provides the cpu temperature of the pi
 * This is worthily to monitor
 * as if the pi exceeds its operation specification
 * of -40°C to +85°C the cpu will be throttled and
 * your services will stop from running smooth.
 * If so you need to consider adding a fan to your setup.
 *
 * @author Stefan Schubert
 */
@Slf4j
public class PisCPUTemperatureProbe implements MeterBinder {

    // result is required to be divided by 1000
    static final String CPU_Temp_CMD = "cat /sys/class/hwmon/hwmon0/temp1_input";

    @Override
    public void bindTo(MeterRegistry meterRegistry) {

        Gauge.builder("CPU_Temperature", this, value -> readCpuTemperature())
                .description("PIs CPU core temperature. Normal operation range is within -40°C to +85°C")
                .baseUnit("°C")
                .register(meterRegistry);

    }

    private double readCpuTemperature() {
        Double tempValue = 0d;
        String line = "N/A";
        try {
            Process process = Runtime.getRuntime().exec(CPU_Temp_CMD);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            int exitValue = process.waitFor(); // Wait until call has been finished.
            if (exitValue != 0) {
                log.warn("Abnormal process termination for: {}",CPU_Temp_CMD);
                log.warn("ExitValue was {} and processinfo: {}", exitValue, process.info());
                return tempValue;
            }

            while ((line = reader.readLine()) != null) {
                log.debug("PIs CPU Temperature readout: {} ",line);
                tempValue = Double.valueOf(line) / 1000;
            }
            reader.close();

        } catch (Exception e) {
            log.warn("Could not access or parse GPU Temperature from result line: {}", line);
            e.printStackTrace();
        }
        return tempValue;
    }

}
