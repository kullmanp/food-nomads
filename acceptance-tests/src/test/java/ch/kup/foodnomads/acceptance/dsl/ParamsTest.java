package ch.kup.foodnomads.acceptance.dsl;

import static ch.kup.foodnomads.acceptance.dsl.Params.optional;
import static ch.kup.foodnomads.acceptance.dsl.Params.required;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ParamsTest {

    @Test
    void readsNamedValues() {
        Params params = Params.parse(
                new String[] {"name: Sternen", "address: Spalenberg 1, Basel"},
                required("name"), optional("address", ""));

        assertEquals("Sternen", params.value("name"));
        assertEquals("Spalenberg 1, Basel", params.value("address"));
    }

    @Test
    void optionalParamFallsBackToItsDefault() {
        Params params = Params.parse(new String[] {"name: Sternen"}, required("name"), optional("expect", "success"));

        assertEquals("success", params.value("expect"));
    }

    @Test
    void emptyValueIsKeptAndNotReplacedByTheDefault() {
        Params params = Params.parse(new String[] {"name: "}, optional("name", "Sternen"));

        assertEquals("", params.value("name"));
    }

    @Test
    void valueMayContainColons() {
        Params params = Params.parse(new String[] {"url: http://example.com:8080/"}, required("url"));

        assertEquals("http://example.com:8080/", params.value("url"));
    }

    @Test
    void missingRequiredParamIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Params.parse(new String[] {}, required("name")));
    }

    @Test
    void unknownParamIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Params.parse(new String[] {"nmae: Sternen"}, optional("name", "")));
    }

    @Test
    void duplicateParamIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Params.parse(new String[] {"name: a", "name: b"}, required("name")));
    }

    @Test
    void argWithoutColonIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Params.parse(new String[] {"Sternen"}, required("name")));
    }

    @Test
    void readingAnUndeclaredParamIsRejected() {
        Params params = Params.parse(new String[] {"name: Sternen"}, required("name"));

        assertThrows(IllegalArgumentException.class, () -> params.value("address"));
    }
}
