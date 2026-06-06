package com.co.lowcode.report.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.co.lowcode.backend.config.GeneralConfig;
import com.co.lowcode.backend.model.RecaptchaResponse;
import com.co.lowcode.backend.model.RequestQuery;

import oracle.jdbc.OracleTypes;

@PropertySource("classpath:general.properties")
@Service
public class QueryService {

	@PersistenceContext
	private EntityManager entityManager;

	@Value("${google.recaptcha.secret.key}")
	public String recaptchaSecret;
	@Value("${google.recaptcha.verify.url}")
	public String recaptchaVerifyUrl;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Autowired
	GeneralConfig generalConfig;

	@Autowired
	RestTemplate restTemplate;

	public boolean verifyCaptcha(String response) {
		MultiValueMap param = new LinkedMultiValueMap<>();
		param.add("secret", recaptchaSecret);
		param.add("response", response);

		RecaptchaResponse recaptchaResponse = null;
		try {
			recaptchaResponse = this.restTemplate.postForObject(recaptchaVerifyUrl, param, RecaptchaResponse.class);
		} catch (RestClientException e) {
			System.out.print(e.getMessage());
		}
		if (recaptchaResponse.isSuccess()) {
			return true;
		} else {
			return false;
		}
	}

	public List<Map<String, Object>> createTempTable(List<Map<String, Object>> jsonData) {

		ReturningWork<List<Map<String, Object>>> work = new ReturningWork<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> execute(Connection conn) throws SQLException {

				ResultSet rs = null;
				try {

					if (jsonData.isEmpty()) {
						System.out.println("No hay datos para insertar.");
						return null;
					}

					// Obtener las claves de la primera entrada para crear la tabla
					Map<String, Object> primerRegistro = jsonData.get(0);
					StringBuilder createTempTableQuery = new StringBuilder("CREATE TABLE #MiTablaTemporal (");

					for (String key : primerRegistro.keySet()) {
						createTempTableQuery.append(key).append(" NVARCHAR(MAX), ");
					}

					createTempTableQuery.setLength(createTempTableQuery.length() - 2); // Eliminar la última coma y
																						// espacio en blanco
					createTempTableQuery.append(")");

					conn.createStatement().executeUpdate(createTempTableQuery.toString());

					// Preparar la consulta de inserción
					String insertQuery = "INSERT INTO #MiTablaTemporal (";

					for (String key : primerRegistro.keySet()) {
						insertQuery += key + ", ";
					}

					insertQuery = insertQuery.substring(0, insertQuery.length() - 2); // Eliminar la última coma y
																						// espacio en blanco
					insertQuery += ") VALUES (";

					for (int i = 0; i < primerRegistro.keySet().size(); i++) {
						insertQuery += "?, ";
					}

					insertQuery = insertQuery.substring(0, insertQuery.length() - 2); // Eliminar la última coma y
																						// espacio en blanco
					insertQuery += ")";
					conn.setAutoCommit(false);
					PreparedStatement preparedStatement = conn.prepareStatement(insertQuery);

					// Iterar sobre los objetos del JSON y realizar las inserciones
					for (Map<String, Object> item : jsonData) {
						int index = 1;
						for (Object value : item.values()) {
							preparedStatement.setObject(index++, value);
						}

						preparedStatement.addBatch();
					}

					// Ejecutar las inserciones
					preparedStatement.executeBatch();
					conn.commit();
					System.out.println("Datos insertados en la tabla temporal correctamente.");

					// Realizar una consulta SELECT * en la tabla temporal
					String selectQuery = "SELECT * FROM #MiTablaTemporal";

					Query q = entityManager.createNativeQuery(selectQuery);

					org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

					hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
					return hibernateQuery.list();

					// Statement statement = conn.createStatement();
					// rs = statement.executeQuery(selectQuery);

					// Procesar los resultados de la consulta
					// while (rs.next()) {
					// Aquí puedes acceder a los datos de cada fila, por ejemplo:
					// String direccion = rs.getString("C_direccion");
					// System.out.println(direccion);
					// Realiza las operaciones necesarias con los datos
					// }

				} finally {

					String dropTableQuery = "DROP TABLE #MiTablaTemporal";
					conn.createStatement().executeUpdate(dropTableQuery);
					conn.commit();
					if (rs != null) {
						rs.close();
					}
				}

			}
		};

