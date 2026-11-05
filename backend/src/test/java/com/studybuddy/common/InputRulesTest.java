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
    @Test void textWithANulCharacterIsRejected() {
        var error = assertThrows(InvalidInputException.class,() -> InputRules.required("a\u0000b","Message",255));
        assertTrue(error.getFieldErrors().containsKey("message"));
    }
    @Test void bcryptPasswordLengthIsCheckedInUtf8Bytes() {
        assertEquals("a".repeat(72),InputRules.password("a".repeat(72)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("a".repeat(73)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("界".repeat(25)));
        assertThrows(InvalidInputException.class,() -> InputRules.password("short"));
    }
    @Test void localSingaporeContactsUseACanonicalCountryCode() {
        assertEquals("+6561234567", InputRules.contactNumber("6 (123) 45-67"));
        assertEquals("+6581234567", InputRules.contactNumber("8.123.4567"));
        assertEquals("+6591234567", InputRules.contactNumber("\t9 123 4567\n"));
    }
    @Test void singaporeCountryCodeNeedsAFullLocalNumber() {
        assertEquals("+6591234567", InputRules.contactNumber("+65 9123 4567"));
        assertEquals("+6561234567", InputRules.contactNumber("+65-6123-4567"));
    }
    @Test void yearOfStudyIsBetweenOneAndTheConfiguredMaximum() {
        assertDoesNotThrow(() -> InputRules.year(1, 6));
        assertDoesNotThrow(() -> InputRules.year(6, 6));
        for (Integer year : new Integer[] {null, 0, 7, 13}) {
            var error = assertThrows(InvalidInputException.class, () -> InputRules.year(year, 6), "Year: " + year);
            assertTrue(error.getFieldErrors().containsKey("yearOfStudy"));
        }
    }
    @Test void internationalContactsRemoveSeparatorsAndAcceptBothDigitBounds() {
        assertEquals("+12025550100", InputRules.contactNumber(" +1 (202) 555-0100 "));
        assertEquals("+1234567", InputRules.contactNumber("+1234567"));
        assertEquals("+123456789012345", InputRules.contactNumber("+123456789012345"));
        assertEquals("+123456789012345", InputRules.contactNumber(" ".repeat(239) + "+123456789012345"));
    }
    @Test void invalidContactShapesAndOversizedRawInputHaveAFieldError() {
        String[] invalid = {null, "", " \t ", "---()...", "12345678", "71234567", "9123456", "912345678",
            "+123456", "+1234567890123456", "+01234567", "65 9123 4567", "++6591234567",
            "659+1234567", "9123ABCD", "+12025550100 ext 2", "+12025550100x2", "+1/202/555/0100",
            "９１２３４５６７", " ".repeat(240) + "+123456789012345",
            "+65999999", "+65 9123 456", "+65 9123 45678", "+65 1234 5678"};
        for (String value : invalid) {
            var error = assertThrows(InvalidInputException.class, () -> InputRules.contactNumber(value), "Contact: " + value);
            assertTrue(error.getFieldErrors().containsKey("contactNumber"));
        }
    }
}
