package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpResponse;
import java.time.LocalDate;
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

        HttpResponse<String> response = api.get("api/lunches");
        assertEquals(200, response.statusCode(), "GET /api/lunches: " + response.body());
        JsonNode listed = StreamSupport.stream(ApiClient.readJson(response).spliterator(), false)
                .filter(lunch -> lunch.path("id").asText().equals(lunchId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Lunch " + lunchId + " is not listed"));
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
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.get("api/lunches");

        assertEquals(401, response.statusCode(), "GET /api/lunches: " + response.body());
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

    private static ObjectNode commentRequest(String text) {
        return ApiClient.newObject().put("text", text);
    }
}
