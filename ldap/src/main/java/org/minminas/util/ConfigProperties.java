package org.minminas.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class ConfigProperties
{
  
  static InputStream inputStream;
 
  public static String getLdapPropValues(String propertyName) throws IOException
  {
    String result = "";
    try {
      Properties prop = new Properties();
      String propFileName = "properties/ldap.properties";
      
      inputStream = ConfigProperties.class.getClassLoader().getResourceAsStream(propFileName);
      
      if (inputStream != null) {
        prop.load(inputStream);
      } else {
        throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
      }
      
      result = prop.getProperty(propertyName);
    }
    catch (Exception e) {
      e.printStackTrace();
      result = null;
    } finally {
      inputStream.close();
    }
    return result;
  }
  
  
  public static Configuration getProperties(){
	  Configurations configs = new Configurations();
	  try
	  {
		  
		 
	      return  configs.properties(new File( ConfigProperties.class.getClassLoader().getResource("ldap.properties").toString()));
	  }
	  catch (ConfigurationException cex)
	  {
		  cex.printStackTrace();
	  }
	  return null;
  }
  
}
