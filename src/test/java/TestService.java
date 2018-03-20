/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import binding.ProductType;
import binding.World;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static  io.restassured.RestAssured.*;
import static  io.restassured.matcher.RestAssuredMatchers.*;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import java.util.UUID;
import junit.framework.Assert;
import static  org.hamcrest.Matchers.*;

/**
 *
 * @author Nicolas
 */
public class TestService {
    
    String serviceUrl = "http://localhost:8080/adventureISIS/webresources/generic/";
    static String uniqueID;
    
    public TestService() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        // tire au sort un login unique
        uniqueID = UUID.randomUUID().toString();
        System.out.println(uniqueID);
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        
    }
    
    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @TestService. For example:
    //
    @Test
    public void checkXMLValidation() { 
     // teste si le monde retourné au format XML est bien conforme au schéma
     given().header("Accept","application/xml").when().get(serviceUrl+"world").then().assertThat().body(matchesXsd(getSystemResourceAsStream("worldSchema.xsd")));
    }
    
    @Test
    public void checkJSONlogo() { 
     // teste si le monde retourné au format JSON contient bien un world.logo qui désigne une image (jpg, gif, ou png)
     given().header("Accept","application/json").when().get(serviceUrl+"world").then().assertThat().body("logo", anyOf(endsWith(".jpg"), endsWith(".gif"), endsWith(".png")));
    }
    
    @Test
    public void checkSixProducts() {
     // teste si le monde retourné contient bien six produits
      given().header("Accept","application/json").when().get(serviceUrl+"world").then().assertThat().body("products.product", hasSize(6));
    }
    
    @Test
    public void checkThreeUnlocksByProducts() {
        // teste si chaque produit contient au moins 3 unlocks
         given().header("Accept","application/json").when().get(serviceUrl+"world").then().assertThat().body("products.product.findAll { it.palliers.pallier.size()>=3 }", hasSize(6));
    }
    
    @Test
    public void checkAManagerForEachProduct() {
        // teste si il y a autant de managers que de produits
        int nproduct = given().header("Accept","application/json").when().get(serviceUrl+"world").then().extract().path("products.product.size()");
        int nmanager = given().header("Accept","application/json").when().get(serviceUrl+"world").then().extract().path("managers.pallier.size()");
        assertEquals(nproduct,nmanager);
    }
    
    @Test
    public void checkAtLeastOneFirstProduct() {
        // teste si il y a au moins un exemplaire du premier produit
      given().header("Accept","application/json").header("X-User",uniqueID).when().get(serviceUrl+"world").then().assertThat().body("products.product[0].quantite", equalTo(1));
    }
    
    @Test
    public void checkBuyFirstProduct() {
        // teste si le serveur met à jour l'achat d'un produit au niveau de l'argent, et de la quantité
         World w = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        assertEquals(w.getName(),"A Nice World");
    }
    
}
