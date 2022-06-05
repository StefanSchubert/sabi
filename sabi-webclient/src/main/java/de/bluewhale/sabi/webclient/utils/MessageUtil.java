/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Small helper class to set messages
 *
 * @author Stefan Schubert
 */
public class MessageUtil {

    /**
     * Accesses the i18n resource bundles and retrieves the localized string
     *
     * @param key    of the property
     * @param locale which shall be used
     * @return localized string
     */
    public static String getFromMessageProperties(String key, Locale locale) {
        ResourceBundle messages = ResourceBundle.getBundle("i18n.messages", locale);
        String message = messages.getString(key);
        return message;
    }

    /**
     * Adds an info Message to FacesContext.
     *
     * @param clientId if you have a specific element, can be null
     * @param msg      the message to be displayed
     */
    public static void info(@Null String clientId, @NotNull String msg) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_INFO, "", msg));
    }

    /**
     * Adds an info Message to FacesContext.
     *
     * @param clientId           if you have a specific element, can be null
     * @param messagePropertyKey key of the message to be displayed
     * @param locale             which shall be used
     */
    public static void info(@Null String clientId, @NotNull String messagePropertyKey, @NotNull Locale locale) {
        String localizedMessage = getFromMessageProperties(messagePropertyKey, locale);
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_INFO, "", localizedMessage));
    }

    /**
     * Adds a warn Message to FacesContext.
     *
     * @param clientId if you have a specific element, can be null
     * @param msg      the message to be displayed
     */
    public static void warn(@Null String clientId, @NotNull String msg) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", msg));
    }

    /**
     * Adds a warn Message to FacesContext.
     *
     * @param clientId           if you have a specific element, can be null
     * @param messagePropertyKey key of the message to be displayed
     * @param locale             which shall be used
     */
    public static void warn(@Null String clientId, @NotNull String messagePropertyKey, @NotNull Locale locale) {
        String localizedMessage = getFromMessageProperties(messagePropertyKey, locale);
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning!", localizedMessage));
    }

    /**
     * Adds an error Message to FacesContext.
     *
     * @param clientId if you have a specific element, can be null
     * @param msg      the message to be displayed
     */
    public static void error(@Null String clientId, @NotNull String msg) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", msg));
    }

    /**
     * Adds a error Message to FacesContext.
     *
     * @param clientId           if you have a specific element, can be null
     * @param messagePropertyKey key of the message to be displayed
     * @param locale             which shall be used
     */
    public static void error(@Null String clientId, @NotNull String messagePropertyKey, @NotNull Locale locale) {
        String localizedMessage = getFromMessageProperties(messagePropertyKey, locale);
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", localizedMessage));
    }

    /**
     * Adds an fatal Message to FacesContext.
     *
     * @param clientId if you have a specific element, can be null
     * @param msg      the message to be displayed
     */
    public static void fatal(@Null String clientId, @NotNull String msg) {
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Fatal!", msg));
    }

    /**
     * Adds a fatal Message to FacesContext.
     *
     * @param clientId           if you have a specific element, can be null
     * @param messagePropertyKey key of the message to be displayed
     * @param locale             which shall be used
     */
    public static void fatal(@Null String clientId, @NotNull String messagePropertyKey, @NotNull Locale locale) {
        String localizedMessage = getFromMessageProperties(messagePropertyKey, locale);
        FacesContext.getCurrentInstance().addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_FATAL, "Fatal!", localizedMessage));
    }

}
