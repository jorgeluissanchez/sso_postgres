package com.co.lowcode.audit.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.co.lowcode.audit.model.Log;


public interface LogRepository extends CrudRepository<Log, Long>  {

	List<Log> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualAndEntidad(Date startDate, Date endDate, String entidad);
}
