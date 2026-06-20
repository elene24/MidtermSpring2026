
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Thin JUnit adapter so the existing CharacterizationTest suite
 * runs through `mvn test` without any manual classpath setup.
 */
public class CharacterizationTestJUnit {

    @Test
    void allCharacterizationChecksPass() {
        CharacterizationTest.passed = 0;
        CharacterizationTest.failed = 0;
        CharacterizationTest.main(new String[0]);
        assertEquals(0, CharacterizationTest.failed,
                CharacterizationTest.passed + " passed / "
                        + CharacterizationTest.failed + " failed — see console output above");
    }
}