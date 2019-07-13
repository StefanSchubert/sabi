/*
 * Copyright (c) 2019 by Stefan Schubert
 */

/**
 * We are using CDI Beans instead of JSF Beans.
 * Please read https://stackoverflow.com/questions/11986847/java-ee-6-javax-annotation-managedbean-vs-javax-inject-named-vs-javax-faces/12012663#12012663
 * for an introduction and history on the different bean scopes.
 *
 * By using the CDI @Named annotation the Beans comes accessible in JSF (in our project
 * through the cdi-api artifact) by using EL, see https://docs.oracle.com/javaee/6/tutorial/doc/gjddd.html
 * for details on that.
 *
 * @author Stefan Schubert
 */
package de.bluewhale.sabi.webclient.CDIBeans;