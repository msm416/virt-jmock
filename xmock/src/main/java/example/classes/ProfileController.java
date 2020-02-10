package example.classes;

import java.util.ArrayList;
import java.util.List;

public class ProfileController {
    private final SocialGraph socialGraph;
    private final UserDetailsService userDetailsService;

    public ProfileController(SocialGraph socialGraph, UserDetailsService userDetailsService) {
        this.socialGraph = socialGraph;
        this.userDetailsService = userDetailsService;
    }

    public void lookUpFriends(long userId) {

//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        List<Long> friendIds = socialGraph.query(userId);
        List<User> friends = new ArrayList<>();

        for (Long friendId : friendIds) {
            friends.add(userDetailsService.lookup(friendId));
        }
    }
}