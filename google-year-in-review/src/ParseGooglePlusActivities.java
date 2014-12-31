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
			try {
				sdfPublished = sdf.parse(published);
				//System.out.println("sdfPublishedDate:"+sdfPublished);
				Date startDateOfInterest = sdf.parse("2014-01-01");
				Date endDateOfInterest = sdf.parse("2014-12-31");
				if (sdfPublished.compareTo(startDateOfInterest)>0 && sdfPublished.compareTo(endDateOfInterest)<0) {
					//System.out.println("** Item in Year of Interest");
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
								bw.append("ActivityID: "+id+"\tPublishedDate: "+sdfPublished+"\tDisplayName: "+displayName+"\tImageURL: "+url
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
}

