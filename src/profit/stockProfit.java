package profit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import org.json.JSONObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/maxprofit")
public class stockProfit {	
	
	LocalDate today = LocalDate.now();	
	java.util.Date targetDate = java.sql.Date.valueOf(today.minusDays(180));
	
	final String timeseries = "TIME_SERIES_DAILY";
	final String opsize = "full";
	final String apikey = "7Q803PIO2VZNV3GM";
	
	final String emptySymbolResult = "No Stock Symbol Entered!";
	final String errorMsgKey = "Error Message";
	final String errorResult = "Invalid Request. Please check request parameters and try again!";
	final String error = "Something went wrong!";
	
	@GET
	public String getMaximumProfit(@QueryParam("symbol") String varSymbol) {
		long startTime = System.nanoTime();
		
		if(varSymbol == null || varSymbol.isEmpty() || varSymbol.trim().length() == 0)
			return emptySymbolResult;
		
		String[] resultArray;
		
		try {
			String response = getHistoricalStockData(varSymbol);
			
			//Read JSON response and print
			JSONObject myResponse = new JSONObject(response);			
			
			if(myResponse.has(errorMsgKey)) {
				return errorResult;
			} 
			
			JSONObject jsonobj = (JSONObject)myResponse.get("Time Series (Daily)");		
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			TreeMap<String, JSONObject> tmap = new TreeMap<String, JSONObject>();
			JSONObject values;
			Iterator<?> keys = jsonobj.keys();
			
			//Populate TreeMap with JSON objects sorted by dates
			while(keys.hasNext())
			{
				String key = (String)keys.next();
				Date date = sdf.parse(key);
				if ( jsonobj.get(key) instanceof JSONObject && date.after(targetDate)) 
				{
					values = (JSONObject)jsonobj.get(key);
					tmap.put(key, values);
				}			
			}	
		
			resultArray = CalculateMaximumProfit(tmap);
		} catch(Exception ex) {
			return error;
		}
		
		String result = "";
		if(resultArray == null) {
			result = "Data Set Empty!";
		} else {
			result = "For Maximum Profit with "+varSymbol+" in the last 180 days: "+"\n"+"Buy Date = "+resultArray[0]+"\n"+"Sell Date = "+resultArray[1]+"\n"+"Maximum Profit = $"+resultArray[2];
		}
		
		//Calculate total execution time
		long endTime = System.nanoTime();
		String duration = String.format("%.2f",(double)((endTime - startTime)/1000000000.0)); 
		System.out.println("Implementation Duration = "+duration+" secs!");
		
		return result;
	}
	
	private String getHistoricalStockData(String varSymbol) throws Exception
	{
		String url = "https://www.alphavantage.co/query?function="+timeseries+"&symbol="+varSymbol+"&outputsize="+opsize+"&apikey="+apikey;

		//Establishing connection
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		
		if(responseCode != 200) {
			throw new Exception(errorResult);
		} 
		
		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		return response.toString();
	}

	//method to calculate maximum profit with daily high prices
	private String[] CalculateMaximumProfit(TreeMap<String, JSONObject> tmap) throws Exception
	{
		String[] maxProfitData = null;
		double maxProfit=0;

		if(tmap.size() != 0)			 
		{
			maxProfitData = new String[3];
			String currentBuyDate = tmap.firstKey();
			String finalBuyDate = tmap.firstKey();
			String sellDate = tmap.lastKey();
			double costPrice = Double.parseDouble((String)tmap.get(tmap.firstKey()).get("2. high"));
			for(String s:tmap.keySet())
			{
				double currentPrice = Double.parseDouble((String) tmap.get(s).get("2. high"));				
				if(currentPrice < costPrice)
				{
					costPrice = currentPrice;
					currentBuyDate = s;					
				}					
				else
				{
					if(currentPrice-costPrice > maxProfit)
					{
						maxProfit = currentPrice-costPrice;
						sellDate = s;
						finalBuyDate = currentBuyDate;
					}
				}				
			}
			maxProfitData[0] = finalBuyDate;
			maxProfitData[1] = sellDate;
			maxProfitData[2] = String.format("%.2f", maxProfit);
		}
		return maxProfitData;		
	}
}


