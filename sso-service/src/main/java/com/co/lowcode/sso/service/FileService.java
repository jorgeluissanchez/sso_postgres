package com.co.lowcode.sso.service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.co.lowcode.sso.config.GeneralConfig;
import com.co.lowcode.sso.model.RequestQuery;
import com.co.lowcode.sso.util.Util;
import java.io.FileInputStream;
import java.security.MessageDigest;

@Service
public class FileService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private GeneralConfig config;

	@Autowired
	public QueryService queryService;

	@Autowired
	public UserService userService;

	private static String generateFileHash(String filePath) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(filePath)) {
			byte[] buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				md.update(buffer, 0, bytesRead);
			}
		}
		byte[] digest = md.digest();

		// Convertir el hash a una representación hexadecimal
		StringBuilder hexString = new StringBuilder();
		for (byte b : digest) {
			hexString.append(String.format("%02x", b));
		}

		return hexString.toString();
	}

	private static boolean verifyFileHash(String filePath, String referenceHash) throws Exception {
		String generatedHash = generateFileHash(filePath);
		return generatedHash.equals(referenceHash);
	}

	public Map<String, Object> getFilePublic(String uuid) {
		String query = "SELECT NAME, PATH FROM FILES WHERE UUID = :UUID";
		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			Map<String, Object> response = res.get(0);
			String path = response.get("PATH").toString();
			Map<String, Object> r = new HashMap<>();
			r.put("fileEncoded", Util.encodeFileToBase64(path));
			r.put("filename", response.get("NAME").toString());
			return r;
		}
		return null;
	}

	public Map<String, Object> getFileByUsername(String username, String uuid) {

		String query = "SELECT F.ID_FILE, F.NAME  FROM FILES F "
				+ "	INNER join ROLE_FILE RF ON F.id_file = RF.file_id "
				+ "	INNER join ROLE R ON R.id_role = RF.role_id "
				+ "	INNER join ROLE_USERS RU ON R.id_role = RU.role_id "
				+ "	INNER join USERS U ON U.id_user = RU.user_id " + "				 WHERE UUID = :UUID "

				+ "				 AND ( U.username = :USERNAME)" + " UNION  " + "SELECT F.ID_FILE, F.NAME FROM FILES F "
				+ "				 WHERE UUID = :UUID AND (USERNAME = :USERNAME OR USERNAME IS NULL)";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);
		q.setParameter("USERNAME", username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return res.get(0);
		}
		return null;
	}

	public List<Map<String, Object>> getFilesByUsername(String username) {

		String query = "SELECT F.ID_FILE, F.NAME, F.UUID  FROM FILES F "
				+ "	INNER join ROLE_FILE RF ON F.id_file = RF.file_id "
				+ "	INNER join ROLE R ON R.id_role = RF.role_id "
				+ "	INNER join ROLE_USERS RU ON R.id_role = RU.role_id "
				+ "	INNER join USERS U ON U.id_user = RU.user_id " + "				 WHERE  ( U.username = :USERNAME)"
				+ " UNION  " + "SELECT F.ID_FILE, F.NAME, F.UUID FROM FILES F "
				+ "				 WHERE  (USERNAME = :USERNAME OR USERNAME IS NULL)";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("USERNAME", username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			return res;
		}
		return null;
	}

	public Map<String, Object> getFile(String username, String uuid) {

		String query = "SELECT F.NAME, PATH, HASH  FROM FILES F " + "	INNER join ROLE_FILE RF ON F.id_file = RF.file_id "
				+ "	INNER join ROLE R ON R.id_role = RF.role_id "
				+ "	INNER join ROLE_USERS RU ON R.id_role = RU.role_id "
				+ "	INNER join USERS U ON U.id_user = RU.user_id " + "				 WHERE UUID = :UUID "

				+ "				 AND ( U.username = :USERNAME)" + " UNION  " + "SELECT F.NAME, PATH, HASH FROM FILES F "
				+ "				 WHERE UUID = :UUID AND (USERNAME = :USERNAME OR USERNAME IS NULL)";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);
		q.setParameter("USERNAME", username);
		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();
		if (res.size() > 0) {
			Map<String, Object> response = res.get(0);
			String path = response.get("PATH").toString();
			if(response.get("HASH") != null) {
				try {
					Boolean isOriginal = verifyFileHash(path, response.get("HASH").toString());
					if(isOriginal) {
						System.out.println("El archivo " + uuid + " es el original" );
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			
			
			Map<String, Object> r = new HashMap<>();
			r.put("fileEncoded", Util.encodeFileToBase64(path));
			r.put("filename", response.get("NAME").toString());
			return r;
		}
		return null;
	}

	public Map<String, Object> getFile(String uuid) {

		String query = "SELECT name, created_at, username  FROM FILES F " + " WHERE UUID = :UUID";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		if (res.size() > 0) {
			return res.get(0);
		}
		return null;
	}

	
	public Map<String, Object> getPathFile(String uuid) {

		String query = "SELECT name, PATH, created_at, username  FROM FILES F " + " WHERE UUID = :UUID";

		Query q = entityManager.createNativeQuery(query);
		q.setParameter("UUID", uuid);

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();
		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		if (res.size() > 0) {
			return res.get(0);
		}
		return null;
	}

	
	@Transactional
	public String saveFile(MultipartFile multipartFile, String username) throws IOException, SQLException {
		String uuid = UUID.randomUUID().toString().replace("-", "");
		String originalFileName = Util.limpiarAcentos(multipartFile.getOriginalFilename());
		String filename = uuid + originalFileName;
		String folder = config.getFilesPath();
		
		
		try {
			Util.convertMultiPartFiletoFile(multipartFile, folder, filename);
			RequestQuery rq = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("NAME", originalFileName);
			params.put("CREATED_AT", new Date());
			params.put("UUID", uuid);
			
			String path = folder != "" ? folder + "/" + filename : filename;
			String hash = generateFileHash(path);
			params.put("HASH", hash);
			params.put("PATH", path);
			params.put("USERNAME", username);
			rq.setParams(params);
			queryService.insert(rq, "FILES");
			return uuid;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
		
	}

	@Transactional
	public void bindFilebyRoleId(String uuid, String idRole, String username) throws SQLException {
		Map<String, Object> mapFile = getFileByUsername(username, uuid);
		if (mapFile != null) {
			RequestQuery rq = new RequestQuery();
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("ROLE_ID", idRole);
			params.put("FILE_ID", mapFile.get("ID_FILE"));
			rq.setParams(params);
			queryService.insert(rq, "ROLE_FILE");
		}
	}

	public void bindFilebyRoleName(String uuid, String nameRole, String username) throws SQLException {
		Map<String, Object> mapFile = getFileByUsername(username, uuid);
		if (mapFile != null) {

			Map<String, Object> role = userService.getRoleByName(nameRole);
			if (role != null) {
				RequestQuery rq = new RequestQuery();
				Map<String, Object> params = new LinkedHashMap<>();
				params.put("ROLE_ID", role.get("ID_ROLE").toString());
				params.put("FILE_ID", mapFile.get("ID_FILE").toString());
				rq.setParams(params);
				queryService.insert(rq, "ROLE_FILE");
			}
		}
	}

	public void bindFilebyRoles(String uuid, List<String> roles, String username) throws SQLException {
		Map<String, Object> mapFile = getFileByUsername(username, uuid);
		if (mapFile != null) {
			for (String r : roles) {
				Map<String, Object> role = userService.getRoleByName(r);
				if (role != null) {
					RequestQuery rq = new RequestQuery();
					Map<String, Object> params = new LinkedHashMap<>();
					params.put("ROLE_ID", role.get("ID_ROLE").toString());
					params.put("FILE_ID", mapFile.get("ID_FILE").toString());
					rq.setParams(params);
					queryService.insert(rq, "ROLE_FILE");
				}
			}

		}
	}

}
