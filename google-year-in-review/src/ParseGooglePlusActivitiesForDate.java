import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class ParseGooglePlusActivitiesForDate {
	private final static Logger LOG = Logger.getLogger(ParseGooglePlusActivities.class.getName()); 
	private static final String TAG = "ParseGooglePlusActivitiesForDate ";

	static boolean dateStatus;;
	public static boolean parseJson(JSONObject jsonObject) {
		//String name = (String) jsonObject.get("name");
		//String nextPageToken = (String) jsonObject.get("nextPageToken");

		JSONArray items = (JSONArray) jsonObject.get("items");

		Iterator i = items.iterator();
		while (i.hasNext()) {
			JSONObject objectsInItems = (JSONObject) i.next();
			String published = (String)objectsInItems.get("published");
			//System.out.println("PublishedDate: "+published);

			//Check that date is in Year of Interest,e.g. 2014
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				Date sdfPublished = sdf.parse(published);
				//System.out.println("sdfPublishedDate:"+sdfPublished);
				Date startDateOfInterest = sdf.parse("2014-01-01");
				Date endDateOfInterest = sdf.parse("2014-12-31");
				if (sdfPublished.compareTo(startDateOfInterest)>0 && sdfPublished.compareTo(endDateOfInterest)<0) {
					//System.out.println("** Item in Year of Interest");
					dateStatus = true;
				}
				else {
					//System.out.println("* Not in Year of Interest");
					dateStatus = false;
				}
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Result of check for Date Status:"+dateStatus);
		return dateStatus;
	}
}
