package com.D2C;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.net.URI;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static D2CUser D2Cuser = null;
    private static BICSUser BICSuser = null;
    private static Map<Integer, String> dataStoreList = null;
    private static Map<Integer, String> dataSourcesList = null;
    private static Connection con = null;


    public static void main(String[] args) {

        Logger logger = Logger.getLogger("org.apache.http");
        logger.setLevel(Level.SEVERE);
        int selectedDataSourceId = -1;
        System.out.println("DataDirect Cloud to Oracle BICS Data Loader");
        System.out.println("===========================================");


        LoginorRegisterD2C();
        DataStores dtstore = new DataStores(D2Cuser);
        DataSources dtsource = new DataSources(D2Cuser);
        dataStoreList = dtstore.getDataStores();
        dataSourcesList = dtsource.getDataSources();
        getBICSUserInfo();
        getBICSURL();
        selectedDataSourceId = getSelectedDataSource();
        String dataSourceName =  dataSourcesList.get(selectedDataSourceId);
        JDBCConnection jdbcConnection = new JDBCConnection(D2Cuser,dataSourceName);
        con = jdbcConnection.CreateConnection();
        Map<String, String> entitys = getTablesListfromD2C();
        ArrayList<String> keys = new ArrayList(entitys.keySet());
        Collections.sort(keys);
        int entityId = chooseEntityAction(keys);
        Utilities.DeleteAllTables(keys,BICSuser);
        if(entityId != -2)
        {
            String entityName = keys.get(entityId);
            String schema = entitys.get(entityName);
            String repeated = new String(new char[(60 - entityName.length())/2]).replace("\0", "=");

            System.out.println(repeated + entityName + repeated);
            System.out.println("Fetching Schema for " + entityName + " from DataDirect Cloud");
            Map<String, String> columnMetadata = buildSchema(entityName, con);
             //columnMetadata= fetchSchema(entityName, dataSourceName);
            getDataFromCloud(entityName, con, columnMetadata, selectedDataSourceId, schema);
            System.out.println("============================================================");
        }
        else
        {
            for(String entity: keys)
            {
                String schema = entitys.get(entity);
                String repeated = new String(new char[Math.abs(60 - entity.length())/2]).replace("\0", "=");
                System.out.println(repeated + entity + repeated);
                System.out.println("Fetching Schema for " + entity + " from DataDirect Cloud");
                Map<String, String> columnMetadata = buildSchema(entity, con);
                //Map<String, String> columnMetadata= fetchSchema(entity, dataSourceName);
                getDataFromCloud(entity, con, columnMetadata, selectedDataSourceId, schema);
                //System.out.println("============================================================");
            }
        }
        try {
            con.close();
        }
        catch (SQLException ex)
        {
            System.out.println(Constants.ANSI_RED + "Exception while closing SQL Connection" + Constants.ANSI_RESET);
        }
    }

    private static void getBICSUserInfo()
    {
        System.out.println("Enter your Oracle Business Intelligence Cloud Credentials:\n");
        String user = null;
        System.out.println("Enter your Oracle BICS UserName");
        user = Utilities.readFromConsole();
        System.out.println("\nEnter your Oracle BICS Password");
        //String pass = new String(System.console().readPassword());
        String pass = Utilities.readFromConsole();
        BICSuser = new BICSUser(user, pass);
        System.out.println("===========================================");
    }

    protected static void LoginorRegisterD2C()
    {

        String userInput = null;
        System.out.println("Choose your action:");
        System.out.println("1. Login into DataDirectCloud [Existing Customers]");
        System.out.println("2. Register for DataDirectCloud [New Customers]");
        userInput = Utilities.readFromConsole();
        System.out.println("===========================================");
        if(userInput.equals("1"))
        {
            String user = null;

            System.out.println("Enter your DataDirect Cloud UserName");
            user = Utilities.readFromConsole();
            System.out.println("\nEnter your DataDirect Cloud Password");
            String pass = Utilities.readFromConsole();
            //String pass = new String(System.console().readPassword());
            D2Cuser = new D2CUser(user, pass);
            System.out.println("===========================================");
        }
        else if (userInput.equals("2"))
        {
            try {
                Desktop.getDesktop().browse(new URI("https://pacific.progress.com/console/register?productName=d2c&ignoreCookie=true"));
                System.out.println(Constants.ANSI_GREEN + "Hi there! \nThank you for choosing DataDirect Cloud. \nPlease wait while we open the Registration page for DataDirect Cloud on your default browser.." + Constants.ANSI_RESET);
                System.out.println("===========================================");
                System.exit(0);
            }
            catch (Exception ex)
            {
                System.out.println(Constants.ANSI_RED + "Error while opening URL" + Constants.ANSI_RESET);
            }

        }
        else
        {
            System.out.println(Constants.ANSI_RED + "Invalid Input. Try again..." + Constants.ANSI_RESET);
            LoginorRegisterD2C();
        }

    }

    private static Map<String, String> getTablesListfromD2C()
    {
        Map<String, String> entities = new HashMap<>();
        String[] type = new String[1];
        type[0] = "TABLE";
        try {
            DatabaseMetaData meta = con.getMetaData();
            ResultSet rsTables = meta.getTables(null, null, "%", type);
            while (rsTables.next()) {
                entities.put(rsTables.getString(3), (rsTables.getString(2)== null)?rsTables.getString(1):rsTables.getString(2));
            }
        }
        catch (SQLException ex)
        {

        }
        return entities;
    }

    private static int getSelectedDataSource()
    {
        System.out.println("Choose your DataDirect Cloud Data source\n");

        System.out.format("%15s%20s\n","DataSourceId","DataSourceName");

        for(Map.Entry<Integer,String> dtsources : dataSourcesList.entrySet())
        {
            System.out.format("%15s%20s\n", dtsources.getKey().toString(), dtsources.getValue().toString());
        }

        System.out.println("\nEnter DataDirect Cloud DataSourceId:");
        String userInput = Utilities.readFromConsole();
        int key = -1;
        try{
            key = Integer.parseInt(userInput);
        }
        catch(NumberFormatException ex)
        {
            System.out.println(Constants.ANSI_RED + "Invalid Input. Please try again." + Constants.ANSI_RESET);
            key =getSelectedDataSource();
            System.out.println("===========================================");
            return key;
        }

        if(dataSourcesList.containsKey(key))
        {
            System.out.println("===========================================");
            return key;
        }
        else
        {
            System.out.println(Constants.ANSI_RED + "Invalid Input. Please try again." + Constants.ANSI_RESET);
            key =getSelectedDataSource();
            System.out.println("===========================================");
            return key;
        }
    }

    private static int chooseEntityAction(ArrayList<String> entities)
    {
        System.out.println("Choose next action:");
        System.out.println("1. Upload a single entity");
        System.out.println("2. Upload all entities");
        String ch = Utilities.readFromConsole();
        int entityId = -1;
        if(ch.equals("1"))
        {
            System.out.println("===========================================");
            entityId = chooseEntity(entities);
            return entityId;
        }
        else if(ch.equals("2"))
        {
            System.out.println("===========================================");
            entityId = -2;
            return entityId;
        }
        else
        {
            entityId = chooseEntityAction(entities);
            return entityId;
        }

    }

    private static int chooseEntity(ArrayList<String> entities)
    {
        int evenflag = -1;
        System.out.println("Available Entities on selected DataSource:");
        System.out.format("%5s%30s%5s%5s%30s\n","EntityId","EntityName","     ","EntityId","EntityName");

        int len = entities.size();
        if(len%2 == 0) {
            evenflag = 0;
        }
        else
        {
            len--;
            evenflag = 1;
        }

        for(int i = 0; i < len; i = i + 2)
        {
            System.out.format("%5s%30s%5s%5s%30s\n",i+1,entities.get(i),"     ",i+2,entities.get(i+1));
        }

        if(evenflag == 1)
        {
            System.out.format("%5d%30s", entities.size(), entities.get(entities.size()-1));
        }

        System.out.println("\n\nChoose an EntityId you wish to upload to Oracle BICS:");
        String userInput = Utilities.readFromConsole();

        int key = -1;
        try{
            key = Integer.parseInt(userInput);
        }
        catch(NumberFormatException ex)
        {
            System.out.println(Constants.ANSI_RED + "Invalid Input. Please try again." + Constants.ANSI_RESET);
            key =chooseEntity(entities);
            System.out.println("===========================================");
            return key;
        }

        if(key > 0 && key <= entities.size())
        {
            System.out.println("===========================================");
            return key-1;
        }
        else
        {
            System.out.println(Constants.ANSI_RED + "Invalid Input. Please try again." + Constants.ANSI_RESET);
            key =chooseEntity(entities);
            System.out.println("===========================================");
            return key;
        }

    }

    private static Map<String, String> buildSchema(String entityName, Connection con)
    {
        Map<String, String> columnMetadata = new HashMap<>();
        JSONArray root = new JSONArray();
        try{
            DatabaseMetaData metaData = con.getMetaData();
            ResultSet rsColumnmetadata = metaData.getColumns(null,null,entityName,"%");
            while(rsColumnmetadata.next())
            {
                JSONObject propertyJSON = new JSONObject();
                int length = rsColumnmetadata.getInt(7);
                if(rsColumnmetadata.getInt(5) == Types.TIME)
                {
                    length = 15;
                }
                String typeName = Utilities.getSqlTypeName(rsColumnmetadata.getInt(5));
                typeName = (typeName != null )?typeName:"VARCHAR2";
                String propertyName = rsColumnmetadata.getString(4) + "S";

                if((length == 1 && rsColumnmetadata.getInt(5) == 16))
                    length = 10;
                if(typeName.equals("VARCHAR2") && length < 5)
                    length =10;

                String defaultValue = rsColumnmetadata.getString(13);
                Boolean isNullable = (rsColumnmetadata.getInt(11) == 1)?true:false;
                int precision = rsColumnmetadata.getInt(10);
                if(typeName.equals("TIMESTAMP") || typeName.equals("DATE") || typeName.equals("INTEGER") ||typeName.equals("DECIMAL"))
                {
                    length = 0;
                    precision = 0;
                }
                else if(typeName.equals("VARCHAR2"))
                {
                    if(length > 4000)
                    {
                        typeName = "CLOB";
                    }
                }

                if(propertyName.length() > 30)
                {
                    propertyName = propertyName.substring(propertyName.length()-30);
                }

                propertyJSON.append("columnName", propertyName);
                propertyJSON.append("length", length);
                propertyJSON.append("dataType",typeName);
                propertyJSON.append("defaultValue", defaultValue);
                propertyJSON.append("nullable", isNullable);
                propertyJSON.append("precision", precision);
                columnMetadata.put(rsColumnmetadata.getString(4), typeName);
                root.put(propertyJSON);
            }
        }
        catch(SQLException ex)
        {

        }
        catch (JSONException ex)
        {

        }
        String temp = root.toString().replaceAll("\\[", "");
        String temp1 = temp.toString().replaceAll("\\]", "");
        temp = "[" + temp1 + "]";

        if(entityName.length() > 30)
        {
            entityName = entityName.substring(entityName.length() - 30);
        }
        String op = Utilities.putResponse(Constants.bicsURL + "tables/" +entityName, BICSuser, temp);
        System.out.println(op);
        return columnMetadata;
    }
    private static void getDataFromCloud(String entityName, Connection con, Map<String, String> columnMetaData, int dataSourceID, String schema)
    {
        try
        {
            ResultSet rs = null;
            Statement stmt = con.createStatement();
            rs = stmt.executeQuery(Utilities.getCustumQueryforDataStore(dataSourceID, entityName, schema));

            JSONObject root = new JSONObject();
            JSONArray columnMaps = new JSONArray();
            StringBuilder csv = new StringBuilder();
            int position = 1;
            ArrayList<String> primaryKeys = Utilities.getPrimaryKey(entityName, con);
            int successRowCount = 0, batchCount = 0, failureRowCount = 0;
            while (rs.next()) {
                int flag = 1;
                if(position > 1)
                {
                    flag = -1;
                }

                for(Map.Entry<String, String> col : columnMetaData.entrySet())
                {
                    if(flag == 1) {
                        buildJSONForDataPut(position, columnMaps, col, primaryKeys);
                        position++;
                    }

                    buildCSV(col, csv, rs);

                }
                int index = csv.lastIndexOf(",");
                csv.replace(index,index+1, "\n");
                batchCount++;
                if(batchCount > Constants.SUG_BATCH_SIZE)
                {
                    ArrayList<Integer> recordData = sendDataRequest(entityName, root, columnMaps, csv, position);
                    if(recordData.size() != 0) {
                        successRowCount = successRowCount + recordData.get(0);
                        failureRowCount = failureRowCount + recordData.get(1);
                    }
                    batchCount = 0;
                    csv = new StringBuilder();
                }

            }

                ArrayList<Integer> recordData = sendDataRequest(entityName, root, columnMaps, csv, position);
                if(recordData.size() != 0) {
                    successRowCount = successRowCount + recordData.get(0);
                    failureRowCount = failureRowCount + recordData.get(1);
                    Utilities.printResults(successRowCount, failureRowCount);
                }


        }
        catch (Exception e) {

            if(e.getMessage().contains("filter") || e.getMessage().contains("unsupported"))
            {
                System.out.println(Constants.ANSI_RED + "Cannot Fetch Data in Bulk for Table " + entityName +  Constants.ANSI_RESET);
            }else if(e.getMessage().contains("permission") || e.getMessage().contains("PRIVILEGE"))
            {
                System.out.println(Constants.ANSI_RED + "Access denied for Entity " + entityName + ". Contact your Administrator" +  Constants.ANSI_RESET);
            }
            else
            {
                System.out.println(Constants.ANSI_RED +  e.getMessage() +  Constants.ANSI_RESET);
            }
        }
    }

    public static ArrayList<Integer> sendDataRequest(String entityName, JSONObject root, JSONArray columnMaps, StringBuilder csv, int position) throws JSONException {
        ArrayList<Integer> record_data = new ArrayList<>();
        root.put("columnMaps", columnMaps);
        root.put("optionalMaximumErrors", 10);
        root.put("removeDuplicates", true);
        root.put("optionalWriteMode", "Insert all");
        root.put("delimiter",",");
        root.put("timestampFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        root.put("numberOfLinesToSkip", 0);

        if(position != 1) {
            String resultJSON = Utilities.savetoBICS(root, csv, BICSuser, entityName);
            try {
                JSONObject json = new JSONObject(resultJSON);
                record_data.add(0, json.getInt("insertedRows"));
                record_data.add(1, json.getInt("failedRows"));
            }
            catch (JSONException ex)
            {
                if(resultJSON.contains("Unique")) {
                    System.out.println(Constants.ANSI_RED + "No Unique Column defined for the Entity " + entityName + Constants.ANSI_RESET );
                }
                else
                {
                    System.out.println(Constants.ANSI_RED + resultJSON + Constants.ANSI_RESET);
                }
            }
        }
        else
        {
            System.out.println(Constants.ANSI_GREEN + "No Records found for Entity " + entityName + Constants.ANSI_RESET);
        }

        return record_data;
    }

    private static void buildJSONForDataPut(int position, JSONArray columnMaps, Map.Entry<String, String> col, ArrayList<String> primaryKeys)
    {
        try {
            JSONObject columnroot = new JSONObject();
            JSONObject column = new JSONObject();
            String propertyName = col.getKey().toUpperCase() + "S";
            if(propertyName.length() > 30)
            {
                propertyName = propertyName.substring(propertyName.length()-30);
            }
            column.put("name", propertyName);
            if(primaryKeys.contains(col.getKey().toUpperCase())) {
                column.put("partOfUniqueKey", true);
            }
            else
            {
                column.put("partOfUniqueKey", false);
            }

            columnroot.put("column", column);
            columnroot.put("position", position);

            columnMaps.put(columnroot);
        }
        catch (Exception ex)
        {

        }
    }

    private static void buildCSV(Map.Entry<String, String> col, StringBuilder csv, ResultSet rs) {
        try {
            Object data = rs.getObject(col.getKey());
            if ((col.getValue().equals("VARCHAR2") || col.getValue().equals("CLOB"))) {

                if (data == null)
                {
                    csv = csv.append("null");
                }
                else
                {
                    csv = csv.append("\"" +  data.toString() + "\"" );
                }
            }
            else if(col.getValue().equals("TIMESTAMP") || col.getValue().equals("DATE"))
            {


                if(data != null)
                {
                    if(data.toString().length() == 10)
                    {
                        data = (Object)(data.toString() + " 00:00:00.0");
                    }
                    csv = csv.append("\"" +  data.toString() + "\"" );
                }

            }
            else
            {
                if(data == null)
                {
                    csv = csv.append("0");
                }
                else {
                    csv = csv.append(data.toString());
                }
            }

            csv.append(",");
        }
        catch (SQLException ex)
        {
            //ex.printStackTrace();
            csv.append(",");
        }
    }

    private static void getBICSURL()
    {
        System.out.println("Provide your Unique Oracle BICS URL below:");
        String URL = Utilities.readFromConsole();
        URL = URL + "/dataload/v1/";
        Constants.bicsURL = URL;
        int hiphenIndex = URL.indexOf("-");
        int dotIndex = URL.indexOf(".");
        Constants.OrgDomain = URL.substring(hiphenIndex+1, dotIndex);
        System.out.println("===========================================\n");
    }

    //private static String xml_Schema = null;

     /*private static ArrayList<String> getTablesListfromD2C(int selectedSourceId)
    {
        ArrayList<String> entities = new ArrayList<>();
        String selectedSource = dataSourcesList.get(selectedSourceId);
        if(xml_Schema == null)
            xml_Schema = Utilities.getResponse(Constants.D2CODATAURL + selectedSource + "/$metadata", D2Cuser);
        //String jsonResponse = Utilities.getResponse(Constants.D2CODATAURL + selectedSource +"?$format=json", D2Cuser);
        try {
            InputStream stream = new ByteArrayInputStream(xml_Schema.getBytes(StandardCharsets.UTF_8));
            Edm edm = EntityProvider.readMetadata(stream, false);
            /*JSONObject root = new JSONObject(jsonResponse);
            JSONObject d = root.getJSONObject("d");
            JSONArray entitySets = d.getJSONArray("EntitySets");*/
           /* for(int i =0 ; i< edm.getEntitySets().size(); i++)
            {
                entities.add(i, edm.getEntitySets().get(i).getEntityType().getName());
            }
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "Error while parsing JSON" + Constants.ANSI_RESET);
        }
        return  entities;
    }*/

   /* private static Map<String, String> fetchSchema(String entityName, String dataSourceName)
    {
        if(xml_Schema == null)
            xml_Schema = Utilities.getResponse(Constants.D2CODATAURL + dataSourceName + "/$metadata", D2Cuser);
        Map<String, String> columnMetadata = new HashMap<>();
        JSONArray root = new JSONArray();
        try {
            InputStream stream = new ByteArrayInputStream(xml_Schema.getBytes(StandardCharsets.UTF_8));
            Edm edm = EntityProvider.readMetadata(stream, false);
            EdmEntityType selectedEntity = edm.getEntityType(dataSourceName, entityName);
            java.util.List<String> propertyNames = selectedEntity.getPropertyNames();

            for(int i = 0; i < propertyNames.size(); i++)
            {
                EdmTyped property = selectedEntity.getProperty(propertyNames.get(i));
                EdmFacets facets = ((EdmSimplePropertyImplProv)property).getFacets();
                JSONObject propertyJSON = new JSONObject();
                String typeName = Utilities.mapDataTypes(property.getType());
                String propertyName = propertyNames.get(i) + "S";
                if(propertyName.length() > 30)
                {
                    propertyName = propertyName.substring(propertyName.length()-30);
                }
                typeName = (typeName != null )?typeName:"VARCHAR2";
                propertyJSON.append("columnName", propertyName);

                propertyJSON.append("dataType",typeName);
                columnMetadata.put(propertyNames.get(i),typeName);
                if(!property.getType().getName().equals("Boolean")) {
                    int len = (facets.getMaxLength() == null) ? 0 : facets.getMaxLength();
                    if(len > 4000)
                    {
                        propertyJSON.put("dataType","CLOB");
                        columnMetadata.put(propertyNames.get(i),"CLOB");
                    }
                    if(typeName.equals("VARCHAR2") && len==0)
                    {
                        len = 50;
                    }
                    propertyJSON.append("length", len);
                }
                else
                {
                    propertyJSON.append("length", 10);
                }
                propertyJSON.append("precision", (facets.getPrecision() == null)?0: facets.getPrecision() );
                propertyJSON.append("nullable", facets.isNullable());
                propertyJSON.append("defaultValue", facets.getDefaultValue());
                root.put(propertyJSON);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String temp = root.toString().replaceAll("\\[", "");
        String temp1 = temp.toString().replaceAll("\\]", "");
        temp = "[" + temp1 + "]";

        if(entityName.length() > 30)
        {
            entityName = entityName.substring(entityName.length() - 30);
        }

        String op = Utilities.putResponse(Constants.bicsURL + "tables/" +entityName, BICSuser, temp);
        System.out.println(op);
        return columnMetadata;
    }*/




}
