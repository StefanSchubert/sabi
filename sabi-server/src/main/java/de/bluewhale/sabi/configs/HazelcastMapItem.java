/*
 * Copyright (c) 2019 by Stefan Schubert
 */

package de.bluewhale.sabi.configs;

/**
 * Summary of all Items we lay on the hazelcast cache, to avoid misspellings and to keep an
 * overview of use-cases, which utilize the hazelcast cache.
 *
 * @author Stefan Schubert
 */
public interface HazelcastMapItem {

    /**
     * Used in password forgotten workflow. Issued via email to the user requesting to reset his password.
     */
    String PASSWORD_FORGOTTEN_TOKEN = "pwfToken";


}
