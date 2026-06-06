package com.co.lowcode.sso.rethinkdb;

import com.co.lowcode.sso.realtime.ScreenChangesListener;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;

public class DbInitializer implements InitializingBean {
    @Autowired
    private RethinkDBConnectionFactory connectionFactory;

    @Autowired
    private ScreenChangesListener chatChangesListener;

    private static final RethinkDB r = RethinkDB.r;

    @Override
    public void afterPropertiesSet() throws Exception {
      //  createDb();
      //  chatChangesListener.pushChangesToWebSocket();
    }

    private void createDb() throws java.net.ConnectException {
        Connection connection = connectionFactory.createConnection();
        List<String> dbList = r.dbList().run(connection);
        if (!dbList.contains("lowcode")) {
            r.dbCreate("lowcode").run(connection);
        }
        List<String> tables = r.db("lowcode").tableList().run(connection);
        if (!tables.contains("component")) {
            r.db("lowcode").tableCreate("component").run(connection);
         //   r.db("lowcode").table("component").indexCreate("id").run(connection);
        }
        if (!tables.contains("brick")) {
            r.db("lowcode").tableCreate("brick").run(connection);
         //   r.db("lowcode").table("component").indexCreate("id").run(connection);
        }
    }
}
