package com.co.lowcode.sso.rethinkdb;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;


public class RethinkDBConnectionFactory {
    private String host;

    public RethinkDBConnectionFactory(String host) {
        this.host = host;
    }

    public Connection createConnection() throws java.net.ConnectException {
    	return RethinkDB.r.connection().hostname(host).connect();
    }
}
