package com.co.lowcode.sso.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.co.lowcode.sso.service.FileService;
import com.co.lowcode.sso.util.Util;

@RestController
public class FileController {

 
    @Autowired
    FileService fs;
    

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody Object upload(@RequestParam("file") MultipartFile multipartFile, 
		 HttpServletRequest request) throws IOException, InstantiationException, SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
    	String uuid = fs.saveFile(multipartFile,username);
    	return uuid;
	}
    
    @GetMapping("/getFiles")
    public  Object getFiles(HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
        return fs.getFilesByUsername(username) ;
    }
    
    @GetMapping("/getFile")
    public  Object getFile(@RequestParam String uuid) throws JSONException {
        return fs.getFilePublic(uuid) ;
    }
    
    @GetMapping("/getFileByUuid")
    public  Object getFileByUuid(@RequestParam String uuid, HttpServletRequest request) throws JSONException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
        return fs.getFile(username, uuid);
    }
    
    @GetMapping("/getInfoFileByUuid")
    public  Object getInfoFileByUuid(@RequestParam String uuid, HttpServletRequest request) throws JSONException {
        return fs.getFile(uuid);
    }
    
    @GetMapping("/getPathFileByUuid")
    public  Object getPathFileByUuid(@RequestParam String uuid, HttpServletRequest request) throws JSONException {
        return fs.getPathFile(uuid);
    }
    
    
    @GetMapping("/bindFilebyRoleId")
    public  void bindFilebyRoleId(@RequestParam String uuid, @RequestParam String roleId, HttpServletRequest request) throws JSONException, SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
        fs.bindFilebyRoleId(uuid, roleId, username);
    }
    
    @GetMapping("/bindFilebyRoleName")
    public  void bindFilebyRoleName(@RequestParam String uuid, @RequestParam String roleName, HttpServletRequest request) throws JSONException, SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
        fs.bindFilebyRoleName(uuid, roleName, username);
    }
    
    @RequestMapping(value = "/bindFilebyRoles", method = RequestMethod.POST)
    public  void bindFilebyRoles(@RequestBody Map<String,Object> requestQuery, HttpServletRequest request) throws JSONException, SQLException {
    	String username = Util.getUserName(request.getHeader("authorization").substring(7));
    	@SuppressWarnings("unchecked")
		List<String> roles = (List<String>) requestQuery.get("roles");
    	
    	fs.bindFilebyRoles(requestQuery.get("uuid").toString(), roles, username);
    	
    }

}