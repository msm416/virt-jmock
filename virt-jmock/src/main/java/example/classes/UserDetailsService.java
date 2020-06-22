package example.classes;

public interface UserDetailsService {
    User lookup(Long userId);

    void analyseUserID(Long userId);
}