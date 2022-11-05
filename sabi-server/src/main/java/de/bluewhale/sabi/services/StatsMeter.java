/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enables monitoring of some "business" metrics through prometheus/grafana
 * for being able to verify if some article postings or other events in the
 * outside world has an impact on service usage.
 *
 * @author Stefan Schubert
 */
public class StatsMeter implements MeterBinder {

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

    @Autowired
    MeasurementService measurementService;

    @Autowired
    PlagueCenterService plagueCenterService;

    @Override
    public void bindTo(MeterRegistry meterRegistry) {

        Gauge.builder("Participant_Count", this, value -> readParticipantCount())
                .description("Number of registered participants")
                .baseUnit("Number")
                .register(meterRegistry);

        Gauge.builder("Tank_Count", this, value -> readTankCount())
                .description("Number of registered tanks.")
                .baseUnit("Number")
                .register(meterRegistry);

        Gauge.builder("Overall_Measurement_Count", this, value -> readMeasurementCount())
                .description("Number of recorded measurements.")
                .baseUnit("Number")
                .register(meterRegistry);

        Gauge.builder("PlagueOR_Count", this, value -> readPlagueObservationRecordCount())
                .description("Number of recorded plague observations.")
                .baseUnit("Number")
                .register(meterRegistry);

    }

    private double readPlagueObservationRecordCount() {
        String amountOfPlagueRecords = plagueCenterService.fetchAmountOfPlagueRecords();
        return Double.valueOf(amountOfPlagueRecords);
    }

    private double readMeasurementCount() {
        String amountOfMeasurements = measurementService.fetchAmountOfMeasurements();
        return Double.valueOf(amountOfMeasurements);
    }

    private double readTankCount() {
        String amountOfTanks = tankService.fetchAmountOfTanks();
        return Double.valueOf(amountOfTanks);
    }

    private double readParticipantCount() {
        String amountOfParticipants = userService.fetchAmountOfParticipants();
        return Double.valueOf(amountOfParticipants);
    }

}
