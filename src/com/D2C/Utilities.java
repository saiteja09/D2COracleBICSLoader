package com.D2C;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;


/**
 * Created by sbobba on 1/5/2016.
 */
public class Utilities {

    public static String getResponse(String URL, D2CUser D2Cuser)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = null;
        try {
            httpget = new HttpGet(URL);
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "Error: Invalid Characters in URL." + Constants.ANSI_RESET);
            System.exit(-1);
        }

        String creds = D2Cuser.getUserName()+":"+D2Cuser.getPassword();
        byte[] credEncode = Base64.encodeBase64(creds.getBytes());
        httpget.setHeader("Authorization", "Basic " + new String(credEncode));

        HttpResponse response = null;
        try {
            response = httpclient.execute(httpget);
        }
        catch (IOException ex)
        {
            System.out.println(Constants.ANSI_RED + "Error: Unable to execute get request" + Constants.ANSI_RESET);
            System.exit(-1);
        }

        if(response.getStatusLine().getStatusCode() == 401)
        {
            System.out.println(Constants.ANSI_RED + "Error: Invalid DataDirect Cloud Credentials. Please try again." + Constants.ANSI_RESET);
            System.out.print("\n\n");
            System.exit(-1);
        }
        else if(response.getStatusLine().getStatusCode() != 200)
        {
            System.out.println(Constants.ANSI_RED + "Http Error:" + response.getStatusLine().getStatusCode() + " while executing get request" + Constants.ANSI_RESET);
            System.exit(-1);
        }

        String responsedata = null;
        HttpEntity entity = response.getEntity();
        try {
            responsedata = EntityUtils.toString(entity);
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "Exception: " + ex.getMessage() + Constants.ANSI_RESET);
            System.exit(-1);
        }

        return responsedata;
    }

    public static String mapDataTypes(EdmType type)
    {
        String typeName = null;
        try {
            typeName = type.getName();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if(typeName.equals("String"))
        {
            return "VARCHAR2";
        }
        else if (typeName.equals("Int64"))
        {
            return "BIGINT";
        }
        else if (typeName.equals("Binary"))
        {
            return "BINARY";
        }
        else if (typeName.equals("Boolean"))
        {
            return "VARCHAR2";
        }
        else if (typeName.equals("Int64"))
        {
            return "LONG";
        }
        else if(typeName.equals("DateTime"))
        {
            return "TIMESTAMP";
        }
        else if(typeName.equals("Decimal"))
        {
            return "DECIMAL";
        }
        else if(typeName.equals("Double"))
        {
            return "DECIMAL";
        }
        else if(typeName.equals("Int32"))
        {
            return "INT";
        }

        return null;
    }

    public static String readFromConsole()
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String userInput = null;
        try {
            userInput = br.readLine();
        }
        catch (Exception ex)
        {
            System.out.println(Constants.ANSI_RED + "Exception while reading user input" + Constants.ANSI_RESET);
        }
        return userInput;
    }

    public static String putResponse(String URL, BICSUser BICSuser, String json)
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        StringBuilder result = new StringBuilder();
        try
        {


            httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY,
                    CookiePolicy.BROWSER_COMPATIBILITY);
            httpClient.getParams().setParameter("http.protocol.single-cookie-header", true);
            HttpPut putRequest = new HttpPut(URL);
            String creds = BICSuser.getUserName()+":"+BICSuser.getPassword();
            byte[] credEncode = Base64.encodeBase64(creds.getBytes());
            putRequest.setHeader("Content-Type", "application/json");
            putRequest.setHeader("Authorization", "Basic " + new String(credEncode));
            putRequest.setHeader("X-ID-TENANT-NAME",Constants.OrgDomain);

            StringEntity entity = new StringEntity(json);
            putRequest.setEntity(entity);
            HttpResponse response = httpClient.execute(putRequest);
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (response.getEntity().getContent())));

            String output;
            while ((output = br.readLine()) != null) {
                result.append(output);
            }
        }
        catch (Exception ex)
        {
            ex.getMessage();
        }

        return result.toString();
    }

    public static String savetoBICS(JSONObject root, StringBuilder csv, BICSUser BICSuser, String entityName)
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        StringBuilder result = new StringBuilder();
        String cwd = System.getProperty("user.dir");
        if(entityName.length() > 30)
        {
            entityName = entityName.substring(entityName.length() - 30);
        }

        try {
            csv.append(",");



            String creds = BICSuser.getUserName()+":"+BICSuser.getPassword();
            byte[] credEncode = Base64.encodeBase64(creds.getBytes());
            WritetoFile(cwd, root.toString(), Constants.jsonFile);
            WritetoFile(cwd, csv.toString(), Constants.csvFile);


            HttpPut put = new HttpPut(Constants.bicsURL + "tables/" + entityName.toUpperCase() + "/data");
            //put.setHeader("Content-Type","multipart/mixed;boundary=--hkdas9012kjkd21");
            put.setHeader("X-ID-TENANT-NAME",Constants.OrgDomain);
            put.setHeader("Authorization", "Basic " + new String(credEncode));

            HttpEntity entity = org.apache.http.entity.mime.MultipartEntityBuilder
                    .create()
                    .setContentType(org.apache.http.entity.ContentType.create("multipart/mixed"))
                    .addBinaryBody("metadata", new File(cwd + "//" + Constants.jsonFile), org.apache.http.entity.ContentType.create("application/json"), Constants.jsonFile)
                    .addBinaryBody("data", new File(cwd + "//" + Constants.csvFile), org.apache.http.entity.ContentType.create("application/octet-stream"), Constants.csvFile)
                    .build();


            put.setEntity(entity);
            HttpResponse response = httpClient.execute(put);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (response.getEntity().getContent())));

            String output;
            while ((output = br.readLine()) != null) {
                result.append(output);
            }



        }
        catch (Exception ex)
        {
            //ex.printStackTrace();
        }

        return result.toString();
    }

    public static String DeleteTables(BICSUser BICSuser, String entityName)
    {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        StringBuilder result = new StringBuilder();
        try {
            String creds = BICSuser.getUserName()+":"+BICSuser.getPassword();
            byte[] credEncode = Base64.encodeBase64(creds.getBytes());

            HttpDelete del = new HttpDelete(Constants.bicsURL + "tables/" + entityName.toUpperCase());
            del.setHeader("X-ID-TENANT-NAME",Constants.OrgDomain);
            del.setHeader("Authorization", "Basic " + new String(credEncode));

            HttpResponse response = httpClient.execute(del);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (response.getEntity().getContent())));

            String output;
            while ((output = br.readLine()) != null) {
                result.append(output);
            }
        }
        catch (Exception ex)
        {

        }
        return result.toString();
    }

    public static void DeleteAllTables(ArrayList<String> entities, BICSUser BICSuser )
    {
        System.out.println("Do you want to delete existing tables with same name on Oracle Business Intelligence Cloud if any? (Yes/No)");
        String input = readFromConsole();
        if(input.equalsIgnoreCase("yes")) {
            for (String entity : entities) {
                System.out.println(DeleteTables(BICSuser, entity));
            }
        }
        else if (input.equalsIgnoreCase("no"))
        {
            return;
        }
        else
        {
            DeleteAllTables(entities, BICSuser);
        }
    }

    private static void WritetoFile(String cwd, String data, String fileName) throws IOException {
        FileWriter file = new FileWriter(cwd + "//" + fileName, false);
        try {
            file.write(data);

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        finally {
            file.flush();
            file.close();
        }
    }

    public static String compressString(String srcTxt)
            throws IOException {
        ByteArrayOutputStream rstBao = new ByteArrayOutputStream();
        GZIPOutputStream zos = new GZIPOutputStream(rstBao);
        zos.write(srcTxt.getBytes());
        zos.close();
        byte[] bytes = rstBao.toByteArray();
        return Base64.encodeBase64String(bytes);
    }

    public static void printResults(int insertedRecords, int failedRows)
    {
            String status = null;
            if(insertedRecords > 0 && failedRows == 0)
            {
                status = "Completed";
            }
            else if (insertedRecords > 0 && failedRows > 0)
            {
                status = "Partial failure";
            }
            else
            {
                status = "Failed";
            }

            System.out.println("Status       : " + status);
            System.out.println("Inserted Rows: " + insertedRecords);
            System.out.println("Failed Rows  : " + failedRows);
    }

    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b){
            return;
        }
        @Override
        public void write(byte[] b){
            return;
        }
        @Override
        public void write(byte[] b, int off, int len){
            return;
        }
        public NullOutputStream(){
        }
    }

    public static String getSqlTypeName(int type) {
        switch (type) {
            case Types.BIT:
                return "VARCHAR2";

            case Types.TINYINT:
                return "INT";

            case Types.SMALLINT:
                return "INT";

            case Types.INTEGER:
                return "INT";

            case Types.BIGINT:
                return "DECIMAL";

            case Types.FLOAT:
                return "DECIMAL";

            case Types.REAL:
                return "DECIMAL";

            case Types.DOUBLE:
                return "DECIMAL";

            case Types.NUMERIC:
                return "DECIMAL";

            case Types.DECIMAL:
                return "DECIMAL";

            case Types.CHAR:
                return "VARCHAR2";

            case Types.VARCHAR:
                return "VARCHAR2";

            case Types.LONGVARCHAR:
                return "CLOB";

            case Types.DATE:
                return "TIMESTAMP";

            case Types.TIME:
                return "VARCHAR2";

            case Types.TIMESTAMP:
                return "TIMESTAMP";

            case Types.BINARY:
                return "VARCHAR2";

            case Types.VARBINARY:
                return "VARCHAR2";


        }
        return  null;
    }

    public static ArrayList<String> getPrimaryKey(String entityName, Connection con)
    {
        ArrayList<String> PKS = new ArrayList<>();
        try {
            DatabaseMetaData meta = con.getMetaData();
            ResultSet pKs = meta.getPrimaryKeys(null,null,entityName.toUpperCase());
            while(pKs.next())
            {
                PKS.add(pKs.getString("COLUMN_NAME").toUpperCase());
            }
        }
        catch (SQLException ex)
        {
            System.out.println(Constants.ANSI_RED + "Error while Fetching Primary Keys for Entity" + entityName + Constants.ANSI_RESET);
        }
        return  PKS;
    }

    public static String getCustumQueryforDataStore(int selectedDataStore, String entityName, String schema)
    {
        String query = null;
        int top = Constants.MAX_BATCH_SIZE;
        if(selectedDataStore == 1)
        {
            query = "SELECT TOP " + top + " * FROM "+schema+".\"" + entityName + "\"";
        }
        else if(selectedDataStore == 41)
        {
            query = "SELECT  * FROM "+schema+ "." + entityName + " FETCH FIRST " + top + " ROWS ONLY";
        }
        else if(selectedDataStore == 42)
        {
            query = "SELECT * FROM "+schema+".`" + entityName + "` LIMIT " + top;
        }
        else if(selectedDataStore == 43)
        {
            query = "SELECT * FROM "+schema+".\"" + entityName + "\" WHERE ROWNUM <= " + top;
        }
        else if(selectedDataStore == 44)
        {
            query = "SELECT * FROM "+schema+".\"" + entityName + "\" LIMIT " + top;
        }
        else if(selectedDataStore == 46)
        {
            query = "SELECT Top " + top + " * FROM "+schema+".`" + entityName + "`";
        }
        else if(selectedDataStore == 52)
        {
            query = "SELECT FIRST " + top + " * FROM "+schema+"." + entityName + "";
        }
        else
        {
            query = "SELECT Top " + top + " * FROM "+schema+"." + entityName + "";
        }

        return query;
    }

}