package Tests;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import binding.PallierType;
import binding.ProductType;
import binding.TyperatioType;
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
    
    String serviceUrl = "http://localhost:8080/AdventureISIS/webresources/generic/";
    String uniqueID;
    
    public TestService() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        // tire au sort un login unique
      
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        uniqueID = UUID.randomUUID().toString();
        System.out.println(uniqueID);
        
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
    public void checkLauchProductionFirstProduct() throws InterruptedException {
        // teste la bonne mise en production du premier produit 
        World w = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        ProductType p = w.getProducts().getProduct().get(0);
        given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
        // on attend un peu plus que le temps de production
        Thread.sleep(p.getVitesse()+100);
        // on recharge le monde
        World w2 = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        // on vérifie qu'on a bien gagné l'argent attendu à 0.1 de précision pret
        assertEquals(w2.getMoney(), w.getMoney()+p.getRevenu() * p.getQuantite(),0.1);
    }
    
    @Test
    public void checkLauchAndBuyProduct() throws InterruptedException {
        // teste l'achat du premier produit avec accumulation d'assez d'argent pour l'acheter 
        World w = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        ProductType p = w.getProducts().getProduct().get(0);
        while (w.getMoney() < p.getCout()) {
            // production d'un exemplaire
            given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
            // on attend un peu plus que le temps de production
            Thread.sleep(p.getVitesse()+100);
            // on met à jour l'argent gagné 
            w.setMoney(w.getMoney()+p.getRevenu());
        }
        // on achete un exemplaire du produit
        p.setQuantite(p.getQuantite()+1);
        given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
        // on recharge le monde
        World w2 = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        // on vérifie qu'on a bien dépensé l'argent pour achat et qu'on a un produit en plus (donc deux)
        assertEquals(w2.getMoney(), w.getMoney()-p.getCout(),0.1);
        ProductType p2 = w2.getProducts().getProduct().get(0);
        assertEquals(p2.getQuantite(), 2);
        // on vérifie aussi que le cout du produit a bien été mis à jour
        assertEquals(p2.getCout(), p.getCout() *  p.getCroissance(), 0.1);
    }
    
    
    @Test
    public void checkUnlockFirstProduct() throws InterruptedException {
        // teste l'achat de plusieurs exemplaires du premier produit avec l'activation de l'unlock et sa bonne prise en compte sur les revenus 
        World w = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        ProductType p = w.getProducts().getProduct().get(0);
        PallierType unlock = p.getPalliers().getPallier().get(0);
        while (p.getQuantite() < unlock.getSeuil()) {
            // production d'un exemplaire
            given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
            // on attend un peu plus que le temps de production
            Thread.sleep(p.getVitesse()+100);
            // on met à jour l'argent gagné 
            w.setMoney(w.getMoney()+p.getRevenu()*p.getQuantite());
            System.out.println("money after prod:"+w.getMoney());
            // si possible on achete des exemplaire supplémentaire
            while (w.getMoney() >= p.getCout()) {
                  p.setQuantite(p.getQuantite()+1);
                  given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
                  w.setMoney(w.getMoney()-p.getCout());
                  p.setCout(p.getCout()*p.getCroissance());
                  System.out.println("money after buy:"+w.getMoney());
                
            }
        }
        // si on est là c'est que l'unlock doit être débloqué, on vérifie : 
        World w2 = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        ProductType p2 = w2.getProducts().getProduct().get(0);
        // on teste la bonne prise en compte du unlock
        assertEquals(p2.getPalliers().getPallier().get(0).isUnlocked(), true);
        if (unlock.getTyperatio() == TyperatioType.VITESSE) {
            p.setVitesse((int) (p.getVitesse() / unlock.getRatio()));
            assertEquals(p2.getVitesse(), p.getVitesse());
        }
        else  {
            p.setRevenu(p.getRevenu() * unlock.getRatio());
            assertEquals(p.getRevenu(), p2.getRevenu(),0.1);
        }
        
        // on teste la prochaine production pour voir si le serveur calcule toujours bien
        given().contentType("application/json").header("X-User",uniqueID).body(p).put(serviceUrl+"product");
        // on attend un peu plus que le temps de production
        Thread.sleep(p.getVitesse()+100);
        // on recharge le monde
        World w3 = given().header("Accept","application/json").header("X-User",uniqueID).get(serviceUrl+"world").as(World.class);
        // on vérifie qu'on a bien gagné l'argent attendu à 0.1 de précision pret
        assertEquals(w3.getMoney(), w.getMoney()+p.getRevenu() * p.getQuantite(),0.1);
       
        
    }
}
