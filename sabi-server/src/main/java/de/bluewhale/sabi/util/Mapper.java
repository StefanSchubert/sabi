/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.util;

/**
 * Mapping Util Functions.
 * Since we have very few attributes per class and the targeted runtime environment of a pi at the beginning,
 * I decided to spare the libs for bean mappings like dozer or MapStruts, to gain a slim jar file.
 * Also To2Entity-Direction will ommit the primary key (for security reasons)
 *
 * @author Stefan Schubert
 */
public class Mapper {

  // Resolved by introduction of MapStruts. See de.bluewhale.sabi.mapper

}
