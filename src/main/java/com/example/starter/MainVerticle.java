package com.example.starter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle 
{	
  private final	int FORECASTS_PER_DAY = 8;  
  private final String API = "http://api.openweathermap.org/";
  private final String CURRENT = "data/2.5/weather?";
  private final String FORECAST = "data/2.5/forecast?";
  private final String APPID = "APPID=ae63f754a64fccd1530faab96dca1fac";
  private final String UNITS = "units=metric";   
  private final static String CITIES = "city.list.json";  
  private final String icon = "http://openweathermap.org/img/w/";
  
  private WebClient client;
  private HttpServer server;
  private Router router;
  private WebClientOptions options;
  private HashMap<String, Country> countries;    
  
  public static void main(String[] args) 
  {  
	    VertxOptions vertxOptions = new VertxOptions();	    
	    //prolong default thread block time due to city list file parsing
	    vertxOptions.setBlockedThreadCheckInterval(999999999);	    
	    Vertx.vertx(vertxOptions).deployVerticle(MainVerticle.class.getName());	 
  }  
  
  @Override
  public void start(Future<Void> startFuture) throws Exception 
  {    
	// Init
	int port = Integer.getInteger("http.port", 8080);
	initTree();  
	options = new WebClientOptions();
    client = WebClient.create(vertx, options);
    server = vertx.createHttpServer();    
    router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");
    
    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    
    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));    
    
    // define routes
    router.get("/")
    	.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain");
      response.end("HTTP server started on port " + port);
    });

    router.get("/healthcheck")
    	.handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.putHeader("content-type", "text/plain");
      response.end("I am Alive!");
    });
        
    router.get("/hello")
    	.handler(HTTPRequestValidationHandler.create()
    			.addQueryParam("name", ParameterType.GENERIC_STRING, true))
    	.handler(routingContext -> {
    	
		      HttpServerResponse response = routingContext.response();		      
		      RequestParameters params = routingContext.get("parsedParameters");
		      RequestParameter name = params.queryParameter("name");		           
		      
		      response.putHeader("content-type", "text/plain");
		  	  response.end(("Hello " + name).trim());     
      
	});
				
	router.get("/cities")
	.handler(HTTPRequestValidationHandler.create()
			.addQueryParam("country", ParameterType.GENERIC_STRING, true))
	.handler(routingContext -> {
	
			HttpServerResponse response = routingContext.response();
			
			RequestParameters params = routingContext.get("parsedParameters");
			RequestParameter c = params.queryParameter("country");					

			JsonArray jCities = new JsonArray();
			
			if(countries.get(c.toString().toLowerCase().trim()) == null)
			{
				response.putHeader("content-type", "text/plain");
				response.end("No country found");	
			}
			
			HashMap<String, Integer> cities = countries.get(c.toString().toLowerCase().trim()).getCityes();	
			
			
			for(String key : cities.keySet())
			{
				JsonObject cityObject = new JsonObject();
				
				key = firstToUpper(key);
				
				cityObject.put("id", key);
				cityObject.put("text", key);							
				
				jCities.add(cityObject);
			}									
			
			response.putHeader("content-type", "application/json");
			response.end(jCities.encodePrettily());			
	});

    router.get("/currentforecasts")
    .handler(HTTPRequestValidationHandler.create()
			.addQueryParam("city", ParameterType.GENERIC_STRING, true)
			.addQueryParam("country", ParameterType.GENERIC_STRING, true))    		   		
    .handler(routingContext -> {
      HttpServerResponse res = routingContext.response();

      RequestParameters params = routingContext.get("parsedParameters");
      RequestParameter city = params.queryParameter("city");
      RequestParameter country = params.queryParameter("country");
      
      int cityId = getCityId(city.getString(), country.getString());            
      
      if(cityId == 0)
      {
	  	  res.putHeader("content-type", "application/json");
          res.end(new JsonObject().put("message", "No city found").encodePrettily());
      }
      else
      {
    	  JsonObject forecast = new JsonObject();
          
          client.getAbs(API + CURRENT + APPID + "&" + "id=" + cityId + "&" + UNITS).as(BodyCodec.jsonObject()).send(ar-> {
       	          if (ar.succeeded()) {
       	            HttpResponse<JsonObject> response = ar.result();

       	            // Decode the body as a json object
       	            Map<?, ?> body = response.body().getMap();
       	            System.out.println("Received response with status code" + response.statusCode());    	                   
       	            
       	            LinkedHashMap<?, ?> main = (LinkedHashMap<?, ?>) body.get("main");
       	            LinkedHashMap<?, ?> sys = (LinkedHashMap<?, ?>) body.get("sys");
       	            ArrayList<?> weatherList = (ArrayList<?>) body.get("weather");

       	            String name = (String) body.get("name");
       	            String state =  (String) sys.get("country");
       	            Number temp = (Number) main.get("temp");
       	            Number humidity = (Integer) main.get("humidity"); 
       	            
       	            LinkedHashMap<?, ?> weather = (LinkedHashMap<?, ?>) weatherList.get(0);
       	            
       	            forecast.put("city", name);
       	            forecast.put("country", state);
       	            forecast.put("temp", temp.intValue());
       	            forecast.put("humidity", humidity.intValue());      
       	            forecast.put("weather", weather.get("main"));
       	            forecast.put("description", firstToUpper((String) weather.get("description")));
       	            forecast.put("icon", icon + weather.get("icon") + ".png");
       	            forecast.put("date", LocalDate.now().toString()); 

       	            System.out.println("forecast is ready");        
       	          } 
       	          else
       	          {
       	            System.out.println("Something went wrong " + ar.cause().getMessage());
       	            forecast.put("message", ar.cause().getMessage());
       	          }
       	          
       	          res.putHeader("content-type", "application/json");
       	          res.end(forecast.encodePrettily());       	          
          }); 
      }         
      
    }); 
    
    router.get("/forecasts")
    .handler(HTTPRequestValidationHandler.create()
			.addQueryParam("city", ParameterType.GENERIC_STRING, true)
			.addQueryParam("country", ParameterType.GENERIC_STRING, true)
    		.addQueryParam("days", ParameterType.INT, true))
    .handler(routingContext -> {
        HttpServerResponse res = routingContext.response();

        RequestParameters params = routingContext.get("parsedParameters");
        
        RequestParameter city = params.queryParameter("city");
        RequestParameter country = params.queryParameter("country");
        
        int days = params.queryParameter("days").getInteger();
        
        if(days > 5 || days < 1)
        {
        	res.putHeader("content-type", "text/plain");
        	res.end(new JsonObject().put("message", "Days paramter should be between 1 and 5").encodePrettily());
        }
        else
        {   
        	int cityId = getCityId(city.getString(), country.getString());
        	
        	if(cityId == 0)
            {
      	  	    res.putHeader("content-type", "application/json");
                res.end(new JsonObject().put("message", "No city found").encodePrettily());
            }
            else
            {
            	JsonObject forecast = new JsonObject();        
    	        
    	        client.getAbs(API + FORECAST + APPID + "&" + "id=" + cityId + "&" + UNITS).as(BodyCodec.jsonObject()).send(ar-> {
    		          if (ar.succeeded()) 
    		          {
    		            HttpResponse<JsonObject> response = ar.result();		            
    		            
    		            Map<?, ?> body = response.body().getMap();
    		            System.out.println("Received response with status code " + response.statusCode());
    		            
    		            if(response.statusCode() != 200 || cityId == 0)
    		            {
    		            	forecast.put("messgae", body.get("message"));
    		            }
    		            else
    		            {            
    			            ArrayList<?> list = (ArrayList<?>) body.get("list");
    			            LinkedHashMap<?, ?> town = (LinkedHashMap<?, ?>) body.get("city");
    			            
    			            System.out.println("forecast list length: " + list.size());
    			            
    			            forecast.put("id", town.get("id"));
    			            forecast.put("city", town.get("name"));
    			            forecast.put("country", town.get("country"));
    			            
    			            JsonArray forecasts = new JsonArray();	            
    			            
    			            //jump 24 hours
    			            for(int i = 0; i < days * FORECASTS_PER_DAY; i += FORECASTS_PER_DAY)
    			            {	
    			            	JsonObject localForecast = new JsonObject();
    			            	
    			            	ArrayList<Number> temp_av = new ArrayList<Number>();
    			            	ArrayList<Number> temp_min_av = new ArrayList<Number>();
    			            	ArrayList<Number> temp_max_av = new ArrayList<Number>();
    			            	ArrayList<Number> humidity_av = new ArrayList<Number>();
    			            	ArrayList<Number> speed_av = new ArrayList<Number>();
    			            	ArrayList<Number> deg_av = new ArrayList<Number>();
    			            	
    			            	//internal daily loop
    			            	for(int j = 0; j < 8; j++)
    			            	{
    			            		Map<?, ?> content = (Map<?, ?>) list.get( i+j );
    			            		
    			            		//create arrays for calculate average
    			            		LinkedHashMap<?, ?> main = (LinkedHashMap<?, ?>) content.get("main");
    				            	ArrayList<?> weatherList = (ArrayList<?>) content.get("weather");
    				            	LinkedHashMap<?, ?> wind = (LinkedHashMap<?, ?>) content.get("wind");
    				            	LinkedHashMap<?, ?> weather = (LinkedHashMap<?, ?>) weatherList.get(0);		            	
    				            	
    				            	Number temp = (Number) main.get("temp");
    				            	Number temp_min =  (Number) main.get("temp_min");
    				            	Number temp_max =  (Number) main.get("temp_max");			            	
    				            	Number humidity = (Number) main.get("humidity");
    				            	Number speed = (Number) wind.get("speed");
    				            	Number deg = (Number) wind.get("deg");
    				            	
    				            	//harvest numerics in arrays
    				            	temp_av.add(temp);
    				            	temp_min_av.add(temp_min);
    				            	temp_max_av.add(temp_max);
    				            	humidity_av.add(humidity);
    				            	speed_av.add(speed);
    				            	deg_av.add(deg);
    				            	
    				            	//just get middle value for strings
    				            	if(j == 3)
    				            	{	String date = (String) content.get("dt_txt");
    				            		date = (String) date.subSequence(0, date.length()-9);
    				            		
    					            	localForecast.put("date", date);
    					            	localForecast.put("weather", weather.get("main"));
    					            	localForecast.put("description", firstToUpper((String) weather.get("description")));
    					            	localForecast.put("icon", icon + weather.get("icon") +".png");
    				            	}    				            	
    			            	}
    			            		            	
    			            	localForecast.put("wind_speed", average(speed_av));
    			            	localForecast.put("wind_deg", average(deg_av));  
    			            	localForecast.put("dayTemp", average(temp_av));		            	
    			            	localForecast.put("minTemp", average(temp_min_av));
    			            	localForecast.put("maxTemp", average(temp_max_av));
    			            	localForecast.put("humidity", average(humidity_av));           	
    			            		            	
    			            	forecasts.add(localForecast);	            	
    			            }
    			            
    			            forecast.put("forecasts", forecasts);	            
    			            
    			            System.out.println("forecast is ready");
    		          	}
    		          } 
    		          else
    		          {
    		            System.out.println("Something went wrong " + ar.cause().getMessage());
    		            forecast.put("message", ar.cause().getMessage());
    		          }
    		          
    		          res.putHeader("content-type", "application/json");
    		          res.end(forecast.encodePrettily());    	          
    	        });
            }	        
        }	          
    });
    
    // Serve the static resources
    router.route().handler(StaticHandler.create());

    // start server
    server.requestHandler(router).listen(port, result -> {
      if (result.succeeded()) 
      {
        startFuture.complete();
        System.out.println("HTTP server started on port " + port);
      } 
      else
        startFuture.fail(result.cause());
    });  
  } 
  
  //capitalize the first letter
  public String firstToUpper(String key) 
  {
	if  (key == null)
	   return "";
	   
	key = key.trim();  
	
	if  (key.equals(""))
	   return "";
		 
	key = key.substring(0,1).toUpperCase() + key.substring(1).toLowerCase(); 
	  
	return key;
  }

  public void initTree() 
  {	
	  try 
	  {
		  System.out.println("Loading city list...");		
		  JsonArray cityList = new JsonArray(new String(Files.readAllBytes(Paths.get(CITIES))));
		  
		  System.out.println("Start making tree...");
		  
		  countries = new HashMap<String, Country>();
		  
		  //create hash table of countries
		  for(int i = 0; i < cityList.size(); i++)
		  {
			  JsonObject city = cityList.getJsonObject(i);
			  String country = city.getString("country").toLowerCase();
			  
			  //if this country still not exists
			  if(countries.get(country) == null)
			  	  countries.put(country, new Country(country));			  
		  }
		  
		  //add cities to countries
		  for(int i = 0; i < cityList.size(); i++)
		  {
			  JsonObject city = cityList.getJsonObject(i);
			  
			  String country = city.getString("country").toLowerCase();
			  String name = city.getString("name").toLowerCase();
			  int id = city.getInteger("id");
			  
			  if(countries.get(country) != null)
			  		countries.get(country).getCityes().put(name, id);			  
		  }
		  
		  System.out.println("countries and cities tree created succefully!");
		
		} 
		catch (Exception e) 
		{		
			e.printStackTrace();
		}
	
  }

//reads city and country from tree and returns an id
  public int getCityId(String cityParam, String countryParam) 
  {
	 int id = 0;
	 
	 try
	 {
		id = countries.get(countryParam.toLowerCase().trim()).getCityes().get(cityParam.toLowerCase().trim());
	 }
	 catch(NullPointerException e)
	 {
		 System.out.println("No city found");		 	 
	 }	  
	 
	 return id;
  }

  //returns an int average of numbers from the list
  public double average(ArrayList<Number> list)
  {
	int average = 0;
	
	for(Number x: list)
		average += x.intValue();  
	  
	try
	{
		average = average/list.size();
	}
	catch(ArithmeticException e)
	{
		System.out.println("Arithmetic Exception: / by 0");
	}	
	return average;	  
  }   
}
