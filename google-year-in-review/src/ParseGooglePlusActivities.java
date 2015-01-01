import java.io.FileNotFoundException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;


public class ParseGooglePlusActivities {
	private final static Logger LOG = Logger.getLogger(ParseGooglePlusActivities.class.getName()); 
	private static final String TAG = "ParseGooglePlusActivities ";
	
	public static String parseJson(JSONObject jsonObject, BufferedWriter bw) throws IOException {
		//System.out.println("OUT: "+jsonObject);
		String name = (String) jsonObject.get("name");
		String nextPageToken = (String) jsonObject.get("nextPageToken");
		//System.out.println("NEXT PAGE TOKEN: "+nextPageToken);
		//LOG.info(TAG+nextPageToken);

		JSONArray items = (JSONArray) jsonObject.get("items");
		//System.out.println("ITEMS: "+items);

		Iterator i = items.iterator();
		while (i.hasNext()) {
			JSONObject objectsInItems = (JSONObject) i.next();
			String verb = (String) objectsInItems.get("verb");
			//System.out.println("Verb: "+verb);

			String id = (String)objectsInItems.get("id");
			//System.out.println(id);

			String published = (String)objectsInItems.get("published");
			//System.out.println("PublishedDate: "+published);
			//Check that date is in Year of Interest,e.g. 2014
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date sdfPublished = null;
			String dateString = null;
			try {
				sdfPublished = sdf.parse(published);
				//System.out.println("sdfPublishedDate:"+sdfPublished);
				Date startDateOfInterest = sdf.parse("2014-01-01");
				Date endDateOfInterest = sdf.parse("2014-12-31");
				if (sdfPublished.compareTo(startDateOfInterest)>0 && sdfPublished.compareTo(endDateOfInterest)<0) {
					//System.out.println("** Item in Year of Interest");
					dateString = convertDateFormat(sdfPublished);
					if (dateString == null) {
						dateString = "defaultDate";
					}
					System.out.println("Date: "+dateString);
				}
				else {
					//System.out.println("* Not in Year of Interest");
				}
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}


			/* 
			 * Limit data review to items that are posts vs. reshares
			 */
			if (verb.equals("post")) {
				JSONObject object = (JSONObject) objectsInItems.get("object");
				//itemsObjects.get("object");
				//System.out.println("OBJECT: "+object);

				String objectType = (String)object.get("objectType");
				//System.out.println("ObjectType: "+objectType);
				if (objectType.equals("note")) {
					// Get other details counts and attachments
					//System.out.println("** Found a note");

					//Get stats for replies, plusoners, and resharers
					JSONObject replies = (JSONObject) object.get("replies");
					Long replyTotalItems = (Long) replies.get("replyTotalItems");
					JSONObject plusoners = (JSONObject) object.get("plusoners");
					Long plusOneTotalItems = (Long) plusoners.get("totalItems");
					JSONObject resharers = (JSONObject) object.get("resharers");
					Long reshareTotalItems = (Long) plusoners.get("reshareTotalItems");
					//System.out.println("Replies:"+replyTotalItems+" PlusOners:"+plusOneTotalItems+" Resharers:"+reshareTotalItems);

					// Get attachments array
					if (object.containsKey("attachments")) {
						JSONArray attachments = (JSONArray) object.get("attachments");
						Iterator j = attachments.iterator();
						while (j.hasNext()) {
							JSONObject attachmentObjects = (JSONObject) j.next();
							if (attachmentObjects.containsKey("objectType")) {
								String attachmentObjectType = (String) attachmentObjects.get("objectType");
								//System.out.println("objectType: "+attachmentObjectType);

								// Only get attachments of photos
								if (attachmentObjectType.equals("photo")) {
									String displayName = (String) attachmentObjects.get("displayName");
									//System.out.println("Display Name: "+displayName);

									// Get image and it's URL
									JSONObject image = (JSONObject) attachmentObjects.get("image");
									String url = (String) image.get("url");
									//System.out.println("URL: "+url);
								bw.append("ActivityID: "+id+"\tPublishedDate: "+dateString+"\tDisplayName: "+displayName+"\tImageURL: "+url
										+"\tReplies: "+replyTotalItems+"\tPlusOnes: "+plusOneTotalItems+"\tReshares: "+reshareTotalItems+"\n");
								}
								else {
									//System.out.println("* No photo found");
								}
							}
						}
					}
					else {
						//System.out.println("* No attachments found\n");
					}
				}
			}
			else {
				//System.out.println("* Not a post for item with ID="+id);
			}
			//System.out.println();
		}
		return nextPageToken;
	}

	/**
	 * Convert date to name of month
	 * @param sdfPublished
	 * @return
	 * @throws java.text.ParseException 
	 */
	private static String convertDateFormat(Date sdfPublished) throws java.text.ParseException {
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
		String dateMonth = null;
		
		if (sdfPublished.compareTo(january)>0 && sdfPublished.compareTo(february)<0) {
			System.out.println("** Date is in January "+sdfPublished);
			dateMonth = "january";
		}
		if (sdfPublished.compareTo(february)>0 && sdfPublished.compareTo(march)<0) {
        	System.out.println("** Date is in February "+sdfPublished+"\n\n");
        	dateMonth = "february";
        }
        if (sdfPublished.compareTo(march)>0 && sdfPublished.compareTo(april)<0) {
        	//System.out.println("** Date is in March "+dateFormatted+"\n\n");
        	dateMonth = "march";
        }
        if (sdfPublished.compareTo(april)>0 && sdfPublished.compareTo(may)<0) {
        	//System.out.println("** Date is in April "+dateFormatted+"\n\n");
        	dateMonth = "april";
        }
        if (sdfPublished.compareTo(may)>0 && sdfPublished.compareTo(june)<0) {
        	//System.out.println("** Date is in May "+dateFormatted+"\n\n");
        	dateMonth = "may";
        }
        if (sdfPublished.compareTo(june)>0 && sdfPublished.compareTo(july)<0) {
        	//System.out.println("** Date is in June "+dateFormatted+"\n\n");
        	dateMonth = "june";
        }
        if (sdfPublished.compareTo(july)>0 && sdfPublished.compareTo(august)<0) {
        	//System.out.println("** Date is in July "+dateFormatted+"\n\n");
        	dateMonth = "july";
        }
        if (sdfPublished.compareTo(august)>0 && sdfPublished.compareTo(september)<0) {
        	//System.out.println("** Date is in August "+dateFormatted+"\n\n");
        	dateMonth = "august";
        }
        if (sdfPublished.compareTo(september)>0 && sdfPublished.compareTo(october)<0) {
        	//System.out.println("** Date is in September "+dateFormatted+"\n\n");
        	dateMonth = "september";
        }
        if (sdfPublished.compareTo(october)>0 && sdfPublished.compareTo(november)<0) {
        	//System.out.println("** Date is in October "+dateFormatted+"\n\n");
        	dateMonth = "october";
        }
        if (sdfPublished.compareTo(november)>0 && sdfPublished.compareTo(december)<0) {
        	//System.out.println("** Date is in November "+dateFormatted+"\n\n");
        	dateMonth = "november";
        }
        if (sdfPublished.compareTo(december)>0 && sdfPublished.compareTo(january15)<0) {
        	//System.out.println("** Date is in December "+dateFormatted+"\n\n");
        	dateMonth = "december";
        }
		return dateMonth;
	}
}

