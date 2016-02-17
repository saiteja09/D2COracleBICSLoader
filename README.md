# D2COracleBICSLoader
Upload data from various datasources(SaaS and On-premise) to Oracle BI Cloud Service powered by DataDirect Cloud

####Instructions to use the micro app

1.	Run the Oracle BI Cloud Loader application using the command java –jar D2COracleBICSLoader.jar
2. Enter your DataDirect credentials and Oracle BICS credentials and Oracle BICS endpoint URL when prompted. The endpoint URL will be similar to following pattern: 
https://businessintell- [orgdomain] .analytics.[region].oraclecloud.com

3.	You will be shown different data Sources that you have defined in your DataDirect Cloud account, Select one and choose to either upload data for one entity or all the entities in the schema.
4.	You can see the progress as the tool uploads data for each entity on screen and when it is done you can check Oracle Business Intelligence db to find all of your data there.
