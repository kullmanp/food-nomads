package ch.kup.foodnomads.members;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void exposesTheDisplayNameToTemplates() {
        assertThat(TestData.member("Anna").displayName()).isEqualTo("Anna");
    }
}
