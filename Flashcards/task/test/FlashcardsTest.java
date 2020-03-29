import flashcards.MainKt;
import org.hyperskill.hstest.v6.stage.BaseStageTest;

public abstract class FlashcardsTest<T> extends BaseStageTest<T> {
    public FlashcardsTest() {
        super(MainKt.class);
    }
}
