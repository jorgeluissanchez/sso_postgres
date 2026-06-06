package com.co.lowcode.lineabase.repository;

import org.springframework.data.repository.CrudRepository;

import com.co.lowcode.lineabase.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

	User findByUsername(String login);
	User findByTokenActivation(String token);
	User findByTokenRestore(String token);
	User findByApiToken(String apiToken);
}