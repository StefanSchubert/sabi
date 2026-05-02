/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.model.WaterType;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestContainerVersions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that JPA optimistic locking via the {@code optlock} column works correctly
 * for all entities that extend {@link de.bluewhale.sabi.persistence.model.Auditable}.
 *
 * <h2>Root-cause of the original bug</h2>
 * {@code Auditable} used {@code org.springframework.data.annotation.@Version} instead of
 * {@code jakarta.persistence.@Version}. Spring Data's annotation has NO effect on JPA
 * entity state management — only the JPA annotation activates the optimistic locking
 * protocol. As a result, the {@code optlock} column was never incremented on updates.
 *
 * <h2>Behaviour note — "initial version"</h2>
 * Spring Data's {@code AuditingEntityListener} sets {@code @LastModifiedDate} during
 * both {@code @PrePersist} and {@code @PreUpdate}. After the initial INSERT, Hibernate
 * detects the field as dirty and issues an automatic UPDATE, bumping {@code optlock}
 * from 0 to 1 within the same flush cycle. This is expected behaviour.
 * The tests therefore use the value returned by the first {@code saveAndFlush} call as
 * their baseline and only assert on relative increments and the "stale-state throws"
 * behaviour.
 *
 * <p>Uses {@link AquariumEntity} as a representative of all {@code Auditable} subclasses
 * ({@code TankFishStockEntity}, {@code MeasurementEntity}, {@code FishEntity}, etc.).</p>
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@DirtiesContext
@Tag("IntegrationTest")
public class OptimisticLockingTest implements TestContainerVersions {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private AquariumRepository aquariumRepository;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------
    // Helper: transient AquariumEntity attached to Flyway seed user id=1
    // -------------------------------------------------------------------
    private AquariumEntity buildTestAquarium(String description) {
        UserEntity user = userRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Flyway seed user (id=1) not found — check V1_0_0_2__basicData.sql"));
        AquariumEntity tank = new AquariumEntity();
        tank.setDescription(description);
        tank.setSize(100);
        tank.setWaterType(WaterType.SEA_WATER);
        tank.setActive(true);
        tank.setUser(user);
        return tank;
    }

    // -------------------------------------------------------------------
    // Test 1: optlock must be a valid, non-negative value after persist.
    // -------------------------------------------------------------------
    @Test
    @Transactional
    public void optlock_isValidAfterInitialPersist() {
        AquariumEntity saved = aquariumRepository.saveAndFlush(buildTestAquarium("OptLock-New"));
        assertThat(saved.getOptlock())
                .as("optlock must be a valid non-negative counter after persist")
                .isGreaterThanOrEqualTo(0L);
    }

    // -------------------------------------------------------------------
    // Test 2: optlock must INCREMENT on each update (core contract).
    // -------------------------------------------------------------------
    @Test
    @Transactional
    public void optlock_incrementsOnEachUpdate() {
        AquariumEntity saved = aquariumRepository.saveAndFlush(buildTestAquarium("OptLock-Increment"));
        long baseline = saved.getOptlock();

        saved.setDescription("v1");
        AquariumEntity v1 = aquariumRepository.saveAndFlush(saved);
        assertThat(v1.getOptlock())
                .as("optlock must be baseline+1 after first update")
                .isEqualTo(baseline + 1);

        v1.setDescription("v2");
        AquariumEntity v2 = aquariumRepository.saveAndFlush(v1);
        assertThat(v2.getOptlock())
                .as("optlock must be baseline+2 after second update")
                .isEqualTo(baseline + 2);
    }

    // -------------------------------------------------------------------
    // Test 3: saving a stale detached entity MUST throw
    //         OptimisticLockingFailureException.
    //
    // Simulates two concurrent sessions:
    //   Session A reads entity  ==> version V
    //   Session B modifies + saves  ==> version V+1 (committed)
    //   Session A tries to save stale copy (still at V) ==> exception
    //
    // Propagation.NOT_SUPPORTED: no surrounding TX, each save auto-commits.
    // -------------------------------------------------------------------
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void optlock_throwsOnStaleModification() {
        AquariumEntity persisted = aquariumRepository.saveAndFlush(buildTestAquarium("OptLock-Concurrent"));
        Long entityId = persisted.getId();
        long baseline = persisted.getOptlock();

        AquariumEntity sessionACopy = aquariumRepository.findById(entityId).orElseThrow();
        assertThat(sessionACopy.getOptlock())
                .as("Session A must see version = baseline")
                .isEqualTo(baseline);

        AquariumEntity sessionBCopy = aquariumRepository.findById(entityId).orElseThrow();
        sessionBCopy.setDescription("Modified by Session B");
        AquariumEntity afterB = aquariumRepository.saveAndFlush(sessionBCopy);
        assertThat(afterB.getOptlock())
                .as("After session B update, DB version must be baseline+1")
                .isEqualTo(baseline + 1);

        sessionACopy.setDescription("Stale update by Session A — must fail!");
        assertThatThrownBy(() -> aquariumRepository.saveAndFlush(sessionACopy))
                .as("Saving a stale entity must throw OptimisticLockingFailureException")
                .isInstanceOf(OptimisticLockingFailureException.class);

        aquariumRepository.deleteById(entityId);
    }
}
