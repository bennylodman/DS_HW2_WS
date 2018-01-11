package blockchain.server.utils;

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GeneralUtilities {
	private static  Gson gson = new Gson();
	
	public static <T> T deepCopy(T object, Class<T> cls) {
	    return gson.fromJson(gson.toJson(object), cls);
	}
	
	public static JsonObject readJsonFile(String fileName){

        JsonObject jsonObject = new JsonObject();
        
        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(new FileReader(fileName));
            jsonObject = jsonElement.getAsJsonObject();
        } catch (IOException e) {
        	System.out.println("failed to read server-config file");
        }
        
        return jsonObject;
    }
}
