package lt.prifkodas.knygnesys.user.service;

import lt.prifkodas.knygnesys.shared.exception.ResourceNotFoundException;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("test@test.lt");
        user.setUsername("testuser");
        user.setIsPublic(true);
    }

    // getById() tests
    @Test
    void getById_success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        User result = userService.getById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
    }

    @Test
    void getById_notFound_throwsException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.getById(99));

        assertEquals("Vartotojas su ID 99 nerastas", ex.getMessage());
    }

    // create() tests
    @Test
    void create_success() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = userService.create(user);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    // getByUsername() tests
    @Test
    void getByUsername_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.getByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.getByUsername("unknown"));

        assertEquals("Vartotojas 'unknown' nerastas", ex.getMessage());
    }

    // getPublicProfiles() tests
    @Test
    void getPublicProfiles_returnsOnlyPublicUsers() {
        User privateUser = new User();
        privateUser.setIsPublic(false);

        when(userRepository.findAllByIsPublicTrue()).thenReturn(List.of(user));

        List<User> result = userService.getPublicProfiles();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsPublic());
    }

    @Test
    void getPublicProfiles_empty_returnsEmptyList() {
        when(userRepository.findAllByIsPublicTrue()).thenReturn(List.of());

        List<User> result = userService.getPublicProfiles();

        assertTrue(result.isEmpty());
    }
}