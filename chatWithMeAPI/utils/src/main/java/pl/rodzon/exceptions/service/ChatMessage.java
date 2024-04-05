package pl.rodzon.exceptions.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class ChatMessage {
    private static ChatMessage singleton;
    private MessageSource source;

    public ChatMessage(MessageSource source) {
        this.source = source;
        if(singleton == null || singleton.source == null) {
            singleton = this;
        }
    }

    public static ChatMessage get() {
        return singleton;
    }

    public String get(String keyAndParamsSeparatedBySpaces) {
        String[] tab = keyAndParamsSeparatedBySpaces.split(" ");
        String key = tab[0];
        Object[] params = null;

        if(tab.length != 1) {
            params = Arrays.stream(tab).skip(1).toArray();
        }

        try {
            return this.source.getMessage(key, params, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return keyAndParamsSeparatedBySpaces;
        }
    }

    public String get(String key, Object... params) {
        try {
            return this.source.getMessage(key, params, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return key;
        }
    }
}