package com.james.taskflow.user;

import com.james.taskflow.common.PageResponse;
import com.james.taskflow.user.dto.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id")));
        return PageResponse.from(users.findAll(pageable).map(UserResponse::from));
    }
}