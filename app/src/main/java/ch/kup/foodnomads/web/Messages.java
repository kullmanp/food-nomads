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

    @Message
    String home_logged_in_as();

    @Message
    String join_title();

    @Message
    String join_intro();

    @Message
    String join_display_name();

    @Message
    String join_submit();

    @Message
    String join_failed();

    @Message
    String join_rejected();
}
