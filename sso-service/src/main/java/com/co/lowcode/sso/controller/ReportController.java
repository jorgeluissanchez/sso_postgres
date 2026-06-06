package com.co.lowcode.sso.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.co.lowcode.sso.service.ReportService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RestController
public class ReportController {

	
	@Autowired
	ReportService rs;

	@RequestMapping(value = "/report/createReport", method = RequestMethod.POST)
	public Map<String,Object> createReport(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.createReport(model);
	}
	
	
	@RequestMapping(value = "/report/updateReport", method = RequestMethod.PUT)
	public Map<String,Object> updateReport(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.updateReport(model);
	}
	
	@GetMapping("/report/getReports")
    public  List<Map<String,Object>> geReports(  HttpServletRequest request)  {
		return rs.getReports();
    }

		
	@GetMapping("/report/roles/checked")
    public  List<Map<String,Object>> getRolesReportChecked(@RequestParam String reportId,  HttpServletRequest request)  {
		return rs.getRolesReportChecked(reportId);
    }

	@GetMapping("/report/roles")
    public  List<Map<String,Object>> getRolesReport(@RequestParam String reportId,  HttpServletRequest request)  {
		return rs.getRolesReport(reportId);
    }
 
	
	@RequestMapping(value = "/report/addRole", method = RequestMethod.POST)
	public Map<String,Object> addRole(@RequestBody Map<String, Object> model) throws JSONException, TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException, MessagingException, SQLException {
		return rs.addRole(model);
	}
	
	
	@RequestMapping(value= "/report/removeRole", method = RequestMethod.DELETE)
	public void removeRole(@RequestParam String reportId,@RequestParam String roleId , HttpServletRequest request) throws SQLException {
		 
	  rs.removeRole(reportId, roleId);
	}
	


}
