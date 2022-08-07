/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.device.pi;

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
 * yor services will stop from running smooth.
 * If so so need to consider adding a fan to your setup.
 *
 * @author Stefan Schubert
 */
@Slf4j
public class PisCPUTemperatureProbe implements MeterBinder {

    static final String GPU_Temp_CMD = "/usr/bin/vcgencmd measure_temp";
    static final String GPU_RESULT_REGEXP = "[=']";

    // result is required to be devided by 1000
    static final String CPU_Temp_CMD = "cat /sys/class/thermal/thermal_zone0/temp";

    @Override
    public void bindTo(MeterRegistry meterRegistry) {

        Gauge.builder("CPU_Temperature", this, value -> readCpuTemperature())
                .description("PIs CPU core temperature. Normal operation range is within -40°C to +85°C")
                .baseUnit("°C")
                .register(meterRegistry);

        Gauge.builder("GPU_Temperature", this, value -> readGpuTemperature())
                .description("PIs GPU temperature. Normal operation range is within -40°C to +85°C")
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


    /**
     * NOTICE: Requires that the running user is part of the video group. Good-Practise: Ensure this via ansible script.
     * @return GPU Temperature from your pi - assuming your pis location settings point to metric area with Celsius
     */
    private double readGpuTemperature() {
        Double tempValue = 0d;
        String line = "N/A";
        try {
            Process process = Runtime.getRuntime().exec(GPU_Temp_CMD);
            int exitValue = process.waitFor(); // Wait until call has been finished.
            if (exitValue != 0) {
                log.warn("Abnormal process termination for: {}",GPU_Temp_CMD);
                log.warn("ExitValue was {} and processinfo: {}", exitValue, process.info());
                return tempValue;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            while ((line = reader.readLine()) != null) {
                log.debug("PIs GPU-Temperature readout: {}", line);
                String[] token = line.split(GPU_RESULT_REGEXP);
                tempValue = Double.valueOf(token[1]);
            }
            reader.close();

        } catch (Exception e) {
            log.warn("Could not access or parse GPU Temperature from result line: {}", line);
            e.printStackTrace();
        }
        return tempValue;
    }
}

