package com.co.eurekatic.ssoadmin.controller;

import com.co.eurekatic.ssoadmin.dto.BindUserGroupRequest;
import com.co.eurekatic.ssoadmin.dto.GroupRequest;
import com.co.eurekatic.ssoadmin.dto.GroupResponse;
import com.co.eurekatic.ssoadmin.service.GroupAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Group CRUD + user↔group bindings. Phase 1 covers the user
 * bindings; app↔group bindings land in Phase 3 (they require
 * the {@code app} table, which doesn't exist yet). Path
 * layout mirrors the legacy
 * {@code com.co.lowcode.sso.controller.GroupController},
 * minus {@code /group/bindAppGroup}.
 */
@RestController
@RequestMapping("/group")
public class GroupController {

    private final GroupAdminService service;

    public GroupController(GroupAdminService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> save(@Valid @RequestBody GroupRequest req) {
        GroupResponse created = service.save(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<GroupResponse> getGroups() {
        return service.getGroups();
    }

    @PostMapping("/bindUserGroup")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bindUserGroup(@Valid @RequestBody BindUserGroupRequest req) {
        service.bindUserGroup(req.userId(), req.groupId());
    }
}
