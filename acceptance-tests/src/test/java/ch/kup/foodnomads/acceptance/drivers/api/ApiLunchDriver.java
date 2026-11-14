package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class ApiLunchDriver implements LunchDriver {

    private final ApiClient api;

    ApiLunchDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public String plan(LocalDate date) {
        HttpResponse<String> response = api.post("api/lunches", planRequest(date));

        assertEquals(201, response.statusCode(), "POST /api/lunches: " + response.body());
        return ApiClient.readJson(response).path("id").asText();
    }

    @Override
    public void assertPlanRejectedAsInThePast(LocalDate date) {
        assertRefused(api.post("api/lunches", planRequest(date)), 400, "date-in-past");
    }

    @Override
    public void assertPlanRejectedAsAlreadyPlanned(LocalDate date) {
        assertRefused(api.post("api/lunches", planRequest(date)), 409, "lunch-already-planned");
    }

    @Override
    public void cancelPlannedLunch() {
        HttpResponse<String> response = api.get("api/lunches/planned");
        if (response.statusCode() == 204) {
            return;
        }
        assertEquals(200, response.statusCode(), "GET /api/lunches/planned: " + response.body());
        cancel(ApiClient.readJson(response).path("id").asText());
    }

    @Override
    public void assertPlanned(String lunchId, LocalDate date, String venue) {
        HttpResponse<String> response = api.get("api/lunches/planned");
        assertEquals(200, response.statusCode(), "GET /api/lunches/planned: " + response.body());
        JsonNode planned = ApiClient.readJson(response);

        assertEquals(lunchId, planned.path("id").asText(), "id of the Planned Lunch " + planned);
        assertEquals(date.toString(), planned.path("date").asText(), "date in " + planned);
        assertEquals("planned", planned.path("state").asText(), "state in " + planned);
        if (venue == null) {
            assertFalse(planned.hasNonNull("venue"), "venue in " + planned);
        } else {
            assertEquals(venue, planned.path("venue").path("name").asText(), "venue in " + planned);
        }
    }

    @Override
    public void cancel(String lunchId) {
        HttpResponse<String> response = api.post("api/lunches/" + lunchId + "/cancelled", ApiClient.newObject());

        assertEquals(204, response.statusCode(), "POST /api/lunches/{id}/cancelled: " + response.body());
    }

    @Override
    public void assertCancelled(String lunchId) {
        assertEquals("cancelled", lunch(lunchId).path("state").asText(), "state of Lunch " + lunchId);

        JsonNode listed = listed(lunchId);
        assertEquals("cancelled", listed.path("state").asText(), "state in " + listed);
    }

    @Override
    public void comment(String lunchId, String text) {
        HttpResponse<String> response = api.post("api/lunches/" + lunchId + "/comments", commentRequest(text));

        assertEquals(201, response.statusCode(), "POST /api/lunches/{id}/comments: " + response.body());
    }

    @Override
    public void assertCommentRejectedAsBlank(String lunchId, String text) {
        assertRefused(api.post("api/lunches/" + lunchId + "/comments", commentRequest(text)), 400, "text-required");
        assertTrue(lunch(lunchId).path("comments").isEmpty(), "comments of Lunch " + lunchId);
    }

    @Override
    public void assertComment(String lunchId, String author, String text) {
        JsonNode lunch = lunch(lunchId);
        JsonNode comment = StreamSupport.stream(lunch.path("comments").spliterator(), false)
                .filter(c -> c.path("author").asText().equals(author)
                        && c.path("text").asText().equals(text))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("No comment by " + author + " saying '" + text + "' in " + lunch));
        assertTrue(comment.hasNonNull("writtenAt"), "writtenAt in " + comment);
    }

    @Override
    public String recordPast(LocalDate date, String venue) {
        HttpResponse<String> response = api.post("api/lunches/past", recordPastRequest(date, venue));

        assertEquals(201, response.statusCode(), "POST /api/lunches/past: " + response.body());
        JsonNode lunch = ApiClient.readJson(response);
        assertEquals("held", lunch.path("state").asText(), "state in " + lunch);
        return lunch.path("id").asText();
    }

    @Override
    public void assertRecordPastRejectedAsVenueVisited(LocalDate date, String venue) {
        assertRefused(api.post("api/lunches/past", recordPastRequest(date, venue)), 409, "venue-visited");
    }

    @Override
    public void assertRecordPastRejectedAsDateNotPast(LocalDate date, String venue) {
        assertRefused(api.post("api/lunches/past", recordPastRequest(date, venue)), 400, "date-not-past");
    }

    @Override
    public void assertHeld(String lunchId, LocalDate date, String venue) {
        JsonNode lunch = lunch(lunchId);
        assertEquals(date.toString(), lunch.path("date").asText(), "date in " + lunch);
        assertEquals("held", lunch.path("state").asText(), "state in " + lunch);
        assertEquals(venue, lunch.path("venue").path("name").asText(), "venue in " + lunch);

        JsonNode listed = listed(lunchId);
        assertEquals("held", listed.path("state").asText(), "state in " + listed);
        assertEquals(venue, listed.path("venue").path("name").asText(), "venue in " + listed);
    }

    @Override
    public void addCandidate(String lunchId, String venue) {
        HttpResponse<String> response = api.post(candidatesPath(lunchId), candidateRequest(venue));

        assertEquals(201, response.statusCode(), "POST /api/lunches/{id}/candidates: " + response.body());
    }

    @Override
    public void assertAddCandidateRejectedAsVenueVisited(String lunchId, String venue) {
        assertRefused(api.post(candidatesPath(lunchId), candidateRequest(venue)), 409, "venue-visited");
        assertTrue(lunch(lunchId).path("candidates").isEmpty(), "candidates of Lunch " + lunchId);
    }

    @Override
    public void removeCandidate(String lunchId, String venue) {
        HttpResponse<String> response = api.delete(candidatesPath(lunchId) + "/" + venueId(venue));

        assertEquals(204, response.statusCode(), "DELETE /api/lunches/{id}/candidates/{venueId}: " + response.body());
    }

    @Override
    public void assertCandidates(String lunchId, Set<String> venues, Set<String> visited) {
        JsonNode candidates = lunch(lunchId).path("candidates");
        List<String> listed = StreamSupport.stream(candidates.spliterator(), false)
                .map(candidate -> candidate.path("venue").path("name").asText())
                .toList();
        Set<String> listedVisited = StreamSupport.stream(candidates.spliterator(), false)
                .filter(candidate -> candidate.path("visited").asBoolean())
                .map(candidate -> candidate.path("venue").path("name").asText())
                .collect(Collectors.toSet());

        assertEquals(venues.size(), listed.size(), "candidates in " + candidates);
        assertEquals(venues, Set.copyOf(listed), "candidates in " + candidates);
        assertEquals(visited, listedVisited, "visited candidates in " + candidates);
    }

    @Override
    public void vote(String lunchId, String venue, String direction) {
        HttpResponse<String> response =
                api.put(votePath(lunchId, venue), ApiClient.newObject().put("direction", direction));

        assertEquals(204, response.statusCode(), "PUT /api/lunches/{id}/candidates/{venueId}/vote: " + response.body());
        JsonNode candidate = candidate(lunchId, venue);
        assertEquals(direction, candidate.path("myVote").asText(), "myVote in " + candidate);
    }

    @Override
    public void withdrawVote(String lunchId, String venue) {
        HttpResponse<String> response = api.delete(votePath(lunchId, venue));

        assertEquals(
                204, response.statusCode(), "DELETE /api/lunches/{id}/candidates/{venueId}/vote: " + response.body());
        JsonNode candidate = candidate(lunchId, venue);
        assertTrue(candidate.has("myVote") && candidate.path("myVote").isNull(), "myVote in " + candidate);
    }

    @Override
    public void assertVotes(String lunchId, String venue, int up, int down) {
        JsonNode candidate = candidate(lunchId, venue);

        assertEquals(up, candidate.path("up").asInt(-1), "up in " + candidate);
        assertEquals(down, candidate.path("down").asInt(-1), "down in " + candidate);
    }

    @Override
    public void rsvp(String lunchId, String answer) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertRsvpRejectedAsNotPlanned(String lunchId, String answer) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertRsvps(String lunchId, Set<String> in, Set<String> out) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void move(String lunchId, LocalDate date) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertMoveRejectedAsInThePast(String lunchId, LocalDate date) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.get("api/lunches");

        assertEquals(401, response.statusCode(), "GET /api/lunches: " + response.body());
    }

    /** The Lunch with this id as {@code GET /api/lunches} lists it. */
    private JsonNode listed(String lunchId) {
        HttpResponse<String> response = api.get("api/lunches");
        assertEquals(200, response.statusCode(), "GET /api/lunches: " + response.body());
        return StreamSupport.stream(ApiClient.readJson(response).spliterator(), false)
                .filter(lunch -> lunch.path("id").asText().equals(lunchId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Lunch " + lunchId + " is not listed"));
    }

    /** The Candidate Venue with this name, as the Lunch's detail lists it to the logged-in Member. */
    private JsonNode candidate(String lunchId, String venue) {
        JsonNode candidates = lunch(lunchId).path("candidates");
        return StreamSupport.stream(candidates.spliterator(), false)
                .filter(candidate ->
                        candidate.path("venue").path("name").asText().equals(venue))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Candidate Venue '" + venue + "' in " + candidates));
    }

    private String votePath(String lunchId, String venue) {
        return candidatesPath(lunchId) + "/" + venueId(venue) + "/vote";
    }

    /** The id of the Venue with this name, from {@code GET /api/venues}. */
    private String venueId(String name) {
        HttpResponse<String> response = api.get("api/venues");
        assertEquals(200, response.statusCode(), "GET /api/venues: " + response.body());
        return StreamSupport.stream(ApiClient.readJson(response).spliterator(), false)
                .filter(venue -> venue.path("name").asText().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Venue '" + name + "' in " + response.body()))
                .path("id")
                .asText();
    }

    private JsonNode lunch(String lunchId) {
        HttpResponse<String> response = api.get("api/lunches/" + lunchId);
        assertEquals(200, response.statusCode(), "GET /api/lunches/{id}: " + response.body());
        return ApiClient.readJson(response);
    }

    private static void assertRefused(HttpResponse<String> response, int status, String error) {
        assertEquals(status, response.statusCode(), "Status: " + response.body());
        assertEquals(error, ApiClient.readJson(response).path("error").asText(), "error in " + response.body());
    }

    private static ObjectNode planRequest(LocalDate date) {
        return ApiClient.newObject().put("date", date.toString());
    }

    private ObjectNode recordPastRequest(LocalDate date, String venue) {
        return ApiClient.newObject().put("date", date.toString()).put("venueId", venueId(venue));
    }

    private ObjectNode candidateRequest(String venue) {
        return ApiClient.newObject().put("venueId", venueId(venue));
    }

    private static String candidatesPath(String lunchId) {
        return "api/lunches/" + lunchId + "/candidates";
    }

    private static ObjectNode commentRequest(String text) {
        return ApiClient.newObject().put("text", text);
    }
}
