import example.classes.*;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertThat;

import umontreal.ssj.probdist.NormalDist;

public class BasicTest {
    static final long USER_ID = 1111L;
    static final List<Long> FRIEND_IDS = Arrays.asList(2222L, 3333L, 4444L, 5555L);

    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Test
    public void looksUpDetailsForEachFriend() {
        final SocialGraph socialGraph = context.mock(SocialGraph.class);
        final UserDetailsService userDetails = context.mock(UserDetailsService.class);

        context.checking(new Expectations() {{
            exactly(1).of(socialGraph).query(USER_ID);
            will(returnValue(FRIEND_IDS));
            inTime(new NormalDist(1000, 10));
            exactly(4).of(userDetails).lookup(with(any(Long.class)));
            will(returnValue(new User()));
            inTime(new NormalDist(100, 10));
        }});

        long startTime = System.currentTimeMillis();
        new ProfileController(socialGraph, userDetails).lookUpFriends(USER_ID);
        long endTime = System.currentTimeMillis();

        assertThat(context.getSingleVirtualTime(true)
                        + (endTime - startTime)
                        - context.getSingleRealTime(),
                lessThan(2000.0 + 2000));


    }
}