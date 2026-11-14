package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.venues.Venue;

/**
 * A Candidate Venue as a Lunch lists it. {@code visited} is true once a Lunch has been held there,
 * which can happen after it was put up.
 */
public record ListedCandidate(Venue venue, boolean visited) {}
