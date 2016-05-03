package org.apache.tika.parser.ner.grobid;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tika.parser.ner.NERecogniser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidQuantitiesNERecogniser implements NERecogniser {
	 private static final Logger LOG = LoggerFactory.getLogger(GrobidQuantitiesNERecogniser.class);
	 private boolean available = false;
	 private static final String JETTY_SERVER_URL = "http://localhost:9876"; 
	 private static final String REST_END_POINT = "/processQuantityText";
	 public static final Set<String> ENTITY_TYPES = new HashSet<String>(){{
	        add("MEASUREMENT_TYPES");
	        add("MEASUREMENT_NAMES");
	        add("RAW_VALUES");
	        add("NORMALISED_VALUES");
	    }};
	    
	public GrobidQuantitiesNERecogniser()
	{
		// Checking Jetty Server Status.
		if(checkServerStatus())
		{
			available = true;
		}
		else
		{
			available = false;
		}
		LOG.info("Available for service ? {}", available);
	}
	
	private boolean checkServerStatus()
	{
		String command = "nmap -p 9876 127.0.0.1";
		return runSystemCommand(command);	
	}
	
	private boolean runSystemCommand(String command)
	{
		String s = "";
		/*
		 *  nmap -p 9876 127.0.0.1 ( Test Open)

			Starting Nmap 6.40 ( http://nmap.org ) at 2016-04-23 12:21 PDT
			Nmap scan report for localhost (127.0.0.1)	
			Host is up (0.000039s latency).
			PORT     STATE SERVICE
			9876/tcp open  sd

			Nmap done: 1 IP address (1 host up) scanned in 0.03 seconds
			=================================================================
			nmap -p 9874 127.0.0.1 ( Test closed)

			Starting Nmap 6.40 ( http://nmap.org ) at 2016-04-23 12:22 PDT
			Nmap scan report for localhost (127.0.0.1)
			Host is up (0.000038s latency).
			PORT     STATE  SERVICE
			9874/tcp closed unknown

			Nmap done: 1 IP address (1 host up) scanned in 0.04 seconds
		 * 
		 */
		s = runProcessCommand(command);
		if(s.contains("open"))
		{
			return true;
		}
		if(s.contains("closed"))
		{
			return false;
		}
		return false;
	}
	
	private String runProcessCommand(String command)
	{
		StringBuilder sb = new StringBuilder();
		try {
			Process p = Runtime.getRuntime().exec(command); 
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s = "";
			// reading output stream of the command
			while ((s = inputStream.readLine()) != null) {
				sb.append(s);
		}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public Set<String> getEntityTypes() {
		return ENTITY_TYPES;
	}

	/**
     * recognises names of entities in the text
     * @param text text which possibly contains names
     * @return map of entity type -> set of names
     */
	
	@Override
	public Map<String, Set<String>> recognise(String text) 
	{
		Map<String, Set<String>> entities = new HashMap<String,Set<String>>();
        Set<String> measurementTypes = new HashSet<String>();
        Set<String> measurementNames = new HashSet<String>();
        Set<String> rawValues = new HashSet<String>();
        Set<String> normalizedValues = new HashSet<String>();
		try
		{
			String url = "curl -X POST -d " + "test=" + text + JETTY_SERVER_URL+ REST_END_POINT; 
			String jsonResponse = runProcessCommand(url);
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject)jsonParser.parse(jsonResponse);
			JSONArray measurements = (JSONArray)jsonObject.get("measurements");
			for(int i=0;i<measurements.size();i++)
			{
				JSONObject mea_object = (JSONObject)measurements.get(i);
				JSONObject quan_object = (JSONObject) mea_object.get("quantity");
				if(quan_object != null)
				{
					String type = (String) quan_object.get("type");
					measurementTypes.add(type);
					String rawValue = (String) quan_object.get("rawValue");
					rawValues.add(rawValue);
				}
				String normalisedValue = (String) mea_object.get("normalizedQuantity");
				normalizedValues.add(normalisedValue);
				JSONObject norm_object = (JSONObject) mea_object.get("normalisedUnit");
				String name = (String) norm_object.get("name");
				measurementNames.add(name);
			}
			
			entities.put("MEASUREMENT_TYPES", measurementTypes);
			entities.put("MEASUREMENT_NAMES", measurementNames);
			entities.put("RAW_VALUES", rawValues);
			entities.put("NORMALISED_VALUES", normalizedValues);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return entities;
	}

}
