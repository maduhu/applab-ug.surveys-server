import configuration.*;
import java.util.*;

public class appconfiguration {

	parseXMLFile parse_xml;
	Hashtable<String,String> key_values;
	
	public appconfiguration()
	{
		parse_xml=new parseXMLFile();
		key_values=parse_xml.getApplicationSetting();
	}
	
	public String getZebraUserName()
	{
		return key_values.get("zebra-username");
	}
	
	public String getZebraPassword()
	{
		return key_values.get("zebra-password");
	}
	
	public String getZebraDatabase()
	{
		return key_values.get("zebra-database");
	}
	
	public String getZebraPort()
	{
		return key_values.get("zebra-port");
	}
}
