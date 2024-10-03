/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("de.bluewhale.sabi.rest.controller")
@IncludeTags("ModuleTest")
public class ModuleTestSuite {
	// Diese Klasse bleibt leer. Sie dient nur als Halter für die obigen Annotationen
}
