package afric.remit.bankapp.controller;

import afric.remit.bankapp.dto.LoginRequest;
import afric.remit.bankapp.dto.RegisterRequest;
import afric.remit.bankapp.service.UserService;
import afric.remit.bankapp.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_WhenValidRequest_ShouldReturnSuccess() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        doNothing().when(userService).register(any(RegisterRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void register_WhenUserServiceThrowsException_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        doThrow(new RuntimeException("Username already exists"))
            .when(userService).register(any(RegisterRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void login_WhenValidCredentials_ShouldReturnToken() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication mockAuth = mock(Authentication.class);
        String expectedToken = "dummy.jwt.token";

        when(userService.authenticate(any(LoginRequest.class))).thenReturn(mockAuth);
        when(jwtUtil.generateToken(mockAuth)).thenReturn(expectedToken);

        // Act & Assert
        String response = mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains(expectedToken);
        verify(userService, times(1)).authenticate(any(LoginRequest.class));
        verify(jwtUtil, times(1)).generateToken(mockAuth);
    }

    @Test
    void login_WhenInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(userService.authenticate(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).authenticate(any(LoginRequest.class));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_WhenEmptyRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();

        // Act & Assert
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).authenticate(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void register_WhenEmptyRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();

        // Act & Assert
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

    @Test
    void register_WhenInvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setEmail("invalid-email");

        // Act & Assert
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).register(any());
    }

        @Test
    void login_WhenServiceUnavailable_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userService.authenticate(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).authenticate(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void register_WhenUsernameAlreadyExists_ShouldReturnConflict() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setEmail("test@example.com");

        doThrow(new RuntimeException("Username already exists"))
            .when(userService).register(any(RegisterRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Username already exists")));

        verify(userService, times(1)).register(any());
    }
}