package ch.kup.foodnomads.web;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

/** User-facing texts, available in templates as {@code {msg:key}}. Texts live in {@code messages/msg.properties}. */
@MessageBundle
public interface Messages {

    @Message
    String app_name();

    @Message
    String home_placeholder();
}
