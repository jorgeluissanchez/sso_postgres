package com.co.lowcode.lineabase.repository;

import org.springframework.data.repository.CrudRepository;

import com.co.lowcode.lineabase.model.Role;

public interface RoleRepository extends CrudRepository<Role, Long>  {
	Role findByName(String name);
}
