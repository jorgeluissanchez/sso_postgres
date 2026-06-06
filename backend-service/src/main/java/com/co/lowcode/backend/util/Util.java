package com.co.lowcode.backend.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;


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

	public static File convertMultiPartFiletoFile(MultipartFile file, String folder,String nameFile) throws IOException
	{    
		String n = nameFile==""?file.getOriginalFilename():nameFile;

		File convFile = new File(folder+n);

		//convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile); 
		fos.write(file.getBytes());
		fos.close(); 
		return convFile;
	}
	
	
	public static List<Map<String,Object>> processFile(File file, Map<String, String> match, Integer rowHeader, String sheetName) throws FileNotFoundException, IOException, InstantiationException{
		Workbook workbook;
		if (Files.getFileExtension(file.getName()).equalsIgnoreCase("xls")) {
			workbook = new HSSFWorkbook(new FileInputStream(file));
		} else if (Files.getFileExtension(file.getName()).equalsIgnoreCase("xlsx")) {
			workbook = new XSSFWorkbook(new FileInputStream(file));
		} else {
			throw new IllegalArgumentException("El archivo recibido no es valido");
		}
		return processFile(workbook, match, rowHeader, sheetName) ;
	}
	
	
	public static List<Map<String,Object>> processFile(String filename, InputStream inputStream, Map<String, String> match, Integer rowHeader, String sheetName) throws FileNotFoundException, IOException, InstantiationException{
		Workbook workbook;
		if (Files.getFileExtension(filename).equalsIgnoreCase("xls")) {
			workbook = new HSSFWorkbook(inputStream);
		}else if (Files.getFileExtension(filename).equalsIgnoreCase("xlsx")) {
			workbook = new XSSFWorkbook(inputStream);
		} else {
			throw new IllegalArgumentException("El archivo recibido no es valido");
		}
		
		return processFile(workbook, match, rowHeader, sheetName) ;
	}

	//Procesa el archivo xls a objeto
	public static List<Map<String,Object>> processFile(Workbook workbook, Map<String, String> match, Integer rowHeader, String sheetName) throws FileNotFoundException, IOException, InstantiationException{
		Sheet sheet = workbook.getSheet(sheetName);
		Row row;
		Cell cell;

		int rows; // No of rows
		rows = sheet.getPhysicalNumberOfRows();
		int cols = 0; // No of columns
		int tmp = 0;

		// This trick ensures that we get the data properly even if it doesn't
		// start from first few rows
		for (int i = 0; i < 3 ; i++) {
			row = sheet.getRow(i);
			if (row != null) {
				tmp = sheet.getRow(i).getPhysicalNumberOfCells();
				if (tmp > cols)
					cols = tmp;
			}
		}
		List<Map<String,Object>> listObject = new ArrayList<Map<String,Object>>();
		List<String> headers = new ArrayList<>();
		for (int r = 0; r < rows; r++) {
			row = sheet.getRow(r);
			if (row != null) {
				Map<String,Object> object = new LinkedHashMap<String, Object>();
				for (int c = 0; c < cols; c++) {
					cell = row.getCell((short) c);
					if (cell != null) {
						Object valorCelda = null;
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_STRING:
							valorCelda = cell.getStringCellValue();
							break;
						case Cell.CELL_TYPE_NUMERIC:
							valorCelda = cell.getNumericCellValue();
							break;
						case Cell.CELL_TYPE_FORMULA:
							try {
								valorCelda = cell.getNumericCellValue();
							}catch (Exception e) {
								try {
									valorCelda = cell.getStringCellValue();
								}catch (Exception e2) {
									valorCelda = "";
								}
							}
							
							break;
						}
						

						if(r == rowHeader && valorCelda != null) {
							headers.add(valorCelda.toString());
						}else if(r > rowHeader){
							if(sheet.getRow(rowHeader)
									.getCell((short) c)==null){
								continue;
							}
							
							if(match !=null && !match.isEmpty()) {
								
								if(c < headers.size()) {
									String m = match.get(headers.get(c));
									if(m != null) {
										object.put(m, valorCelda);
										continue;
									}
								}
							}
							if(c < headers.size()) {
								object.put(headers.get(c), valorCelda);
							}
							
						}
					}
				}
				if(!object.isEmpty()){
					listObject.add(object);
				}
			}
		}
		return listObject;
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

}
