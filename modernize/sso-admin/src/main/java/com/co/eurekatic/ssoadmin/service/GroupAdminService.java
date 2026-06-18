package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Group;
import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.GroupRepository;
import com.co.eurekatic.common.repository.UserRepository;
import com.co.eurekatic.ssoadmin.dto.GroupRequest;
import com.co.eurekatic.ssoadmin.dto.GroupResponse;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Group CRUD and user↔group bindings. The legacy
 * {@code com.co.lowcode.sso.service.GroupService} also
 * supported app↔group bindings — those require the
 * {@code app} table which lands in Phase 3, so we defer the
 * {@code bindAppGroup} endpoint to Phase 3 as well.
 */
@Service
public class GroupAdminService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupAdminService(GroupRepository groupRepository,
                             UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    /**
     * Idempotent — if a group with the same name already
     * exists, returns the existing one. The legacy did the
     * same (raw SQL with "IF NOT EXISTS"-like semantics).
     */
    @Transactional
    public GroupResponse save(GroupRequest req) {
        Group group = groupRepository.findByName(req.name())
                .orElseGet(() -> {
                    Group g = new Group();
                    g.setName(req.name());
                    return g;
                });
        group.setDescription(req.description());
        return GroupResponse.fromEntity(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroups() {
        return groupRepository.findAll().stream()
                .map(GroupResponse::fromEntity)
                .toList();
    }

    /**
     * Binds a user to a group. Idempotent.
     */
    @Transactional
    public void bindUserGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group", groupId));
        group.addUser(user);
        groupRepository.save(group);
    }
}
