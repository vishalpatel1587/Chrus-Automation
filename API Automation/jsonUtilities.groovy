/**
* The jsonUtilities library contains a set of tools for dealing with JSON strings as required for the SCG project.
*
* --------------------------------------- 
* EXPLANATION: Closure
* A closure is in essence a code snippet set as a variable, this allows for method calls to be sent as
* as parameters. In the context of this project we are using them to perform jsonPath and GPath expressions
* NOTE: Closures are similar to a Java specific lambda expression and are a relatively advanced programming
* concept. If understanding around their design and use is unclear, simply think of them as a way of performing
* pathing expression (jsonPath/GPath) in the context of this project.
* ---------------------------------------
*
* @author Nic Golding
* @version 0.1
* @since 2017-07-06
*/

package readyapi.jsonUtilities

class JSONUtilities {

	/**
	* The removeCDATA method is used to remove any reference to CDATA tags from an 
	* incoming string. Depending on the parser these CDATA tags can cause issues with 
	* retrieving data values. The ReadyAPI and SOAPUI parsers tend to have intermittent 
	* issues with CDATA.
	*
	* @param json - The JSON response string from which the CDATA tags will be removed
	* @return String - This returns the json String parameter without CDATA tags
	*/
	public String removeCDATA(String json)
	{
		String tempJSON = json.replaceAll("<!\\[CDATA\\[","").replaceAll("\\]\\]>","");
		return tempJSON;
	}

	/**
	* The compareString method is a direct string comparison method which provides 
	* error message generation depending on match results. 
	* 
	* @param firstString - The first string to compare
	* @param secondString - The second string to compare
	* @param validationCheck - This map is used throughout all checks in the jsonUtilities 
	* library as a way of storing error message, count and log information. It is used 
	* in the associated groovy scripts to details reporting functionality on failures.
	* @return java.util.Map - The error log containing the results of the check. If no errors
	* the map will be the same as that which was provided to the method.
	*/
	public java.util.Map compareStrings(String firstString, String secondString, java.util.Map validationCheck)
	{
		int errorCount = validationCheck.count
		String errorLog = validationCheck.message
		String errorMessage
		
		//Check if the provided strings match. If the dont, generate an error message.
		if (firstString != secondString)
		{
			errorMessage = "First input \"$firstString\" does not match second input \"$secondString\""
			errorCount++
			errorLog = errorLog + errorMessage + "<br>"
		}
		return [count : errorCount, message: errorLog]
	}
	
	/**
	* The compareStringtoList method performs a string comparison on everything provided
	* in the checkList parameter and provides error message generation depending on match results. 
	* 
	* @param checkString - The string to compare against the list
	* @param checkList - The list to compare with the string
	* @param validationCheck - This map is used throughout all checks in the jsonUtilities 
	* library as a way of storing error message, count and log information. It is used 
	* in the associated groovy scripts to details reporting functionality on failures.
	* @return java.util.Map - The error log containing the results of the check. If no errors
	* the map will be the same as that which was provided to the method.
	*/
	public java.util.Map compareStringToList(String checkString, String[] checkList, java.util.Map validationCheck)
	{
		int errorCount = validationCheck.count;
		String errorLog = validationCheck.message;
		String errorMessage
		 
		Boolean bInList = false;
		//Check if the provided strings match. If the dont, generate an error message.
		for(int i = 0; i<checkList.length; i++)
		{
			if(checkString.equals(checkList[i]))
			{
				bInList = true;
				break;
			}
		}
		
		if (!bInList)
		{
			errorMessage = "First input \"$checkString\" is not in the list of strings provided"
			errorCount++
			errorLog = errorLog + errorMessage + "<br>"
		}
		return [count : errorCount, message: errorLog]
	}
	

	/**
	* The slurpString method simply converts a given JSON string to a map of nodes and their values.
	* This method is largely provided to remove the necessity to create JsonSlurper objects in associated
	* groovy scripts for each API.
	*
	* @param message - The message string to convert into a map. Intended to be used on the JSON 
	* provided by ReadyAPI!
	* @return java.util.Map - The map of nodes and their respective values.
	*/
	public java.util.Map slurpString(String message)
	{
		groovy.json.JsonSlurper slurper = new groovy.json.JsonSlurper();
		return slurper.parseText(message);
	}
 

