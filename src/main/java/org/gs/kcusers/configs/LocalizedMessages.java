/*
 * Created by Eugene Sokolov 23.07.2024, 09:41.
 */

package org.gs.kcusers.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocalizedMessages {
    private static MessageSource messageSource;

    @Autowired
    public LocalizedMessages(MessageSource messageSource) {
        LocalizedMessages.messageSource = messageSource;
    }

    public static String getMessage(String key, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, locale);
    }

    public static String getMessage(String key) {
        return getMessage(key, null);
    }
}
