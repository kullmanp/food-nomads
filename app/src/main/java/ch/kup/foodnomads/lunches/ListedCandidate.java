package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.venues.Venue;

/**
 * A Candidate Venue as a Lunch lists it to one Member. {@code visited} is true once a Lunch has
 * been held there, which can happen after it was put up. {@code up} and {@code down} count the
 * Votes of all Members, without saying whose they are. {@code myVote} is the Member's own Vote, or
 * null for none.
 */
public record ListedCandidate(Venue venue, boolean visited, int up, int down, VoteDirection myVote) {}
