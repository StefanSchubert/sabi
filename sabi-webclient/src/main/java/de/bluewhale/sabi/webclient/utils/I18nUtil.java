/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import de.bluewhale.sabi.model.SupportedLocales;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Common Methods supporting i18n handling that are not covered by spring et.al.
 *
 * @author Stefan Schubert
 */
@Named
public class I18nUtil {

    /**
     * Sabi has only a few languages for which translation exists - this ensures that
     *
     * @param language - An ISO 639 alpha-2 or alpha-3 language code, or a language subtag up to 8 characters in length. See the Locale class description about valid language values.
     * @return belonging requested Locale object or English as fallback.
     */
    public Locale getEnsuredSupportedLocale(String language) {
        Locale requestedLocale = new Locale(language);
        Locale fallBackLocale = Locale.ENGLISH;
        boolean fallBack = true;
        for (SupportedLocales sabiLocale : SupportedLocales.values()) {
            if (sabiLocale.getLocale().getLanguage().equals(requestedLocale.getLanguage())) {
                fallBack = false;
                break;
            }
        }
        return (fallBack ? fallBackLocale : requestedLocale);
    }


    /**
     * Uses to distinguish users decimal seperator, depending on provided locale, which will be the sessions locale settings
     * in most cases. However you may request other punctuation than sessions locale through the locale parameter.
     *
     * @param forLocale
     * @return character used as decimal Separator
     */
    public Character getDecimalSeparator(@NotNull Locale forLocale) {
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(forLocale);
        return decimalFormat.getDecimalFormatSymbols().getDecimalSeparator();
    }

    /**
     * Uses to distinguish users number grouping character, depending on provided locale, which will be the sessions locale settings
     * in most cases. You may request other punctuation than sessions locale would suggest. It's controlled by the parameter at last.
     *
     * @param forLocale
     * @return character used as decimal Separator
     */
    public Character getNumberGroupingSign(@NotNull Locale forLocale) {
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(forLocale);
        return decimalFormat.getDecimalFormatSymbols().getGroupingSeparator();
    }

}
