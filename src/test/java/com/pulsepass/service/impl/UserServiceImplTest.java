package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    // ---------- helpers ----------

    private RegisterUserRequest buildRequest(LocalDate birthDate) {
        return new RegisterUserRequest("andrea", "andrea@email.com",
                "Andrea", "Perez", "3001234567", "Santa Marta", birthDate,
                "ClaveDePrueba2026");
    }

    private UserResponse buildResponse() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Perez", "3001234567", "Santa Marta", LocalDate.of(2000, 1, 1));
    }

    // ---------- TEST-USER-001 ----------
    @Test
    void register_validUser_savesActiveUserWithProfile() {
        // ARRANGE
        RegisterUserRequest request = buildRequest(LocalDate.of(2000, 1, 1));
        UserResponse response = buildResponse();
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        // ACT
        UserResponse result = userService.register(request);

        // ASSERT
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getProfile()).isNotNull();
        assertThat(saved.getProfile().getFirstName()).isEqualTo("Andrea");
        assertThat(result).isEqualTo(response);
    }

    // ---------- TEST-USER-002 ----------
    @Test
    void register_duplicateUsername_throwsDuplicateAndNeverSaves() {
        // ARRANGE
        RegisterUserRequest request = buildRequest(LocalDate.of(2000, 1, 1));
        when(userRepository.findByUsername("andrea"))
                .thenReturn(Optional.of(new User("andrea", "otro@email.com")));

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea");
        verify(userRepository, never()).save(any());
    }

    // ---------- TEST-USER-003 ----------
    @Test
    void register_duplicateEmail_throwsDuplicateAndNeverSaves() {
        // ARRANGE
        RegisterUserRequest request = buildRequest(LocalDate.of(2000, 1, 1));
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea@email.com");
        verify(userRepository, never()).save(any());
    }

    // ---------- TEST-USER-004 ----------
    @Test
    void register_futureBirthDate_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        RegisterUserRequest request = buildRequest(LocalDate.now().plusDays(1));
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);

        // ACT + ASSERT
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessRuleException.class);
        verify(userRepository, never()).save(any());
    }
}