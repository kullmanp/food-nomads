package ch.kup.foodnomads.acceptance.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AliasesTest {

    @Test
    void sameAliasResolvesToTheSameRealNameWithinOneTest() {
        Aliases aliases = new Aliases();

        assertEquals(aliases.realName("Sternen"), aliases.realName("Sternen"));
    }

    @Test
    void realNameStartsWithTheAliasSoFailuresStayReadable() {
        assertTrue(new Aliases().realName("Sternen").startsWith("Sternen "));
    }

    @Test
    void differentTestsGetDifferentRealNamesForTheSameAlias() {
        assertNotEquals(new Aliases().realName("Sternen"), new Aliases().realName("Sternen"));
    }

    @Test
    void differentAliasesGetDifferentRealNames() {
        Aliases aliases = new Aliases();

        assertNotEquals(aliases.realName("Sternen"), aliases.realName("Krone"));
    }

    @Test
    void realNameMapsBackToItsAlias() {
        Aliases aliases = new Aliases();

        assertEquals("Sternen", aliases.aliasOf(aliases.realName("Sternen")));
    }

    @Test
    void unknownRealNameMapsToItself() {
        assertEquals("Krone", new Aliases().aliasOf("Krone"));
    }

    @Test
    void emptyAliasStaysEmptySoMissingValuesStayMissing() {
        assertEquals("", new Aliases().realName(""));
    }
}