	/**
	* The slurpString method simply converts a given JSON string to an array of maps for when multiple objects are returned.
	* This method is largely provided to remove the necessity to create JsonSlurper objects in associated
	* groovy scripts for each API.
	*
	* @param message - The message string to convert into a map. Intended to be used on the JSON 
	* provided by ReadyAPI!
	* @return java.util.Map - The map of nodes and their respective values.
	*/
	public ArrayList slurpStringMulti(String message)
	{
		groovy.json.JsonSlurper slurper = new groovy.json.JsonSlurper();
		return slurper.parseText(message);
	}

	
	public java.util.Map validationLoop(java.util.Map slurpedMessage, java.util.Map<String,Closure,String>[] closureList, java.util.Map validationCheck)
	{
		java.util.Map returnMap = validationCheck
		for(int i = 1; i < closureList.length; i++)
		{
			if(closureList[i].get('expected')!="")
			{
				/*if(closureList[i].get('node')=="dueDate" && !closureList[i].get('expected').contains("T"))
				{
					String firstDate = closureList[i].get('expected') + "T11:59:00+12:00"
					String secondDate = closureList[i].get('expected') + "T12:59:00+13:00"
					String[] checkList = [firstDate,secondDate]
					validationCheck = j.checkJSONValueAgainstList(slurpResult, closureList[i].get('node'), closureList[0].get('check'), closureList[i].get('check'),checkList, validationCheck)
				}
				else
				{*/
					returnMap = checkJSONValue(slurpedMessage, closureList[i].get('node'), closureList[0].get('check'), closureList[i].get('check'),closureList[i].get('expected'), returnMap)
				//}
			}
			else
			{
				returnMap = checkJSONNodeExists(slurpedMessage, closureList[i].get('node'), closureList[0].get('check'), closureList[i].get('check'),false , returnMap)
			}
		}
		return returnMap;
	}

	/**
	* The checkJSONValue method is used to compare the value of a node in the response against an expected value.
	* To be used on checks where a node should exist and the expected value is known.
	*
	* @param slurpedMessage - The JSON message to investigate for 
	* @param nodeName - The name of the node which is being checked. Used purely for error logging purposes.
	* @param parent - The parent node of the node being checked. Used to prevent attempting checks of children of null nodes as
	* this causes exceptions. Defaults to null as this will cause a failure.
	* @param check - The node to be checked against the response message.
	* @param expectedValue - The value expected of the node being checked.
	* @param validationCheck - This map is used throughout all checks in the jsonUtilities 
	* library as a way of storing error message, count and log information. It is used 
	* in the associated groovy scripts to details reporting functionality on failures.
	* @return java.util.Map - The error log containing the results of the check. If no errors
	* the map will be the same as that which was provided to the method.
	*/
	public java.util.Map checkJSONValue(java.util.Map slurpedMessage, String nodeName, Closure parent = null, Closure check, String expectedValue,
						 java.util.Map validationCheck)
	{
		int errorCount = validationCheck.count;
		String errorLog = validationCheck.message;
		String errorMessage;

		String actualValue = "";
		Boolean parentCheck = slurpedMessage.any(parent)
		
		//Check if the parent exists as the Closure containing the child check will fail. If the parent doesn't exist, neither can the child.
		if(parentCheck)
		{
			actualValue = slurpedMessage.findResult(check);
		}
		else
		{
			errorCount++;
			errorMessage = "Parent of '"+nodeName+"' not found";
			errorLog = errorLog + errorMessage + "<br>";
		}
		
		//Check if the values match. If they dont, generate and error message.
		if(actualValue != expectedValue)
		{
			errorCount++;
			errorMessage = "Actual value \"" + actualValue + "\" of '" + nodeName + "' does not match expected value \"" + expectedValue + "\"";
			errorLog = errorLog + errorMessage + "<br>";
		}
		return [count : errorCount, message: errorLog]
	}
	
