package com.co.lowcode.sso.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.util.regex.Matcher;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public final class Util {
	public static File convert(String usuario, MultipartFile file) throws IOException
	{    
		 Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	    File convFile = new File(usuario+"_"+file.getOriginalFilename() + "_" +  timestamp.getTime());
	   
	    //convFile.createNewFile();
	    FileOutputStream fos = new FileOutputStream(convFile); 
	    fos.write(file.getBytes());
	    fos.close(); 
	    return convFile;
	}
    
    public static String convertirCamelCaseToUnderScore(String s){
    	String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return s.replaceAll(regex, replacement).toUpperCase();
    }
    
    public static String limpiarAcentos(String cadena) {
        String limpio =null;
        if (cadena !=null) {
            String valor = cadena;
            valor = valor.toUpperCase();
            // Normalizar texto para eliminar acentos, dieresis, cedillas y tildes
            limpio = Normalizer.normalize(valor, Normalizer.Form.NFD);
            // Quitar caracteres no ASCII excepto la enie, interrogacion que abre, exclamacion que abre, grados, U con dieresis.
            limpio = limpio.replaceAll("[^\\p{ASCII}(N\u0303)(n\u0303)(\u00A1)(\u00BF)(\u00B0)(U\u0308)(u\u0308)]", "");
            // Regresar a la forma compuesta, para poder comparar la enie con la tabla de valores
            limpio = Normalizer.normalize(limpio, Normalizer.Form.NFC);
        }
        return limpio;
    }
    
	public static File convertMultiPartFiletoFile(MultipartFile file, String folder,String nameFile) throws IOException
	{    
		String n = nameFile==""?file.getOriginalFilename():nameFile;
		
		File convFile = new File(folder+"/"+n);

		//convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile); 
		fos.write(file.getBytes());
		fos.close(); 
		return convFile;
	}

	  public static String decrypt(byte[] raw, String textToDecrypt) throws Exception {
	        byte[] encrypted = Base64.decode(textToDecrypt.getBytes());
	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
	        Cipher cipher = javax.crypto.Cipher.getInstance("AES");
	        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
	        byte[] decrypted = cipher.doFinal(encrypted);
	        return new String(decrypted, "UTF-8");
	    }

	    public static String encryptDecrypt(String text) {
	    	
	    	String input = new String(Base64.decode(text.getBytes()));
	        char[] key = {'R','Q','W','M','G','L','S','A','Q','C','B'};
	        StringBuilder output = new StringBuilder();

	        for(int i = 0; i < input.length(); i++) {
	            output.append((char) (input.charAt(i) ^ key[i % key.length]));
	        }

	        return  output.toString();
	    }
	    
	    public static String getUserName(String auth) {
	    	org.springframework.security.jwt.Jwt jwtToken = JwtHelper.decode(auth);
	    	String jwtClaims = jwtToken.getClaims();
	    	try {
				JsonNode jwtClaimsJsonNode = new ObjectMapper().readTree(jwtClaims);
				String username = jwtClaimsJsonNode.get("sub").asText();
				return username;
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	return "";
	    }
	
		public static String encodeFileToBase64(String fileName) {
		    try {
		    	File file = new File(fileName);
		        byte[] fileContent = Files.readAllBytes(file.toPath());
		        return java.util.Base64.getEncoder().encodeToString(fileContent);
		    } catch (IOException e) {
		        throw new IllegalStateException("could not read file " + fileName, e);
		    }
		}
		
		public static Boolean validateEmailAddress(String emailAddress) {
			java.util.regex.Pattern regexPattern;
		    Matcher regMatcher;
	        regexPattern =java.util.regex.Pattern.compile("([a-z0-9]+(\\.?\\-?\\_?[a-z0-9])*)+@(([a-z]+(\\.?\\-?[a-z0-9])*)\\.([a-z]+))+");
	        regMatcher   = regexPattern.matcher(emailAddress);
	        if(regMatcher.matches()) {
	            return true;
	        } else {
	            return false;
	        }
	    }

}
