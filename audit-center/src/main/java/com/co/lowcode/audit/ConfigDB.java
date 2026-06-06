package com.co.lowcode.audit;

import java.io.InputStream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.co.lowcode.audit.config.GeneralConfig;


@Configuration
@EnableTransactionManagement
@EnableAutoConfiguration
@EnableJpaRepositories(
        entityManagerFactoryRef = "sqlserverEntityManagerFactory", 
        transactionManagerRef = "sqlserverTransactionManager",
        basePackages = { "com.co.lowcode.audit.repository" })
public class ConfigDB {

	@Autowired
   	GeneralConfig config;

   @Autowired
    JpaVendorAdapter jpaVendorAdapter; 
    InputStream inputStream;

   /* @Bean
    public HibernateExceptionTranslator hibernateExceptionTranslator() {
        return new HibernateExceptionTranslator();
    }*/
    
    /**
     * Primary because if we have activated embedded databases, we do not want
     * the application to connect to an external database.
     */
    
    
    @Bean(name = "mysqlserver")
    @Primary
    public DataSource dataSource() {
    	ApplicationContext ac = new ClassPathXmlApplicationContext("/context.xml", ConfigDB.class);
        DataSource dataSource = (DataSource) ac.getBean(config.getContextName());//Toma la instancia sqlserver
        return dataSource;
    }
       
   
    @Bean(name = "sqlserverEntityManager")
    public EntityManager sqlserverEntityManager() {
        return sqlserverEntityManagerFactory().createEntityManager();
    }
    
  
    public HibernateJpaVendorAdapter oraclehibernateJpaVendorAdapter() {  
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();  
        Oracle12cDialect oracleSQLDialect = new Oracle12cDialect();       
        hibernateJpaVendorAdapter.setDatabasePlatform(oracleSQLDialect.toString());  
        hibernateJpaVendorAdapter.setShowSql(true);  
        hibernateJpaVendorAdapter.setGenerateDdl(true);    
        hibernateJpaVendorAdapter.setDatabase(Database.ORACLE);                
        return hibernateJpaVendorAdapter;  
    } 

    
    public HibernateJpaVendorAdapter postgresHibernateJpaVendorAdapter() {  
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();  
        PostgreSQL94Dialect postgresSQLDialect = new PostgreSQL94Dialect();       
        hibernateJpaVendorAdapter.setDatabasePlatform(postgresSQLDialect.toString());
        hibernateJpaVendorAdapter.setShowSql(false);
        hibernateJpaVendorAdapter.setGenerateDdl(true);    
        hibernateJpaVendorAdapter.setDatabase(Database.POSTGRESQL);                
        return hibernateJpaVendorAdapter;  
    } 
    
    public HibernateJpaVendorAdapter sqlserverHibernateJpaVendorAdapter() {  
        HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();  
        SQLServer2012Dialect sqlserverSQLDialect = new SQLServer2012Dialect();       
        hibernateJpaVendorAdapter.setDatabasePlatform(sqlserverSQLDialect.toString());
        hibernateJpaVendorAdapter.setShowSql(false);
        hibernateJpaVendorAdapter.setGenerateDdl(true);    
        hibernateJpaVendorAdapter.setDatabase(Database.SQL_SERVER);                
        return hibernateJpaVendorAdapter;  
    } 
    
    
    
   
    @Bean(name = "sqlserverEntityManagerFactory")
    public EntityManagerFactory sqlserverEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean lef = new LocalContainerEntityManagerFactoryBean();
        lef.setDataSource(dataSource()); 
        if(config.getDatabaseEngine().equals("POSTGRES")) {
        	lef.setJpaVendorAdapter(postgresHibernateJpaVendorAdapter());
        }else if(config.getDatabaseEngine().equals("ORACLE")) {
        	lef.setJpaVendorAdapter(oraclehibernateJpaVendorAdapter());
        }else if(config.getDatabaseEngine().equals("SQLSERVER")) {
        	lef.setJpaVendorAdapter(sqlserverHibernateJpaVendorAdapter());
        }
        
        lef.setPackagesToScan("com.co.lowcode.audit.model");
        lef.setPersistenceUnitName("sqlserverPersistenceUnit");
        lef.afterPropertiesSet();
        return lef.getObject();
    }

    @Bean(name = "sqlserverTransactionManager")
    public PlatformTransactionManager sqlserverTransactionManager() {
        return new JpaTransactionManager(sqlserverEntityManagerFactory());
    }

}
