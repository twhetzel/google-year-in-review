import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/*
 * Use the Google+ API to get Activities for a userId and 
 * collect all items that contain the post of a public picture. 
 * Rank each post by month and by popularity (plusoners, shares, comments).  
 * Then get pictures for the most popular items and create a new album for the user.
 * 
 * @author whetzel
 * @date 12-25-2014
 */


public class Main {
	//TODO Add file location as a program parameter or store data in memory
	//public static File file = new File("/Users/whetzel/Documents/workspace/google-year-in-review/data/googleActivitiesFile.txt");
	public static File file = new File("./data/googleActivitiesFile.txt");
	
	/**
	 * @param args
	 * @return 
	 * @return 
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws java.text.ParseException 
	 */
	public static void main(String[] args) throws ParseException, InterruptedException, IOException, java.text.ParseException {
		//String userId = "+EJohnFeig";
		String userId = "UserIdOfInterest";
		String apiKey = "YourApiKey";
		
		// ** DO NOT DELETE ** Remove comment after getting data file to develop scoring section
		// If file does not exists, then create it
		/* if (!file.exists()) {
			file.createNewFile();
		}
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		String nextPageToken = getFirstActvities(userId, apiKey, bw);
		System.out.println("Page Token from First call: "+nextPageToken);

		//Try to get all other activities
		getAllActivities(userId, apiKey, nextPageToken, bw);
		bw.close();
		System.out.println("Date out of range found. Move to processing data!");
		*/
		//** DO NOT DELETE ** Remove comment after getting data file to develop scoring section
		
		/**
		 * Set of methods that use static file generated from methods above 
		 * to rank the posts before downloading images
		 */
		rankData();
		
	}


