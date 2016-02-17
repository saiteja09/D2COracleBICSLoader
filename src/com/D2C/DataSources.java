package com.D2C;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sbobba on 1/5/2016.
 */
public class DataSources {

    private static D2CUser D2Cuser = null;

    public DataSources(D2CUser D2Cuser)
    {
        this.D2Cuser = D2Cuser;
    }

    public Map<Integer, String> getDataSources()
    {
        if(D2Cuser == null)
        {
            return null;
        }

        Map<Integer, String> dataSourceList = new HashMap<>();
        String jsonresponse = Utilities.getResponse(Constants.D2CAPIURL + "datasources", D2Cuser);
        JSONObject root = null;
        try
        {
            root = new JSONObject(jsonresponse);
            JSONArray dataSources = root.getJSONArray("dataSources");
            int arrayLen = dataSources.length();
            for(int i=0; i < arrayLen; i++)
            {
                JSONObject datasource = (JSONObject)dataSources.get(i);
                dataSourceList.put(datasource.getInt("dataStore"), datasource.getString("name") );
            }
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "[DataSources]: Exception while parsing JSON" + Constants.ANSI_RESET);
            System.exit(-1);
        }
        return dataSourceList;
    }


}
