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
    String home_log_in();

    @Message
    String home_log_out();

    @Message
    String home_venues();

    @Message
    String login_title();

    @Message
    String login_intro();

    @Message
    String login_submit();

    @Message
    String login_failed();

    @Message
    String venues_title();

    @Message
    String venues_empty();

    @Message
    String venues_propose_title();

    @Message
    String venues_name();

    @Message
    String venues_address();

    @Message
    String venues_propose();

    @Message
    String venues_name_required();

    @Message
    String venues_candidates();

    @Message
    String venues_proposed_by();

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
