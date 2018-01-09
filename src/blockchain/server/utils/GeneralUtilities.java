package blockchain.server.utils;

import com.google.gson.Gson;

public class GeneralUtilities {
	private static  Gson gson = new Gson();
	
	public static <T> T deepCopy(T object, Class<T> cls) {
	    return gson.fromJson(gson.toJson(object), cls);
	}
}
