package com.co.lowcode.sso.model;

import java.util.List;

import com.rethinkdb.converter.id;

public class Application {
	
	@id
	private String id;
	
	private List<Screen> screens;

}
