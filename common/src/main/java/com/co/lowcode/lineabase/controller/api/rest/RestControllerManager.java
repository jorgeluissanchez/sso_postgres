package com.co.lowcode.lineabase.controller.api.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

public abstract class RestControllerManager<T,T1> {
    
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(method = { org.springframework.web.bind.annotation.RequestMethod.GET }, value = {
			"/{id}" }, produces = { "application/json" })
	public abstract T doGet(@PathVariable T1 id);

	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(method = { org.springframework.web.bind.annotation.RequestMethod.GET }, produces = {
			"application/json" })
	public abstract List<T> doGet();

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(method = { org.springframework.web.bind.annotation.RequestMethod.POST }, produces = {
			"application/json" })
	public abstract T doPost(@RequestBody T ObjectT);

	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(method = { org.springframework.web.bind.annotation.RequestMethod.PUT }, value = {
			"/{id}" }, produces = { "application/json" })
	public abstract T doPut(@RequestBody T ObjectT, @PathVariable T1 id);

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(method = { org.springframework.web.bind.annotation.RequestMethod.DELETE }, value = {
			"/{id}" }, produces = { "application/json" })
	public abstract void doDelete(@PathVariable T1 id);
	
	
}