	/**
	* The checkJSONValueAgainstList method is used to compare the value of a node in the response against an expected value.
	* To be used on checks where a node should exist and the expected value is known.
	*
	* @param slurpedMessage - The JSON message to investigate for 
	* @param nodeName - The name of the node which is being checked. Used purely for error logging purposes.
	* @param parent - The parent node of the node being checked. Used to prevent attempting checks of children of null nodes as
	* this causes exceptions. Defaults to null as this will cause a failure.
	* @param check - The node to be checked against the response message.
	* @param expectedValues - A list of potential values expected of the node being checked.
	* @param validationCheck - This map is used throughout all checks in the jsonUtilities 
	* library as a way of storing error message, count and log information. It is used 
	* in the associated groovy scripts to details reporting functionality on failures.
	* @return java.util.Map - The error log containing the results of the check. If no errors
	* the map will be the same as that which was provided to the method.
	*/
	public java.util.Map checkJSONValueAgainstList(java.util.Map slurpedMessage, String nodeName, Closure parent = null, Closure check, String[] expectedValues,
						 java.util.Map validationCheck)
	{
		int errorCount = validationCheck.count;
		String errorLog = validationCheck.message;
		String errorMessage;

		String actualValue = "";
		Boolean parentCheck = slurpedMessage.any(parent)
		Boolean bInList = false;
		
		//Check if the parent exists as the Closure containing the child check will fail. If the parent doesn't exist, neither can the child.
		if(parentCheck)
		{
			actualValue = slurpedMessage.findResult(check);
		}
		else
		{
			errorCount++;
			errorMessage = "Parent node not found";
			errorLog = errorLog + errorMessage + "<br>";
		}
		
		//Check if the values match. If they dont, generate and error message.
		for(int i = 0; i < expectedValues.length; i++)
		{
			if(actualValue.equals(expectedValues[i]))
			{
				bInList = true;
				break;
			}
		}
		
		if(!bInList)
		{
			errorCount++;
			errorMessage = "Actual value \"" + actualValue + "\" of '" + nodeName + "' does not match any of the supplied expected values";
			errorLog = errorLog + errorMessage + "<br>";
		}
		return [count : errorCount, message: errorLog]
	}
	
	/**
	* The checkJSONNodeExists method is used to check whether a given node is found in the response
	*
	* @param slurpedMessage - The JSON message to investigate for 
	* @param nodeName - The name of the node which is being checked. Used purely for error logging purposes.
	* @param parent - The parent node of the node being checked. Used to prevent attempting checks of children of null nodes as
	* this causes exceptions. Defaults to null as this will cause a failure.
	* @param check - The node to be checked against the response message.
	* @param expectedValue - A boolean stating whether or not the node is expected.
	* @param validationCheck - This map is used throughout all checks in the jsonUtilities 
	* library as a way of storing error message, count and log information. It is used 
	* in the associated groovy scripts to details reporting functionality on failures.
	* @return java.util.Map - The error log containing the results of the check. If no errors
	* the map will be the same as that which was provided to the method.
	*/
	java.util.Map checkJSONNodeExists(java.util.Map slurpedMessage, String nodeName, Closure parent = null, Closure check, Boolean expectedValue,
						java.util.Map validationCheck)
	{
		int errorCount = validationCheck.count;
		String errorLog = validationCheck.message;
		String errorMessage;

		def actualValue = false;
		Boolean parentCheck = slurpedMessage.any(parent)
		
		//Check if the parent exists as the Closure containing the child check will fail. If the parent doesn't exist, neither can the child.
		if(parentCheck)
		{
			actualValue = slurpedMessage.any(check);
		}
		//else
			/*
		{
			errorCount++;
			errorMessage = "Parent node not found";
			errorLog = errorLog + errorMessage + "<br>";
		}
		*/
		//Check if the findings match what was expected. If not, generate an error message
		if(actualValue != expectedValue)
		{
			//If the value was expected and not found
			if(expectedValue)
			{
				errorMessage = "'" + nodeName + "' was not found not exist and is expected.";
			}
			//If the value was not expected but found
			else
			{
				errorMessage = "'" + nodeName + "' was found and is not expected."
			}
			errorCount++;
			errorLog = errorLog + errorMessage + "<br>";
		}
		return [count : errorCount, message: errorLog]
	}
}