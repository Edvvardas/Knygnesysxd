package lt.prifkodas.knygnesys.user.service;


import lombok.RequiredArgsConstructor;
import lt.prifkodas.knygnesys.shared.exception.ResourceNotFoundException;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vartotojas su ID " + id + " nerastas"));
    }

    public User create(User user) {
        return userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Vartotojas '" + username + "' nerastas"));
    }

    public List<User> getPublicProfiles() {
        return userRepository.findAllByIsPublicTrue();
    }
}