	/**
	 * Get first page of Activities with known maxResults 
	 * @param userId
	 * @param apiKey
	 * @param bw 
	 * @return 
	 */
	private static String getFirstActvities(String userId, String apiKey, BufferedWriter bw) {
		String nextPageToken = null;
		try {
			URL url = new URL("https://www.googleapis.com/plus/v1/people/"+userId+"/activities/public?key="+apiKey+"&maxResults=20");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			JSONObject response = null;
			JSONParser parser = new JSONParser();
			try {
				response = (JSONObject) parser
						.parse(new InputStreamReader(conn.getInputStream()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			//System.out.println(response.toString());
			nextPageToken = ParseGooglePlusActivities.parseJson(response, bw);

			conn.disconnect();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {		 
			e.printStackTrace();	
		}
		return nextPageToken;
	}


	/**
	 * Get all Activities for a User based on their Google+ ID
	 * @param userId
	 * @param nextPageToken 
	 * @param bw 
	 * @param ApiKey 
	 * @return 
	 * @return 
	 * @throws ParseException 
	 */
	private static void getAllActivities(String userId, String APIKey, String nextPageToken, BufferedWriter bw) throws ParseException {
		//Documentation for Activities: https://developers.google.com/+/api/latest/activities
		// E.g. https://www.googleapis.com/plus/v1/people/+EJohnFeig/activities/public?key=AIzaSyBEOKC-Oq2vFEEQNYobB6CJS66457JHv90&maxResults=100
		try {
			URL url = new URL("https://www.googleapis.com/plus/v1/people/"+userId+"/activities/public?key="+APIKey+"&maxResults=20"+"&pageToken="+nextPageToken);
			//System.out.println("Web service URL: "+url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			JSONObject response;
			JSONParser parser = new JSONParser();
			response = (JSONObject) parser
					.parse(new InputStreamReader(conn.getInputStream()));
			nextPageToken = ParseGooglePlusActivities.parseJson(response, bw);
			//System.out.println("Returned nextPageToken: "+nextPageToken);
			conn.disconnect();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {		 
			e.printStackTrace();	
		}
		if(!nextPageToken.equals(null)) {
			//Check date to see if still in date range of interest
			if (checkDate(userId, APIKey, nextPageToken)) {
				getAllActivities(userId, APIKey, nextPageToken, bw);
			}
		}
	}

	/**
	 * Check date from web service call
	 * @param nextPageToken 
	 * @param apiKey 
	 * @param userId 
	 * @return
	 * @throws ParseException 
	 */
	private static boolean checkDate(String userId, String apiKey, String nextPageToken) throws ParseException {
		boolean dateStatus = false;
		try {
			URL url = new URL("https://www.googleapis.com/plus/v1/people/"+userId+"/activities/public?key="+apiKey+"&maxResults=20"+"&pageToken="+nextPageToken);
			//System.out.println("Web service URL: "+url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			JSONObject response;
			JSONParser parser = new JSONParser();
			response = (JSONObject) parser
					.parse(new InputStreamReader(conn.getInputStream()));
			dateStatus = ParseGooglePlusActivitiesForDate.parseJson(response);
			//System.out.println("Date Status:"+dateStatus);
			conn.disconnect();
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch (IOException e) {		 
			e.printStackTrace();	
		}		
		return dateStatus;
	}
	
	
	/**
	 * Rank data by date and popularity of content. 
	 * @throws java.text.ParseException 
	 */
	private static void rankData() throws java.text.ParseException {
		HashMap<String, String> data = new HashMap<String, String>();
		String[] dataItems = new String[7];
		ArrayList<String> list = new ArrayList<String>();
		HashMap<String, Integer> allScores = new HashMap<String, Integer>();
		Map<String, Integer> sortedByScore = null;
		HashMap<String, String> allDates = new HashMap<String, String>();
		Map<String, String> sortedByDate = null;
		
		/*
		 * Read in data file
		 */
		BufferedReader br = null;
			
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(file));
 
			while ((sCurrentLine = br.readLine()) != null) {
				System.out.println("Data from file: "+sCurrentLine);
				// Process data to weight as follows; replies x3, plusOnes x1, reshares x2, #throughglass x5 (display name)
				dataItems = sCurrentLine.split("\t");
				// Add file contents to data structure
				String activityID = dataItems[0];
				String[] activityItems = activityID.split(": ");
				String activity = activityItems[1];
				
				String pubDate = dataItems[1];
				String[] dateItems = pubDate.split(": ");
				String date = dateItems[1];
				
				String imageURL = dataItems[3];	
				String[] imageURLItems = imageURL.split(": ");
				String url = imageURLItems[1];
				
				list.add(date);
				list.add(url);
				//System.out.println("LIST: "+list+"\n");
				
				data.put(activity, url); //Add contents of line to data structure, Try just URL in HashMap
				//data = createDataHashMap(activity, list); //create data structure in method to avoid while loop issues
				//Clear ArrayList for next line
				list.clear();
				
				String displayName = dataItems[2];
				
				String replies = dataItems[4];
				String[] replyScoreItems = replies.split(": ");
				String replyScore = replyScoreItems[1];
				replyScore = replyScore.trim();
				
				String plusOnes = dataItems[5];
				String[] plusOneItems = plusOnes.split(": ");
				String plusOneScore = plusOneItems[1];
				
				String reshares = dataItems[6];
				String[] reshareScoreItems = reshares.split(": ");
				String reshareScore = reshareScoreItems[1];
				//System.out.println("Items to Score: "+displayName+"\t"+replyScore+"\t"+plusOneScore+"\t"+reshareScore);
				
				//Sort posts by Date to change Date format time stamp to string month name 
				allDates.put(activity, date);
				//sortedByDate = sortPostsByDate(allDates);
				//System.out.println("sortedByDate size:"+sortedByDate.size());
				
				//Score posts 
				int totalScore = scorePost(displayName,replyScore,plusOneScore,reshareScore);
				//System.out.println("Total Score: "+totalScore+"\n");
				allScores.put(activity, totalScore);
				//System.out.println("All Score Data"+allScores);
				
				//Sort by score --> DO LATER
				//sortedByScore = sortPostsByScore(allScores);
				//System.out.println("Sorted Scores: "+sortedScores);
				
			}	
			//list.clear();  //--> Need to find location to clear the ArrayList for "data"  
			
			//Print out top 1-3 posts/month 
			System.out.println("DATA HashMap: "+data);
			getDataForTopPosts(allDates, data, allScores);
			
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}	
	}
	
	/**
	 * Sort posts by Date
	 * @param allDates
	 * @return
	 * @throws java.text.ParseException 
	 */
	private static Map<String, String> sortPostsByDate(
			HashMap<String, String> allDates) throws java.text.ParseException {
		HashMap<String, String> reformattedAllDates = new HashMap<String, String>();
		HashMap<String, String> test = new HashMap<String, String>();
		
		SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		Date january = df.parse("2014-01-01");
		Date february = df.parse("2014-02-01");
		Date march = df.parse("2014-03-01");
		Date april = df.parse("2014-04-01");
		Date may = df.parse("2014-05-01");
		Date june = df.parse("2014-06-01");
		Date july = df.parse("2014-07-01");
		Date august = df.parse("2014-08-01");
		Date september = df.parse("2014-09-01");
		Date october = df.parse("2014-10-01");
		Date november = df.parse("2014-11-01");
		Date december = df.parse("2014-12-01");
		Date january15 = df.parse("2015-01-01");
		
		// Iterate through HashMap
		Iterator it = allDates.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        String id = pairs.getKey().toString();
	        String date = pairs.getValue().toString();
	        //System.out.println("allDates: "+pairs.getKey() + " = " + pairs.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	        
	        //System.out.println("JAN: "+january);
	        Date dateFormatted = (Date)sdf.parse(date);
	        //System.out.println("dateFormatted: "+dateFormatted);
	        
	        //Group by date, Can only use compareTo with Date Object 
	        if (dateFormatted.compareTo(january)>0 && dateFormatted.compareTo(february)<0) {
	        	//System.out.println("** Date is in January "+dateFormatted+"for activityId: "+id+"\n\n");
	        	reformattedAllDates.put(id, "january"); //ERROR - Only adds last seen key/value
	        	test.putAll(reformattedAllDates);
	        }
	        if (dateFormatted.compareTo(february)>0 && dateFormatted.compareTo(march)<0) {
	        	//System.out.println("** Date is in February "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, "february");
	        }
	        if (dateFormatted.compareTo(march)>0 && dateFormatted.compareTo(april)<0) {
	        	//System.out.println("** Date is in March "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(april)>0 && dateFormatted.compareTo(may)<0) {
	        	//System.out.println("** Date is in April "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(may)>0 && dateFormatted.compareTo(june)<0) {
	        	//System.out.println("** Date is in May "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(june)>0 && dateFormatted.compareTo(july)<0) {
	        	//System.out.println("** Date is in June "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(july)>0 && dateFormatted.compareTo(august)<0) {
	        	//System.out.println("** Date is in July "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(august)>0 && dateFormatted.compareTo(september)<0) {
	        	//System.out.println("** Date is in August "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(september)>0 && dateFormatted.compareTo(october)<0) {
	        	//System.out.println("** Date is in September "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(october)>0 && dateFormatted.compareTo(november)<0) {
	        	//System.out.println("** Date is in October "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(november)>0 && dateFormatted.compareTo(december)<0) {
	        	//System.out.println("** Date is in November "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	        if (dateFormatted.compareTo(december)>0 && dateFormatted.compareTo(january15)<0) {
	        	//System.out.println("** Date is in December "+dateFormatted+"\n\n");
	        	reformattedAllDates.put(id, dateFormatted.toString());
	        }
	    }	
		//return reformattedAllDates;
	    return test;
	}
	
	


	/**
	 * Score post 
	 * @param displayName
	 * @param replies
	 * @param plusOnes
	 * @param reshares
	 * @return 
	 */
	private static int scorePost(String displayName, String replyScore,
			String plusOneScore, String reshareScore) {
		// Process data to weight as follows; replies x3, plusOnes x1, reshares x2, #throughglass x5 (display name)
		int score = 0;
		if (displayName.contains("#throughglass")) {
			score = score + 5; 
		}
		//if (replyScore != null && !replyScore.isEmpty()) {
		if (!replyScore.equals("null")) {
			//System.out.println("RS: \'"+replyScore+"\'");
			score = Integer.parseInt(replyScore) * 3;
		}
		if (!plusOneScore.equals("null")) {
			score = Integer.valueOf(plusOneScore) * 1;
		}
		if (!reshareScore.equals("null")) {
			score = Integer.valueOf(reshareScore) * 2;
		}
		//System.out.println("Total Score for Post: "+score+"\n");
		return score;
	}

	
	/**
	 * Sort by score
	 * @param allScores
	 * @return 
	 */
	private static Map<String, Integer> sortPostsByScore(HashMap<String, Integer> allScores) {
		// Convert Map to List
		// http://www.mkyong.com/java/how-to-sort-a-map-in-java/
		List<Map.Entry<String, Integer>> list = 
				new LinkedList<Map.Entry<String, Integer>>(allScores.entrySet());

		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	
	
	/**
	 * Get top 3 scoring posts for each month and their photo data(?)
	 * @param sortedByDate 
	 * @param sortedScores
	 * @param data
	 * @param allScores 
	 */
	private static void getDataForTopPosts(Map<String, String> sortedByDate, HashMap<String, String> data, 
			HashMap<String, Integer> allScores) {
		String[] months = {"january", "february", "march", "april", "may",
				"june", "july", "august", "september", "october", "november", "december"};
		
		//Invert sortedByDate HashMap to be able to get all values for a given key(month)
		Multimap<String, String> multiMap = HashMultimap.create();
		for (Entry<String, String> entry : sortedByDate.entrySet()) {
		  multiMap.put(entry.getValue(), entry.getKey());
		}
		
		// Iterate through months array
		for (String m: months) {
			HashMap<String, Integer> hashMap = new HashMap<String, Integer>(); 
			System.out.println("Month: "+m);
			//Get all activityIds (keys) for month "m" from multiMap
			// http://stackoverflow.com/questions/12710494/java-how-to-get-set-of-keys-having-same-value-in-hashmap
			for (Entry<String, Collection<String>> entry : multiMap.asMap().entrySet()) {
			//System.out.println("Original value: " + entry.getKey() + " was mapped to keys: "
			//		+ entry.getValue());
				if (m.equals(entry.getKey())) {
					//Print out all keys for month
					//System.out.println("Month: " + entry.getKey() + " all activityIds: "
					//		+ entry.getValue());
					
					// Rank scores for posts in the month
					// Iterate through all activityIds and get the score, then use method to order by score
					for (String id : entry.getValue()) {
						Integer score = allScores.get(id);
						//System.out.println("ID:"+id+" Score:"+score.toString());
						hashMap.put(id, score);
					}
					Map<String, Integer> sortedPosts = sortPostsByScore(hashMap);
					System.out.println("Sorted score for the month ("+m+"): "+sortedPosts);
		
					int scoreCount = 0;	
					String keyScores = null;
					for (Entry<String, Integer> entrySortedPosts : sortedPosts.entrySet()) {
						scoreCount++;
						if (scoreCount <= 3) {
							keyScores = entrySortedPosts.getKey();
							System.out.println("Key: " + entrySortedPosts.getKey() + " Value: "
									+ entrySortedPosts.getValue());
							// Get data line for this top score
							//String dataLine = data.get(keyScores);
							System.out.println(" Data Line Value: "+data.get(entrySortedPosts.getKey())+"\n");
						}
					}
				}
			}
		}
	}
	

}
