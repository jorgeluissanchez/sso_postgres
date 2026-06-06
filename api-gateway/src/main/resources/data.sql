ALTER TABLE report ALTER COLUMN uuid SET DEFAULT gen_random_uuid();

-- Instrucciones de inserción para la tabla app:
INSERT INTO app (id_app, createdat, description, name, owner, rootscreen, updateat, uri) VALUES ('1', NULL, 'SSO', 'SSO', NULL, '0', NULL, NULL);

ALTER SEQUENCE app_id_app_seq RESTART WITH 2;

-- Instrucciones de inserción para la tabla role:
INSERT INTO role (id_role, name) VALUES ('1', 'SSO_ADMIN');
ALTER SEQUENCE role_id_role_seq RESTART WITH 2;



-- Instrucciones de inserción para la tabla route:
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('2', NULL, '1', '1', 'Usuarios', '/sso/users', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('3', NULL, '1', '2', 'Roles', '/sso/roles', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('4', NULL, '1', '3', 'Rutas', '/sso/routes', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('5', NULL, '1', '4', 'Microservicios', '/sso/microservices', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('6', NULL, '1', '5', 'Endpoints', '/sso/endpoints', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('7', NULL, '1', '6', 'Consultas Dinamicas', '/sso/reports', NULL);
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('8', '', '1', '7', 'Aplicaciones', '/sso/applications', '');
INSERT INTO route (id_route, icon, idparent, menuorder, name, path, type) VALUES ('1', NULL, NULL, '1', 'SSO', '/', NULL);
ALTER SEQUENCE route_id_route_seq RESTART WITH 9;

-- Instrucciones de inserción para la tabla microservice:
INSERT INTO microservice (id_microservice, createddate, description, requesturi, targeturipath, targeturlhost, targeturlport, serviceid) VALUES ('1', NULL, 'SSO', '/sso', NULL, NULL, NULL, 'SSO');
ALTER SEQUENCE microservice_id_microservice_seq RESTART WITH 2;
 
 
-- Instrucciones de inserción para la tabla endpoint:
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('1', 'Obtener microservicios', 'GET', NULL, '/microservice/getMicroservices');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('2', 'Obtener endpoints', 'GET', NULL, '/endpoint/getEndpoints');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('3', 'Obtener microservicios seleccionados', 'GET', '1', '/endpoint/microservices/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('4', 'Crear endpoint', 'POST', NULL, '/endpoint/createEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('6', 'Actualizar endpoint', 'PUT', '0', '/endpoint/updateEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('5', 'Obtener roles de un endpoint', 'GET', '1', '/endpoint/roles/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('7', 'Desvincular role de un endpoint', 'DELETE', '2', '/role/removeEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('8', 'Vincular role a un endpoint', 'POST', '0', '/role/addEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('9', 'Desvincular microservicio de un endpoint ', 'DELETE', '2', '/microservice/removeEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('10', 'Vincular endpoint a un microservicio', 'POST', '0', '/microservice/addEndpoint');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('11', 'Obtener roles', 'GET', '0', '/role/getRoles');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('12', 'Obtener usuarios', 'GET', '0', '/getUsers');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('13', 'Obtener consultas sql', 'GET', '0', '/report/getReports');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('14', 'Crear consulta sql', 'POST', '0', '/report/createReport');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('15', 'Obtener roles', 'GET', '0', '/getRoles');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('16', 'Crear cuenta', 'POST', '0', '/createAccount');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('17', 'Crear microservicios', 'POST', '0', '/microservice/createMicroservice');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('19', 'Obtener rutas', 'GET', '1', '/route/routes');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('20', 'Obtener roles vinculados a un usuario', 'GET', '1', '/user/roles/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('21', 'Desvincular rol de un usuario', 'DELETE', '2', '/unbindUserRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('22', 'Vincular rol a un usuario', 'POST', '0', '/bindUserRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('23', 'Obtener endpoints de un rol', 'GET', '1', '/role/endpoints/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('24', 'Obtener usuarios de un rol', 'GET', '1', '/role/users/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('25', 'Obtener las rutas de un rol', 'GET', '1', '/role/routes/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('26', 'Vincular rutas a un rol', 'POST', '0', '/role/saveRoutes');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('27', 'Actualizar rol', 'PUT', '0', '/role/updateRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('28', 'Actualizar cuenta', 'PUT', '0', '/updateAccount');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('29', 'Obtener roles de una ruta', 'GET', '1', '/route/roles/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('30', 'Obtener aplicaciones de una ruta', 'GET', '1', '/route/apps/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('31', 'Crear ruta', 'POST', '0', '/route/createRoute');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('32', 'Obtener aplicaciones', 'GET', '0', '/app/getApplications');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('33', 'Obtener rutas de una app', 'GET', '1', '/app/routes/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('34', 'Obtener microservicios de una app', 'GET', '1', '/app/microservices/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('35', 'Obtener roles de una app', 'GET', '1', '/app/roles/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('36', 'Obtener endpoints de un microservicio', 'GET', '1', '/microservice/endpoints/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('37', 'Actualizar microservicios', 'PUT', '0', '/microservice/updateMicroservice');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('38', 'Obtener roles de una consulta sql', 'GET', '1', '/report/roles/checked');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('39', 'Vincular rol por report', 'POST', '0', '/report/addRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('40', 'Desvincular rol de una consulta sql', 'DELETE', '2', '/report/removeRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('41', 'Vincular un microservicio a una app', 'POST', '0', '/app/addMicroservice');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('42', 'Vincular rutas a una app', 'POST', '0', '/app/saveRoutes');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('43', 'Desvincular microservicio de una app', 'DELETE', '2', '/app/removeMicroservice');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('44', 'Vincular una app a una ruta', 'POST', '0', '/app/addRoute');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('45', 'Desvincular ruta de una app', 'DELETE', '2', '/app/removeRoute');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('46', 'Desvincular rol de una ruta', 'DELETE', '2', '/route/removeRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('47', 'Vincular rol a una ruta', 'POST', '0', '/route/addRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('48', 'Actualizar ruta', 'PUT', '0', '/route/updateRoute');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('49', 'Actualizar aplicacion', 'PUT', '0', '/app/updateApp');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('50', 'Desvincular rol de una app', 'DELETE', '2', '/app/removeRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('51', 'Vincular rol a una app', 'POST', '0', '/app/addRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('52', 'Crear rol', 'POST', '0', '/role/createRole');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('53', 'Crear app', 'POST', '0', '/app/createApp');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('54', 'Actualizar consulta sql', 'PUT', '0', '/report/updateReport');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('55', 'Crear base de datos', 'GET', '1', '/createDatabase');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('56', 'Crear select consulta dinamica', 'POST', '0', '/createSqlReport');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('57', 'Crear insert consulta dinamica', 'POST', '0', '/insertSQLQuery');
INSERT INTO endpoint (id_endpoint, description, method, numberparams, path) VALUES ('58', 'Crear update consulta dinamica', 'POST', '0', '/updateSQLQuery');

ALTER SEQUENCE endpoint_id_endpoint_seq RESTART WITH 59;


-- Instrucciones de inserción para la tabla app_route:
INSERT INTO app_route (app_id, route_id) VALUES ('1', '2');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '3');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '4');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '5');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '6');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '7');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '8');
INSERT INTO app_route (app_id, route_id) VALUES ('1', '1');



-- Instrucciones de inserción para la tabla app_microservice:
INSERT INTO app_microservice (app_id, id_microservice) VALUES ('1', '1');


-- Instrucciones de inserción para la tabla role_app:
INSERT INTO role_app (role_id, app_id) VALUES ('1', '1');

-- Instrucciones de inserción para la tabla role_users:
-- INSERT INTO role_users (user_id, role_id) VALUES ('1', '1');

-- Instrucciones de inserción para la tabla endpoint_microservice:
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('2', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('3', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('4', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('5', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('6', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('8', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('9', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('7', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('11', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('12', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('13', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('14', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('15', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('16', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('17', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('19', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('20', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('21', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('22', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('24', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('23', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('25', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('26', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('27', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('28', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('30', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('29', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('31', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('32', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('33', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('34', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('35', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('36', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('37', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('1', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('38', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('39', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('40', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('41', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('42', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('43', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('44', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('45', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('46', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('47', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('48', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('10', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('49', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('50', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('51', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('52', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('53', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('54', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('55', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('56', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('57', '1');
INSERT INTO endpoint_microservice (endpoint_id, microservice_id) VALUES ('58', '1');

-- Instrucciones de inserción para la tabla role_endpoint:
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '2');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '3');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '5');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '6');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '8');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '4');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '9');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '10');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '7');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '11');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '12');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '13');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '14');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '15');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '16');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '17');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '19');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '20');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '21');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '22');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '24');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '23');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '25');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '26');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '27');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '28');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '30');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '29');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '31');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '32');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '33');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '34');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '35');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '36');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '37');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '38');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '39');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '40');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '41');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '42');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '43');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '44');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '45');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '46');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '47');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '48');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '49');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '50');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '51');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '52');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '53');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '1');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '54');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '55');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '56');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '57');
INSERT INTO role_endpoint (role_id, endpoint_id) VALUES ('1', '58');

-- Instrucciones de inserción para la tabla report_filter_filter_validator:


-- Instrucciones de inserción para la tabla role_route:
INSERT INTO role_route (role_id, route_id) VALUES ('1', '1');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '2');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '3');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '4');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '5');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '6');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '7');
INSERT INTO role_route (role_id, route_id) VALUES ('1', '8');
