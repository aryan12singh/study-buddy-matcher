package com.studybuddy.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The same contact policy applies to every public write path without changing read permissions. */
class ContactHttpTest extends PostgresHttpTest {
    private static final String CREATED_PASSWORD = "contact-test-password";

    @Test
    void registrationRejectsTextAndReturnsTheCanonicalSingaporeContactOnlyToItsOwner() {
        var request = accountDetails("new@example.test", "New student", "9123ABCD");
        assertContactError(call("POST", "/auth/register", null, request));
        assertEquals(0, count("select count(*) from users"));

        request.put("contactNumber", "9 (123).45-67");
        JsonNode created = expect(call("POST", "/auth/register", null, request), 201);
        String token = created.path("token").asString();
        long id = created.path("account").path("id").asLong();
        assertPrivate(created);
        assertEquals("+6591234567", savedContact(id));
        assertEquals("+6591234567", expect(call("GET", "/profile/me", token, null), 200).path("contactNumber").asString());
    }

    @Test
    void administratorCreationValidatesInternationalContactAndWithholdsItInEveryAdminResponse() {
        account("admin@example.test", "ADMIN");
        String admin = login("admin@example.test");
        var request = accountDetails("created@example.test", "Created student", "+01234567");
        request.put("role", "STUDENT");
        assertContactError(call("POST", "/admin/users", admin, request));
        assertEquals(1, count("select count(*) from users"));

        request.put("contactNumber", "+1 (202) 555-0100");
        JsonNode created = expect(call("POST", "/admin/users", admin, request), 201);
        long id = created.path("account").path("id").asLong();
        assertPrivate(created);
        assertPrivate(expect(call("GET", "/admin/users/" + id, admin, null), 200));
        assertPrivate(expect(call("GET", "/admin/users", admin, null), 200));
        assertEquals("+12025550100", savedContact(id));
        String owner = expect(call("POST", "/auth/login", null,
            Map.of("email", "created@example.test", "password", CREATED_PASSWORD)), 200).path("token").asString();
        assertEquals("+12025550100", expect(call("GET", "/profile/me", owner, null), 200).path("contactNumber").asString());
    }

    @Test
    void ownProfileEditRejectsExtensionsWithoutMutationAndReadsBackNormalizedInternationalContact() {
        long id = student("owner@example.test", "Owner", "+12025550101");
        String owner = login("owner@example.test");
        var request = profileDetails("Changed name", "+12025550100 ext 2");
        assertContactError(call("PUT", "/profile/me", owner, request));
        assertEquals("+12025550101", savedContact(id));
        assertEquals("Owner", database.queryForObject("select name from students where id=?", String.class, id));

        request.put("contactNumber", "+44 (20) 7946.0958");
        assertEquals("+442079460958", expect(call("PUT", "/profile/me", owner, request), 200).path("contactNumber").asString());
        assertEquals("+442079460958", savedContact(id));
        assertEquals("+442079460958", expect(call("GET", "/profile/me", owner, null), 200).path("contactNumber").asString());
    }

    @Test
    void administratorEditPreservesOmittedContactAndValidatesPrivateCanonicalReplacements() {
        account("admin@example.test", "ADMIN");
        long id = student("owner@example.test", "Owner", "+12025550101");
        String admin = login("admin@example.test");
        String owner = login("owner@example.test");
        var request = accountDetails("owner@example.test", "Renamed owner", "+12025550101");
        request.remove("password");
        request.remove("contactNumber");
        assertPrivate(expect(call("PUT", "/admin/users/" + id, admin, request), 200));
        assertEquals("+12025550101", savedContact(id));
        request.put("contactNumber", null);
        assertPrivate(expect(call("PUT", "/admin/users/" + id, admin, request), 200));
        assertEquals("+12025550101", savedContact(id));

        request.put("name", "Invalid replacement");
        request.put("contactNumber", "659+1234567");
        assertContactError(call("PUT", "/admin/users/" + id, admin, request));
        assertEquals("Renamed owner", database.queryForObject("select name from students where id=?", String.class, id));
        assertEquals("+12025550101", savedContact(id));
        request.put("contactNumber", "6 (123) 45-67");
        assertPrivate(expect(call("PUT", "/admin/users/" + id, admin, request), 200));
        assertPrivate(expect(call("GET", "/admin/users/" + id, admin, null), 200));
        assertEquals("+6561234567", savedContact(id));
        assertEquals("+6561234567", expect(call("GET", "/profile/me", owner, null), 200).path("contactNumber").asString());
    }

    private Map<String, Object> accountDetails(String email, String name, String contact) {
        return new HashMap<>(Map.of("email", email, "password", CREATED_PASSWORD, "name", name,
            "school", "SCIS", "programme", "Information Systems", "yearOfStudy", 2, "contactNumber", contact));
    }

    private Map<String, Object> profileDetails(String name, String contact) {
        return new HashMap<>(Map.of("name", name, "school", "SCIS", "programme", "Information Systems", "yearOfStudy", 2,
            "contactNumber", contact, "courseIds", List.of(course), "preferredStudyMode", "ONLINE",
            "groupSizePreference", "EITHER", "studyGoals", List.of("CONCEPT_REVIEW")));
    }

    private String savedContact(long id) {
        return database.queryForObject("select contact_number from students where id=?", String.class, id);
    }

    private void assertContactError(Reply reply) {
        assertTrue(expect(reply, 400).path("fieldErrors").has("contactNumber"), reply.body());
    }

    private void assertPrivate(JsonNode reply) {
        assertFalse(reply.toString().contains("contactNumber"));
        assertFalse(reply.toString().contains("+120255501"));
        assertFalse(reply.toString().contains("+6561234567"));
        assertFalse(reply.toString().contains("+6591234567"));
    }
}
