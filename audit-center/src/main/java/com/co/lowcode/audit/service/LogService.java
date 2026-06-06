package com.co.lowcode.audit.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.co.lowcode.audit.model.Log;
import com.co.lowcode.audit.repository.LogRepository;

@Service
public class LogService {
	@Autowired
	LogRepository logRepository;

	public Log save(Log log){
		Log logCreated = logRepository.save(log);
		return logCreated;
	}
	
	public List<Log> getLog(Date startDate, Date endDate, String entidad) {
		return logRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualAndEntidad(startDate, endDate, entidad);
	}
	
	
}