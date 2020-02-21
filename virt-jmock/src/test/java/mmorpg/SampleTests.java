package mmorpg;

import example.classes.mmorpg.Character;
import example.classes.mmorpg.WorldMap;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class SampleTests {

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void testRunTrainingCallsTakeAStepTenTimes() {
        final WorldMap worldMap = context.mock(WorldMap.class);
        Character warrior = new Character();

        context.checking(new Expectations() {{
            exactly(10).of(worldMap).takeAStep(warrior);
        }});

        warrior.runTraining(worldMap);
    }
}