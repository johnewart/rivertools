package net.johnewart.rivertools.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: jewart
 * Date: 12/29/12
 * Time: 9:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class RiverSimModelFactory {
    public static Object loadObject(long id, String resource, Class klass)
    {
         try {
             String resourceUrl = "http://localhost:8000/riversim/api/v1/" + resource + "/" + id  + "/?format=json";
             HttpClient httpClient = new DefaultHttpClient();
             HttpGet getRequest = new HttpGet(resourceUrl);

             getRequest.addHeader("accept", "application/json");

             HttpResponse response = httpClient.execute(getRequest);

             if (response.getStatusLine().getStatusCode() != 200) {
                 throw new RuntimeException("Failed : HTTP error code : "
                         + response.getStatusLine().getStatusCode());
             }

             BufferedReader br = new BufferedReader(
                     new InputStreamReader((response.getEntity().getContent())));

             String jsonData = "";
             String line;
             while ((line = br.readLine()) != null) {
                 jsonData += line;
             }

             httpClient.getConnectionManager().shutdown();

             ObjectMapper objectMapper = new ObjectMapper();
             return objectMapper.readValue(jsonData, klass);

         } catch (ClientProtocolException e) {

             e.printStackTrace();

         } catch (IOException e) {

             e.printStackTrace();
         }

         return null;
    }
}
