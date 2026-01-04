package uz.qarzdorlar_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uz.qarzdorlar_ai.enums.Role;
import uz.qarzdorlar_ai.exception.AccessDeniedException;
import uz.qarzdorlar_ai.exception.EntityNotFoundException;
import uz.qarzdorlar_ai.mapper.UserMapper;
import uz.qarzdorlar_ai.model.*;
import uz.qarzdorlar_ai.payload.PageDTO;
import uz.qarzdorlar_ai.payload.ProfileUpdateDTO;
import uz.qarzdorlar_ai.payload.UserFilterDTO;
import uz.qarzdorlar_ai.payload.user.UserDTO;
import uz.qarzdorlar_ai.repository.UserRepository;
import uz.qarzdorlar_ai.specification.UserSpecification;

import java.util.EnumSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private static final EnumSet<Role> ALLOWED_ROLES = EnumSet.of(Role.ADMIN, Role.DEVELOPER, Role.STAFF_PLUS);


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return getUser(username);
    }


    @Override
    public UserDTO getMe(User user) {
        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username: " + user.getUsername())
                );

        return userMapper.toDTO(currentUser);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new EntityNotFoundException("User not found with username: " + username);
                });
    }

    @Override
    public UserDTO updateDTO(User user, ProfileUpdateDTO dto) {
        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username: " + user.getUsername())
                );

        UserProfile profile = currentUser.getUserProfile();

        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        profile.setPhoneNumber(dto.getPhoneNumber());
        profile.setEmail(dto.getEmail());
        profile.setEmailEnabled(false);

        user.setUserProfile(profile);
        userRepository.save(user);

        return userMapper.toDTO(user);
    }

    @Override
    public PageDTO<UserDTO> getAllUser(Integer page, Integer size) {
        Sort sort = Sort.by(User.Fields.username);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<User> users = userRepository.findAll(pageRequest);

        return new PageDTO<>(
                users.getContent().stream().map(userMapper::toDTO).toList(),
                users
        );
    }

    @Override
    public void deleteUser(Long id, User user) {

        User currentUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found with username: " + user.getUsername())
                );

        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        if (!currentUser.getId().equals(userToDelete.getId()) // agar o‘zini o‘chirayotgan bo‘lmasa
                && !ALLOWED_ROLES.contains(currentUser.getRole())) { // allowed role bo‘lmasa
            throw new AccessDeniedException("You are not allowed to delete other users");
        }

        userRepository.delete(userToDelete);
    }

    @Override
    public PageDTO<UserDTO> getSearch(UserFilterDTO userFilterDTO, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(
                page != null ? page : 0,
                size != null ? size : 10,
                Sort.by(User.Fields.username).ascending()
        );

        Page<User> userPage = userRepository.findAll(UserSpecification.filter(userFilterDTO), pageable);

        List<UserDTO> content = userPage.getContent().stream()
                .map(userMapper::toDTO)
                .toList();

        return new PageDTO<>(
                content,
                userPage
        );
    }
}
