package com.D2C;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sbobba on 1/5/2016.
 */
public class DataStores {

    private static D2CUser D2Cuser = null;

    public DataStores(D2CUser D2Cuser)
    {
        this.D2Cuser = D2Cuser;
    }

    public Map<Integer, String> getDataStores()
    {
        if(D2Cuser == null)
        {
            return null;
        }

        Map<Integer, String> dataStoreList = new HashMap<>();
        String jsonresponse = Utilities.getResponse(Constants.D2CAPIURL + "datastores", D2Cuser);
        JSONObject root = null;
        try
        {
            root = new JSONObject(jsonresponse);
            JSONArray dataStores = root.getJSONArray("dataStores");
            int arrayLen = dataStores.length();
            for(int i=0; i < arrayLen; i++)
            {
                JSONObject datastore = (JSONObject)dataStores.get(i);
                dataStoreList.put(datastore.getInt("id"), datastore.getString("name") );
            }
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "[DataStores]: Exception while parsing JSON" + Constants.ANSI_RESET);
            System.exit(-1);
        }
        return dataStoreList;
    }

}
