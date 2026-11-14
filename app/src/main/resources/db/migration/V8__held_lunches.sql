-- Where a Lunch took place. A Held Lunch has one; a Planned Lunch gets one when a Venue is picked.
-- A Venue is visited when a Held Lunch has it, so there's no visited column on venue.
alter table lunch add column venue_id uuid references venue (id);

create index lunch_venue_id on lunch (venue_id);
