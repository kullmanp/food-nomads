-- Two people picking the same name at once both pass the check in Membership; this stops the second.
create unique index member_display_name_lower_key on member (lower(display_name));
