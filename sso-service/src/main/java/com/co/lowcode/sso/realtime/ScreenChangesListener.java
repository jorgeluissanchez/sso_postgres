package com.co.lowcode.sso.realtime;

import com.co.lowcode.sso.rethinkdb.RethinkDBConnectionFactory;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Cursor;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class ScreenChangesListener {
    protected final Logger log = LoggerFactory.getLogger(ScreenChangesListener.class);

    private static final RethinkDB r = RethinkDB.r;

    @Autowired
    private RethinkDBConnectionFactory connectionFactory;

    @Autowired
    private SimpMessagingTemplate webSocket;

    @Async
    public void pushChangesToWebSocket() throws java.net.ConnectException {
        Cursor cursor = r.db("lowcode").table("component").changes()
        		.optArg("include_types", true)
        		/*.optArg("include_states", true)
        		.optArg("include_initial", true)
        		.optArg("squash", true)*/
        		
                //.getField("new_val")
                .run(connectionFactory.createConnection());

        while (cursor.hasNext()) {
        	HashMap o = (HashMap) cursor.next();
        	String id = "";
        	if( o.get("type").equals("remove")) {
        		id = ((HashMap) o.get("old_val")).get("screenId").toString();
        		o.put("id", ((HashMap) o.get("old_val")).get("id").toString());
        		o.remove("new_val");
        	}else {
        		id = ((HashMap) o.get("new_val")).get("screenId").toString();
        	}
        	o.remove("old_val");
            webSocket.convertAndSend("/structure/" + id , o );
        }
        
        
        
    }

}
