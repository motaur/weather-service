package com.example.starter;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle 
{	
  // Deploy the verticle and execute the test methods when the verticle is successfully deployed
  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) 
  {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }
  
  @Test
  @RepeatedTest(3)
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void server_test(Vertx vertx, VertxTestContext testContext) 
  {
    WebClient client = WebClient.create(vertx);

    client.get(8080, "localhost", "/")
      .as(BodyCodec.string())
      .send(testContext.succeeding(response -> testContext.verify(() -> 
      {
        Assert.assertTrue(response.body().equals("HTTP server started on port 8080"));
        testContext.completeNow();

      })));
  }
  
  @Test  
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void healthcheck_test(Vertx vertx, VertxTestContext context) 
  {
      WebClient client = WebClient.create(vertx);

      client.get(8080, "localhost", "/" + "healthcheck")
      .as(BodyCodec.string())
      .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
        else 
        {
          context.verify(() -> Assert.assertEquals("I am Alive!" ,ar.result().body()));
          context.completeNow();
        }
      });
  }

  @Test  
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void hello_test(Vertx vertx, VertxTestContext context) 
  {
    Checkpoint responsesReceived = context.checkpoint(7);
    WebClient client = WebClient.create(vertx);

    String expected = "";
    String sent = "";
    
    for (int i = 0; i < 7; i++) 
    { 
      switch(i) 
      {
        case 0:
          sent = "hello?name=name";
          expected ="Hello name";
          break;

        case 1:
          sent = "hello?name";
          expected ="Hello";
          break;

        case 2:
          sent = "hello?name";
          expected ="Hello";        
          break; 

        case 3:
          sent = "hello";
          expected ="Bad Request";     
         break;

        case 4:
          sent = "hello?name=null";
          expected ="Hello null";       
          break;

        case 5:
          sent = "hello?name=" + null;
          expected ="Hello null";
          break;

        case 6:
          sent = "hello?";
          expected ="Bad Request";
          break;
      }

      final String exp = expected;

        client.get(8080, "localhost", "/" + sent)
          .as(BodyCodec.string())
          .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
            else 
            {
              context.verify(() -> Assert.assertEquals(exp ,ar.result().body()));
              responsesReceived.flag();
            }
          }); 
      }      
        
  }  

  @Test 
  void firstToUpper_test(Vertx vertx, VertxTestContext context) 
  {
    MainVerticle vert = new MainVerticle();
    
    Assert.assertEquals("Some",  vert.firstToUpper("   some"));
    Assert.assertEquals("",  vert.firstToUpper(""));
    Assert.assertEquals("",  vert.firstToUpper(" "));
    Assert.assertEquals("",  vert.firstToUpper(null));
    Assert.assertEquals("Some",  vert.firstToUpper("Some"));
    Assert.assertEquals("S",  vert.firstToUpper("s"));
    Assert.assertEquals("S",  vert.firstToUpper("S"));    
    Assert.assertEquals("0",  vert.firstToUpper("0"));
    Assert.assertEquals("*=-9",  vert.firstToUpper("*=-9"));

    Assert.assertEquals("Some",  vert.firstToUpper("some"));
        
    context.completeNow();
  }

  @Test 
  @Timeout(value = 7, timeUnit = TimeUnit.SECONDS)
  void getCityId_test(Vertx vertx, VertxTestContext context) 
  {  
    MainVerticle vert = new MainVerticle();
    vert.initTree();

    //wrong or null input
    Assert.assertEquals(0,  vert.getCityId("some", "some"));
    Assert.assertEquals(0,  vert.getCityId("", "some"));
    Assert.assertEquals(0,  vert.getCityId("some", null));
    Assert.assertEquals(0,  vert.getCityId(null, "some"));
    Assert.assertEquals(0,  vert.getCityId(null, null));
    Assert.assertEquals(0,  vert.getCityId(null, "IL"));
    Assert.assertEquals(0,  vert.getCityId("Ashqelon", null));
    Assert.assertEquals(0,  vert.getCityId("Haifa", ""));

    //trim test
    Assert.assertEquals(3067696,  vert.getCityId("   Prague ", "CZ    "));
    
    //same city in differnt countries
    Assert.assertEquals(5083330,  vert.getCityId("Berlin", "US"));
    Assert.assertEquals(2950158,  vert.getCityId("Berlin", "DE"));

    Assert.assertEquals(293397,  vert.getCityId("Tel Aviv", "IL"));
    Assert.assertEquals(1609350,  vert.getCityId("bangkok", "th"));
    Assert.assertEquals(1486209,  vert.getCityId("yEkaTerinbUrg", "rU"));       
        
    context.completeNow();
  }

  @Test 
  void average_test(Vertx vertx, VertxTestContext context) 
  {
    MainVerticle vert = new MainVerticle();
    
    ArrayList<Number> av = new ArrayList<Number>();

    Assert.assertEquals(0,  vert.average(av), 0);

    av.add(-9);
    Assert.assertEquals(-9,  vert.average(av), 0);
    av.add(9);
    Assert.assertEquals(0,  vert.average(av), 0);

    context.completeNow();
  }

  @Test  
  void cities_test(Vertx vertx, VertxTestContext context) 
  {
    Checkpoint responsesReceived = context.checkpoint(7);
    WebClient client = WebClient.create(vertx);

    String expected = "";
    String sent = "";
    
    for (int i = 0; i < 7; i++) 
    { 
      switch(i) 
      {
        case 0:
          sent = "cities?country=IL";
          expected ="Tel aviv";
          System.out.println(i);
          break;

        case 1:
          sent = "cities?country=il";
          expected ="Nahariyya";
          System.out.println(i);
          break;

        case 2:
          sent = "cities?country=ru   ";
          expected ="Moscow";
          System.out.println(i);        
          break; 

        case 3:
          sent = "cities?country=0";
          expected ="No country found";
          System.out.println(i);     
         break;

        case 4:
          sent = "cities?";
          expected ="Bad Request"; 
          System.out.println(i);      
          break;

        case 5:
          sent = "cities?country=" + null;
          expected ="No country found";
          System.out.println(i);
          break;

        case 6:
          sent = "cities?country=   aU";
          expected ="Stawell";
          System.out.println(i);
          break;
      }

      final String exp = expected;

      client.get(8080, "localhost", "/" + sent)
        .as(BodyCodec.string())
        .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
          else 
          {
            context.verify(() -> Assert.assertTrue(ar.result().body().toString().contains(exp)));
            responsesReceived.flag();
          }
        }); 
      } 
  }

  @Test  
  void currentforecasts_test(Vertx vertx, VertxTestContext context) 
  {
    Checkpoint responsesReceived = context.checkpoint(7);
    WebClient client = WebClient.create(vertx);

    String expected = "";
    String sent = "";
    
    for (int i = 0; i < 7; i++) 
    { 
      switch(i) 
      {
        case 0:
          sent = "currentforecasts?country=GB&city=Lofthouse";
          expected ="temp";
          System.out.println(i);
          break;

        case 1:
          sent = "currentforecasts?country=GB&city=123";
          expected ="No city found";
          System.out.println(i);
          break;

        case 2:
          sent = "currentforecasts?country=&city=Lofthouse";
          expected ="No city found";
          System.out.println(i);        
          break; 

        case 3:
          sent = "currentforecasts?city=Lofthouse";
          expected ="Bad Request";
          System.out.println(i);     
         break;

        case 4:
          sent = "currentforecasts?country=AF";
          expected ="Bad Request"; 
          System.out.println(i);      
          break;

        case 5:
          sent = "currentforecasts";
          expected ="Bad Request";
          System.out.println(i);
          break;

        case 6:
          sent = "currentforecasts?country=AU&city=" + null;
          expected ="No city found";
          System.out.println(i);
          break;
      }

      final String exp = expected;

      client.get(8080, "localhost", "/" + sent)
        .as(BodyCodec.string())
        .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
          else 
          {
            context.verify(() -> Assert.assertTrue(ar.result().body().toString().contains(exp)));
            responsesReceived.flag();
          }
        }); 
      } 
  }

  @Test  
  void forecasts_test(Vertx vertx, VertxTestContext context) 
  {
    Checkpoint responsesReceived = context.checkpoint(7);
    WebClient client = WebClient.create(vertx);

    String expected = "";
    String sent = "";
    
    for (int i = 0; i < 7; i++) 
    { 
      switch(i) 
      {
        case 0:
          sent = "forecasts?country=GB&city=Lofthouse&days=5";
          expected ="temp";
          System.out.println(i);
          break;

        case 1:
          sent = "forecasts?country=GB&city=Lofthouse";
          expected ="Bad Request";
          System.out.println(i);
          break;

        case 2:
          sent = "forecasts?country=gb&city=london&days=0";
          expected ="Days paramter should be between 1 and 5";
          System.out.println(i);        
          break; 

        case 3:
          sent = "forecasts?country=gb&city=london&days=ru";
          expected ="Bad Request";
          System.out.println(i);     
         break;

        case 4:
          sent = "forecasts?country=&city=&days=3";
          expected ="No city found"; 
          System.out.println(i);      
          break;

        case 5:
          sent = "forecasts?country=dE&city=bErLiN&days=3";
          expected ="forecasts";          
          System.out.println(i);
          break;

        case 6:
          sent = "forecasts?country=AU&city=" + null;
          expected ="No city found";
          System.out.println(i);
          break;
      }

      final String exp = expected;

      client.get(8080, "localhost", "/" + sent)
        .as(BodyCodec.string())
        .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
          else 
          {
            context.verify(() -> Assert.assertTrue(ar.result().body().toString().contains(exp)));
            responsesReceived.flag();
          }
        }); 
      } 
  }

  @Test  
  void forecastsCountLenght_test(Vertx vertx, VertxTestContext context) 
  {  
    Checkpoint responsesReceived = context.checkpoint(5);  
    WebClient client = WebClient.create(vertx);

    int expected = 0;
    String sent = "";

    for (int i = 0; i < 5; i++) 
    { 
      switch(i) 
      {
        case 0:
          sent = "forecasts?country=GB&city=Lofthouse&days=5";
          expected = 5;
          System.out.println(i);
          break;

        case 1:
          sent = "forecasts?country=GB&city=Lofthouse&days=4";
          expected = 4;
          System.out.println(i);
          break;

        case 2:
          sent = "forecasts?country=GB&city=Lofthouse&days=3";
          expected = 3;
          System.out.println(i);        
          break; 

        case 3:
          sent = "forecasts?country=GB&city=Lofthouse&days=2";
          expected = 2;
          System.out.println(i);     
         break;

        case 4:
          sent = "forecasts?country=GB&city=Lofthouse&days=1";
          expected = 1; 
          System.out.println(i);      
          break;
      }

      final int exp = expected;

      client.get(8080, "localhost", "/" + sent)
        .as(BodyCodec.string())
        .send(ar -> { if (ar.failed()){context.failNow(ar.cause());} 
          else 
          {
            context.verify(() -> Assert.assertEquals(exp, StringUtils.countMatches(ar.result().body().toString(), "date")));
            responsesReceived.flag();
          }
        });
    }
  }
  
}
