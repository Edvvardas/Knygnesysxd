package lt.prifkodas.knygnesys.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lt.prifkodas.knygnesys.auth.dto.LoginRequest;
import lt.prifkodas.knygnesys.auth.dto.RegisterRequest;
import lt.prifkodas.knygnesys.user.User;
import lt.prifkodas.knygnesys.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession httpSession;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setEmail("test@test.lt");
        user.setUsername("testuser");
        user.setPassword("password");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@test.lt");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");
    }

    // register() tests
    @Test
    void register_success() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("test@test.lt", result.getEmail());
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Vartotojas su šiuo el. paštu jau egzistuoja", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_usernameAlreadyExists_throwsException() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Vartotojo vardas jau užimtas", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    // login() tests
    @Test
    void login_success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(request.getSession(true)).thenReturn(httpSession);
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));

        User result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_badCredentials_throwsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Neteisingas vartotojo vardas arba slaptažodis"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(loginRequest));

        verify(userRepository, never()).findByUsername(any());
    }

    // logout() tests
    @Test
    void logout_success() {
        when(request.getSession(false)).thenReturn(httpSession);

        authService.logout();

        verify(httpSession).invalidate();
    }
}