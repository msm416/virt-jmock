import example.classes.*;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;
import static utilities.distributions.PerfStatistics.hasPercentile;

import umontreal.ssj.probdist.NormalDist;

public class RepeatTest {
    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void repeatedlyLooksUpDetailsForEachFriend() {
        final SocialGraph socialGraph = context.mock(SocialGraph.class);
        final UserDetailsService userDetails = context.mock(UserDetailsService.class);

        context.repeat(1000, () -> {

            context.checking(new Expectations() {{
                exactly(1).of(socialGraph).query(USER_ID);
                will(returnValue(FRIEND_IDS));
                //inTime(new NormalDistr(1000, 10));
                inTime(new NormalDist(1000, 10));
                exactly(4).of(userDetails).lookup(with(any(Long.class)));
                will(returnValue(new User()));
                inTime(new NormalDist(100, 10));
                //inTime(new NormalDistr(100, 10));
            }});

            new ProfileController(socialGraph, userDetails).lookUpFriends(USER_ID);

        });

        assertThat(context.getMultipleVirtualTimes(), hasPercentile(80, lessThan(2000.0)));
    }
}