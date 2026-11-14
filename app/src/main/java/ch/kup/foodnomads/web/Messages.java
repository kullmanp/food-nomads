package ch.kup.foodnomads.web;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

/** User-facing texts, available in templates as {@code {msg:key}}. Texts live in {@code messages/msg.properties}. */
@MessageBundle
public interface Messages {

    @Message
    String app_name();

    @Message
    String nav_label();

    @Message
    String nav_home();

    @Message
    String nav_invite();

    @Message
    String nav_lunches();

    @Message
    String sheet_close();

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
    String home_invites();

    @Message
    String home_next_lunch();

    @Message
    String home_plan_lunch();

    @Message
    String home_lunch_over();

    @Message
    String home_lunch_over_held();

    @Message
    String home_lunch_over_cancel();

    @Message
    String home_lunch_over_pick_venue();

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
    String venues_all();

    @Message
    String venues_proposed_by();

    @Message
    String venues_visited();

    @Message
    String invites_title();

    @Message
    String invites_intro();

    @Message
    String invites_create();

    @Message
    String invites_link();

    @Message
    String invites_share();

    @Message
    String invites_copy();

    @Message
    String invites_copied();

    @Message
    String invites_open();

    @Message
    String invites_none();

    @Message
    String invites_created_by();

    @Message
    String invites_expires();

    @Message
    String invites_revoke();

    @Message
    String lunches_title();

    @Message
    String lunches_planned();

    @Message
    String lunches_plan_title();

    @Message
    String lunches_date();

    @Message
    String lunches_plan();

    @Message
    String lunches_date_required();

    @Message
    String lunches_date_in_past();

    @Message
    String lunches_already_planned();

    @Message
    String lunches_earlier();

    @Message
    String lunches_none();

    @Message
    String lunches_record_title();

    @Message
    String lunches_record_venue();

    @Message
    String lunches_record_choose_venue();

    @Message
    String lunches_record();

    @Message
    String lunches_record_date_not_past();

    @Message
    String lunches_record_venue_required();

    @Message
    String lunches_record_venue_visited();

    @Message
    String lunches_record_venue_picked();

    @Message
    String lunches_record_no_venues();

    @Message
    String lunch_title();

    @Message
    String lunch_state_planned();

    @Message
    String lunch_state_held();

    @Message
    String lunch_state_cancelled();

    @Message
    String lunch_no_venue();

    @Message
    String lunch_in_count(int count);

    @Message
    String lunch_move_title();

    @Message
    String lunch_move();

    @Message
    String lunch_rsvps();

    @Message
    String lunch_rsvp_in();

    @Message
    String lunch_rsvp_out();

    @Message
    String lunch_rsvps_in();

    @Message
    String lunch_rsvps_out();

    @Message
    String lunch_rsvps_nobody();

    @Message
    String lunch_cancel();

    @Message
    String lunch_cancel_confirm();

    @Message
    String lunch_clear_venue();

    @Message
    String lunch_hold();

    @Message
    String lunch_hold_confirm();

    @Message
    String lunch_hold_needs_venue();

    @Message
    String lunch_candidates();

    @Message
    String lunch_no_candidates();

    @Message
    String lunch_candidate_visited();

    @Message
    String lunch_candidate_remove();

    @Message
    String lunch_candidate_pick();

    @Message
    String lunch_candidate_picked();

    @Message
    String lunch_vote_up(int count);

    @Message
    String lunch_vote_down(int count);

    @Message
    String lunch_vote_up_withdraw(int count);

    @Message
    String lunch_vote_down_withdraw(int count);

    @Message
    String lunch_votes_up(int count);

    @Message
    String lunch_votes_down(int count);

    @Message
    String lunch_candidate_add_label();

    @Message
    String lunch_candidate_add();

    @Message
    String lunch_candidate_venue_visited();

    @Message
    String lunch_candidate_already();

    @Message
    String lunch_no_addable_venues();

    @Message
    String lunch_chat();

    @Message
    String lunch_no_comments();

    @Message
    String lunch_comment();

    @Message
    String lunch_comment_send();

    @Message
    String lunch_comment_required();

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

    @Message
    String join_name_taken();

    @Message
    String join_name_required();

    @Message
    String join_already_member();

    @Message
    String join_go_home();
}
