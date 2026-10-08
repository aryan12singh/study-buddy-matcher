package com.studybuddy.common;
import com.studybuddy.common.error.InvalidInputException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InputRulesTest {
    @Test void emailHasOneTrimmedCaseNormalizedIdentity() {
        assertEquals("jamie@example.test",InputRules.email(" Jamie@EXAMPLE.TEST "));
    }
    @Test void invalidEmailAndOversizedIdentityAreRejected() {
        assertThrows(InvalidInputException.class,() -> InputRules.email("missing-domain@"));
        assertThrows(InvalidInputException.class,() -> InputRules.email("a".repeat(250)+"@example.test"));
    }
    @Test void optionalBlankTextIsAbsentAndLimitsApplyToSavedText() {
        assertNull(InputRules.optional(" ","Message",255));
        assertEquals("abc",InputRules.optional(" abc ","Message",3));
        assertThrows(InvalidInputException.class,() -> InputRules.optional("abcd","Message",3));
    }
    @Test void bcryptPasswordLengthIsCheckedInUtf8Bytes() {
        assertEquals("a".repeat(72),InputRules.password("a".repeat(72)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("a".repeat(73)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("界".repeat(25)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("short"));
    }
}