		Session session = (Session) entityManager.getDelegate();

		return session.doReturningWork(work);

	}

	public Map<String, Object> createProcedureInsertOrUpdate(String tabla, String schema) {

		ReturningWork<Map<String, Object>> work = new ReturningWork<Map<String, Object>>() {
			@Override
			public Map<String, Object> execute(Connection conn) throws SQLException {

				String procedimiento = "SP_INSERTAR_" + tabla + "_SP"; // Nombre del procedimiento almacenado
				ResultSet rs = null;
				try {
					// Obtener metadatos de la tabla
					DatabaseMetaData metadata = conn.getMetaData();
					rs = metadata.getColumns(null, schema, tabla, null);

					ResultSet primaryKeys = metadata.getPrimaryKeys(null, schema, tabla);
					String primaryKeyColumn = null;
					if (primaryKeys.next()) {
						primaryKeyColumn = primaryKeys.getString("COLUMN_NAME");
					}

					// Guardar los nombres de columna y tipos de datos en arrays de String
					String[] columnas = new String[100]; // Suponiendo que hay menos de 100 columnas
					String[] tipos = new String[100];
					int i = 0;

					while (rs.next()) {
						String columna = rs.getString("COLUMN_NAME");
						String tipo = rs.getString("TYPE_NAME");
						int tamaño = rs.getInt("COLUMN_SIZE");
						columnas[i] = columna;
						if (tipo.startsWith("int") || tipo.startsWith("bit") || tipo.startsWith("date")) {
							tipos[i] = tipo.replaceAll("identity", "");
						} else {
							tipos[i] = tipo.replaceAll("identity", "") + "(" + tamaño + ")";
						}

						i++;
					}

					// Construir la sentencia SQL
					StringBuilder sql = new StringBuilder();
					sql.append("CREATE PROCEDURE ").append(procedimiento).append(" (");

					for (int j = 0; j < i; j++) {
						sql.append("@").append(columnas[j]).append(" ").append(tipos[j]).append(",");
					}

					// Eliminar la última coma
					sql.deleteCharAt(sql.length() - 1);

					sql.append(") AS BEGIN IF  ").append(" (@");

					sql.append(primaryKeyColumn).append(" = 0");

					// Añadir los parámetros de entrada y la sentencia de inserción
					sql.append(") BEGIN INSERT INTO ").append(tabla).append(" (");

					for (int j = 0; j < i; j++) {
						if (!primaryKeyColumn.equals(columnas[j])) {
							sql.append(columnas[j]).append(",");
						}
					}

					// Eliminar la última coma
					sql.deleteCharAt(sql.length() - 1);

					sql.append(") VALUES (");

					for (int j = 0; j < i; j++) {
						if (!primaryKeyColumn.equals(columnas[j])) {
							sql.append("@").append(columnas[j]).append(",");
						}
					}

					// Eliminar la última coma
					sql.deleteCharAt(sql.length() - 1);

					sql.append(") ").append(" select SCOPE_IDENTITY() as id ").append("END");

					sql.append(" BEGIN UPDATE ").append(tabla).append(" SET ");

					for (int j = 0; j < i; j++) {

						if (!primaryKeyColumn.equals(columnas[j])) {
							sql.append(columnas[j]).append(" = @").append(columnas[j]).append(",");
						}

					}
					sql.deleteCharAt(sql.length() - 1);

					sql.append(" WHERE ").append(primaryKeyColumn).append(" = @").append(primaryKeyColumn);
					sql.append(" SELECT @").append(primaryKeyColumn).append(" AS id ");
					sql.append(" END ");
					sql.append(" END");

					System.out.println(sql.toString());

					// Ejecutar la sentencia SQL
					// Statement stmt = conn.createStatement();
					// stmt.execute(sql.toString());

				} finally {
					rs.close();
				}
				return null;
			}
		};

		Session session = (Session) entityManager.getDelegate();

		return session.doReturningWork(work);

	}

	public Map<String, Object> buildQuery(RequestQuery requestQuery, Integer publicEndpoint, String token)
			throws Exception {

		RequestQuery tmpRequestQuery = new RequestQuery();
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		tmpRequestQuery.setUsername(requestQuery.getUsername());
		tmpRequestQuery.setUuid(requestQuery.getUuid());

		Map<String, Object> tmpParams = new LinkedHashMap<String, Object>();

		Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();
		if (m != null) {
			tmpParams.putAll(m);
			tmpRequestQuery.setParams(tmpParams);
		}

		if (m != null && m.get("array") != null && m.get("array").getClass().equals(ArrayList.class)) {
			for (Map<String, Object> a : (List<Map<String, Object>>) m.get("array")) {
				tmpParams.putAll(a);
				result = query(tmpRequestQuery, publicEndpoint, token);
			}
		} else {
			return query(tmpRequestQuery, publicEndpoint, token);
		}
		return result;

	}

	public Map<String, Object> query(RequestQuery requestQuery, Integer publicEndpoint, String token) throws Exception {

		Map<String, Object> map = new HashMap<>();

		String uuid = requestQuery.getUuid();

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", token);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response_ = restTemplate.exchange(
				generalConfig.getBackenUrl() + "/getQuery?uuid=" + uuid, HttpMethod.GET, entity, String.class);

		String s = response_.getBody();
		try {
			
			if(publicEndpoint == 0 && s == null) {
				throw new RuntimeException("No autorizado");
			}
			
			JSONObject jsonQuery = new JSONObject(s);

			
			
			if (!publicEndpoint.equals(jsonQuery.get("PUBLIC_END"))) {
				throw new RuntimeException("No autorizado");
			}

			if (jsonQuery.get("CAPTCHA").equals(true)) {
				boolean captchaVerified = verifyCaptcha(requestQuery.getCaptcha());

				if (!captchaVerified) {
					throw new RuntimeException("Captcha Invalido");
				}
			}

			if (!jsonQuery.get("QUERY").equals(null)) {
				String query = jsonQuery.getString("QUERY");
				List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();

				if (query.startsWith("{call")) {
					Map<String, Object> ep = executeProcedure(query, requestQuery);
					map.putAll(ep);
				} else {
					res = getResultQuery(query, requestQuery);
					map.put("QUERY", res);
				}
				
				if (jsonQuery.has("DETAIL")) {
					JSONArray jArray = jsonQuery.getJSONArray("DETAIL");
					map.put("DETAIL", jArray.toList());
					JSONArray jArray2 = jsonQuery.getJSONArray("ACTION");
					map.put("ACTION", jArray2.toList());
					JSONArray jArray3 = jsonQuery.getJSONArray("STYLE");
					map.put("STYLE", jArray3.toList());
				}
			}

			String type = jsonQuery.getString("TYPE");
			if (type.equals("CHART")) {
				JSONArray charts = jsonQuery.getJSONArray("CHARTS");
				List<Map<String, Object>> chartsResponse = new ArrayList<>();
				for (Object c : charts) {
					Object queryChart = ((JSONObject) c).get("QUERY");
					List<Map<String, Object>> resChart = new ArrayList<Map<String, Object>>();
					if (queryChart != null) {
						if (queryChart.toString().startsWith("{call")) {
							Map<String, Object> ep = executeProcedure(queryChart.toString(), requestQuery);
							resChart = (List<Map<String, Object>>) ep.get("QUERY");
							map.put("message", ep.get("MESSAGE"));
						} else {
							resChart = getResultQuery(queryChart.toString(), requestQuery);
						}
					}
					Map<String, List> table = new HashMap();
					if (resChart.size() > 0) {
						for (Map<String, Object> r : resChart) {
							for (String key : r.keySet()) {
								if (table.get(key) != null) {
									table.get(key).add(r.get(key));
								} else {
									List list = new ArrayList<>();
									list.add(r.get(key));
									table.put(key, list);
								}
							}
						}
					}

					Map<String, Object> chart = new HashMap<String, Object>();
					JSONArray axes = ((JSONObject) c).getJSONArray("AXES");
					List<Map<String, Object>> yAxis = new ArrayList<>();
					List<Map<String, Object>> xAxis = new ArrayList<>();
					for (Object a : axes) {

						Map<String, Object> axesResponse = new HashMap<>();
						axesResponse.put("type", ((JSONObject) a).get("TYPE"));
						if (((JSONObject) a).get("NAMECOLUMN") != null
								&& table.containsKey(((JSONObject) a).get("NAMECOLUMN"))) {
							axesResponse.put("data", table.get(((JSONObject) a).get("NAMECOLUMN")));
						} else {
							if (!((JSONObject) a).get("QUERY").equals(null)) {
								String query = ((JSONObject) a).getString("QUERY");
								List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
								if (query.startsWith("{call")) {
									Map<String, Object> ep = executeProcedure(query, requestQuery);
									res = (List<Map<String, Object>>) ep.get("QUERY");
									map.put("message", ep.get("MESSAGE"));
								} else {
									res = getResultQuery(query, requestQuery);
								}
								List response = new ArrayList<>();
								for (Map<String, Object> r : res) {
									response.add(new ArrayList<Object>(r.values()).get(r.size() - 1));
								}
								axesResponse.put("data", response);
							}
						}
						if (((JSONObject) a).get("AXIS").equals("x")) {
							xAxis.add(axesResponse);
						} else {
							yAxis.add(axesResponse);
						}
					}
					if (!xAxis.isEmpty()) {
						chart.put("xAxis", xAxis);
					}
					if (!yAxis.isEmpty()) {
						chart.put("yAxis", yAxis);
					}

					JSONArray series = ((JSONObject) c).getJSONArray("SERIES");
					List<Map<String, Object>> seriesResponse = new ArrayList<Map<String, Object>>();
					for (Object a : series) {

						Map<String, Object> seriesMap = new HashMap<>();
						seriesMap.put("type", ((JSONObject) a).get("TYPE"));
						seriesMap.put("name", ((JSONObject) a).get("NAME"));
						if (table.containsKey(((JSONObject) a).get("NAMECOLUMN"))) {
							if ((((JSONObject) a).get("TYPE")).equals("pie")) {
								if (resChart.size() > 0) {
									List pie = new ArrayList<>();
									for (Map<String, Object> r : resChart) {
										Map<String, Object> m = new HashMap<>();
										m.put("name", r.get(((JSONObject) a).get("VALUECOLUMN")));
										m.put("value", r.get(((JSONObject) a).get("NAMECOLUMN")));
										pie.add(m);
									}
									seriesMap.put("data", pie);
								}
							} else {
								seriesMap.put("data", table.get(((JSONObject) a).get("NAMECOLUMN")));
							}

						} else {
							if (!((JSONObject) a).get("QUERY").equals(null)) {
								String query = ((JSONObject) a).getString("QUERY");
								List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
								if (query.startsWith("{call")) {
									Map<String, Object> ep = executeProcedure(query, requestQuery);
									res = (List<Map<String, Object>>) ep.get("QUERY");
									map.put("message", ep.get("MESSAGE"));
								} else {
									res = getResultQuery(query, requestQuery);
								}

								List response = new ArrayList<>();
								for (Map<String, Object> r : res) {
									if (((JSONObject) a).get("TYPE").equals("heatmap")) {
										response.add(new ArrayList<Object>(r.values()));
										continue;
									}
									if (r.size() > 1) {
										Object o = r.get(((JSONObject) a).get("NAMECOLUMN"));
										response.add(o);
									} else {
										response.add(new ArrayList<Object>(r.values()).get(0));
									}

								}
								seriesMap.put("data", response);
							}
						}

						if (!((JSONObject) a).get("SMOOTH").equals(null)) {
							seriesMap.put("smooth", ((JSONObject) a).get("SMOOTH"));
						}
						if (seriesMap.get("data") != null) {
							seriesResponse.add(seriesMap);
						}
					}

					chart.put("series", seriesResponse);
					chart.put("type", "CHART");
					chart.put("name", ((JSONObject) c).get("NAME"));
					chartsResponse.add(chart);

				}
				map.put("charts", chartsResponse);
			}
			map.put("type", type);
			return map;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public List<Map<String, Object>> getResultQuery(String query, RequestQuery requestQuery) {

		Query q = entityManager.createNativeQuery(query);
		q.getHints();

		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();

			Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();

			Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			if (m != null) {
				map.putAll(m);
			}
			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || clazz == BigDecimal.class
						|| clazz == Double.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else if (paramName.trim().startsWith("$")) {
				q.setParameter(paramName, requestQuery.getUsername());
			} else {
				//
				if (paramName.contains("FECHA")) {
					Date d = null;
					q.setParameter(paramName, d, TemporalType.DATE);
				} else {
					q.setParameter(paramName, null);
				}

			}
		}

		org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

		hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		List<Map<String, Object>> res = hibernateQuery.list();

		return res;
	}

	@Transactional
	public Map<String, Object> executeProcedure(String query, RequestQuery requestQuery) throws SQLException {
		ReturningWork<Map<String, Object>> work = new ReturningWork<Map<String, Object>>() {
			@Override
			public Map<String, Object> execute(Connection con) throws SQLException {

				CallableStatement call = con.prepareCall(query.replaceAll("@", "").replace("$", ""));

				try {

					String cursorName = "";
					String pmensaje = "";
					String cursorHeatMap = "";
					Pattern pattern = Pattern.compile(":(.*?),|:(.*?)\\)");
					Matcher matcher = pattern.matcher(query);
					LinkedList<String> params = new LinkedList<String>();
					Map<String, Object> mapResult = new HashMap<>();
					while (matcher.find()) {
						params.add(matcher.group().replaceAll(",", "").replaceAll("\\)", "").replaceAll(":", ""));
					}

					for (String paramName : params) {
						Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();

						Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
						if (m != null) {
							map.putAll(m);
						}

						if (map.get(paramName.trim()) != null) {
							Class clazz = map.get(paramName.trim()).getClass();
							if (clazz == String.class || clazz == Integer.class || clazz == Double.class
									|| clazz == BigDecimal.class || clazz == Long.class) {
								call.setObject(paramName.trim(), map.get(paramName.trim()));

							} else if (clazz == ArrayList.class) {
								ArrayList<Object> list = (ArrayList<Object>) map.get(paramName.trim());
								String sentence = "";
								for (int i = 0; i < list.size(); i++) {
									clazz = list.get(i).getClass();
									if (i == list.size() - 1) {
										if (clazz == String.class || clazz == Integer.class || clazz == Double.class
												|| clazz == BigDecimal.class || clazz == Long.class) {
											sentence += list.get(i).toString();
										} else {
											sentence += ((LinkedHashMap<String, Object>) list.get(i)).get("CODE")
													.toString();
										}
									} else {
										if (clazz == String.class || clazz == Integer.class || clazz == Double.class
												|| clazz == BigDecimal.class || clazz == Long.class) {
											sentence += list.get(i).toString() + ",";
										} else {
											sentence += ((LinkedHashMap<String, Object>) list.get(i)).get("CODE")
													.toString() + ",";
										}
									}
								}
								call.setString(paramName.trim(), sentence);
							} else {
								LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map
										.get(paramName.trim());
								call.setString(paramName.trim(), param.get("CODE").toString());
							}
						} else if (paramName.trim().startsWith("@")) {
							if (paramName.trim().equals("@p_codigo_error")) {
								call.registerOutParameter(paramName.substring(1), OracleTypes.NUMBER);
							} else if (paramName.trim().equals("@p_mensaje")) {
								pmensaje = paramName.trim().substring(1);
								call.registerOutParameter(paramName.substring(1), OracleTypes.VARCHAR);
							} else if (paramName.trim().equals("@p_colorimetria")) {
								cursorHeatMap = paramName.trim().substring(1);
								call.registerOutParameter(paramName.substring(1), OracleTypes.CURSOR);
							} else {
								cursorName = paramName.trim().substring(1);
								call.registerOutParameter(paramName.substring(1), OracleTypes.CURSOR);
							}
						} else if (paramName.trim().startsWith("$")) {
							call.setString(paramName.substring(1), requestQuery.getUsername());
						} else {
							call.setString(paramName.trim(), "");
						}
					}

					Boolean b = call.execute();

					if (cursorName != "") {
						ResultSet rs = (ResultSet) call.getObject(cursorName);

						List<Map<String, Object>> valList = new ArrayList<Map<String, Object>>();
						if (rs != null) {
							List<String> clmnList = null;
							ResultSetMetaData rsmd = rs.getMetaData();
							int countColumn = rsmd.getColumnCount();

							for (int k = 1; k <= countColumn; k++) {
								String name = rsmd.getColumnName(k);
								if (clmnList != null && clmnList.size() > 0) {
									if (clmnList.contains(name)) {
										name = rsmd.getTableName(k) + "." + name;
									}
									clmnList.add(name);
								} else {
									clmnList = new ArrayList<String>();
									clmnList.add(name);
								}
							}
							while (rs.next()) {
								Map<String, Object> valueMap = new HashMap<String, Object>();
								for (int i123 = 1; i123 <= countColumn; i123++) {
									valueMap.put(clmnList.get(i123 - 1), rs.getString(i123));
								}
								valList.add(valueMap);
							}
							mapResult.put("QUERY", valList);
							try {
								rs.close();
							} catch (Throwable e) {
							}
						}

					}

					if (pmensaje != "") {
						String mensaje = call.getString(pmensaje);
						mapResult.put("MESSAGE", mensaje);
					}

					if (cursorHeatMap != "") {
						ResultSet rsHeatMap = (ResultSet) call.getObject(cursorHeatMap);
						List<Map<String, Object>> listHeatMap = new ArrayList<Map<String, Object>>();
						if (rsHeatMap != null) {
							List<String> clmnList = null;
							ResultSetMetaData rsmd = rsHeatMap.getMetaData();
							int countColumn = rsmd.getColumnCount();

							for (int k = 1; k <= countColumn; k++) {
								String name = rsmd.getColumnName(k);
								if (clmnList != null && clmnList.size() > 0) {
									if (clmnList.contains(name)) {
										name = rsmd.getTableName(k) + "." + name;
									}
									clmnList.add(name);
								} else {
									clmnList = new ArrayList<String>();
									clmnList.add(name);
								}
							}
							while (rsHeatMap.next()) {
								Map<String, Object> valueMap = new HashMap<String, Object>();
								for (int i123 = 1; i123 <= countColumn; i123++) {
									valueMap.put(clmnList.get(i123 - 1), rsHeatMap.getString(i123));
								}
								listHeatMap.add(valueMap);
							}
						}
						mapResult.put("HEATMAP", listHeatMap);
						if (rsHeatMap != null) {
							try {
								rsHeatMap.close();
							} catch (Throwable e) {
							}
						}
					}

					return mapResult;
				} finally {
					call.close();
				}
			}
		};

		Session session = (Session) entityManager.getDelegate();

		return session.doReturningWork(work);
	}

	public Map<String, Object> queryChart(RequestQuery requestQuery) {
		Map<String, Object> map = new HashMap<>();
		String s = this.restTemplate
				.getForObject(generalConfig.getBackenUrl() + "/getQuery?uuid=" + requestQuery.getUuid(), String.class);
		try {
			JSONObject jsonQuery = new JSONObject(s);
			String query = jsonQuery.getString("QUERY");

			Query q = entityManager.createNativeQuery(query);
			q.getHints();

			Set<javax.persistence.Parameter<?>> params = q.getParameters();
			for (javax.persistence.Parameter<?> p : params) {
				String paramName = p.getName();

				if (((LinkedHashMap<String, Object>) requestQuery.getParams()).get(paramName)
						.getClass() == String.class) {
					q.setParameter(paramName,
							((LinkedHashMap<String, Object>) requestQuery.getParams()).get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) requestQuery
							.getParams()).get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			}
			org.hibernate.Query hibernateQuery = ((org.hibernate.jpa.HibernateQuery) q).getHibernateQuery();

			hibernateQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> res = hibernateQuery.list();
			map.put("QUERY", res);
			JSONArray jArray = jsonQuery.getJSONArray("DETAIL");
			map.put("DETAIL", jArray.toList());
			// return q.getResultList();

			return map;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Transactional
	public Map<String, Object> insert(RequestQuery requestQuery, String tableName) {

		Map<String, Object> mapEmail = new LinkedHashMap<String, Object>();
		Map<String, Object> paramsRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getParams();
		String s1 = "";
		String s2 = "";
		int i = 1;
		for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
			if (paramsRequestQuery.entrySet().size() == i) {
				s1 = s1 + entry.getKey();
				s2 = s2 + ":" + entry.getKey();
				break;
			}
			s1 = s1 + entry.getKey() + ",";
			s2 = s2 + ":" + entry.getKey() + ",";
			i++;
		}
		String query = String.format("INSERT INTO %s (%s)  VALUES (%s)", tableName, s1, s2);
		Query q = entityManager.createNativeQuery(query);
		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();

			Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();

			Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

			map.putAll(m);

			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || clazz == Date.class || clazz == Boolean.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else {
				q.setParameter(paramName, null);
			}
		}
		q.executeUpdate();

		return mapEmail;
	}

	@Transactional
	public Map<String, Object> update(RequestQuery requestQuery, String tableName) {

		Map<String, Object> mapEmail = new LinkedHashMap<String, Object>();
		Map<String, Object> paramsRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getParams();
		String s1 = "";
		String s2 = "";
		int i = 1;
		for (Map.Entry<String, Object> entry : paramsRequestQuery.entrySet()) {
			if (paramsRequestQuery.entrySet().size() == i) {
				s1 = s1 + entry.getKey() + "=:" + entry.getKey();
				break;
			}
			s1 = s1 + entry.getKey() + "=:" + entry.getKey() + ",";
			i++;
		}
		i = 1;
		Map<String, Object> whereRequestQuery = (LinkedHashMap<String, Object>) requestQuery.getWhere();
		for (Map.Entry<String, Object> entry : whereRequestQuery.entrySet()) {
			if (whereRequestQuery.entrySet().size() == i) {
				s2 = s2 + entry.getKey() + "=:" + entry.getKey();
				break;
			}
			s2 = s2 + entry.getKey() + "=:" + entry.getKey() + ",";
			i++;
		}

		String query = String.format("UPDATE  %s SET %s  WHERE %s", tableName, s1, s2);
		Query q = entityManager.createNativeQuery(query);
		Set<javax.persistence.Parameter<?>> params = q.getParameters();
		Map<String, Object> m = (LinkedHashMap<String, Object>) requestQuery.getParams();
		Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		map.putAll(m);

		Map<String, Object> mwhere = (LinkedHashMap<String, Object>) requestQuery.getWhere();
		Map<String, Object> map2 = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		map2.putAll(mwhere);

		for (javax.persistence.Parameter<?> p : params) {
			String paramName = p.getName();
			if (map.get(paramName) != null) {
				Class clazz = map.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class || clazz == Boolean.class
						|| clazz == BigDecimal.class) {
					q.setParameter(paramName, map.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else if (map2.get(paramName) != null) {
				Class clazz = map2.get(paramName).getClass();
				if (clazz == String.class || clazz == Integer.class) {
					q.setParameter(paramName, map2.get(paramName));
				} else {
					LinkedHashMap<String, Object> param = (LinkedHashMap<String, Object>) map2.get(paramName);
					q.setParameter(paramName, param.get("CODE"));
				}
			} else {
				q.setParameter(paramName, null);
			}
		}
		q.executeUpdate();
		return mapEmail;
	}

}
