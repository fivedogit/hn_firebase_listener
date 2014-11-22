package club.hackbook.hnfbl;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
 
public class FirebaseListener implements ServletContextListener {
 
    private static ExecutorService executor;
    private AWSCredentials credentials;
	private AmazonDynamoDBClient client;
	private DynamoDBMapper mapper;
	private DynamoDBMapperConfig dynamo_config;
    private Firebase myFirebaseRef = null;
    
    @SuppressWarnings("unchecked")
    @Override
    public void contextInitialized(ServletContextEvent cs) 
    {
    	try 
    	{
    		credentials = new PropertiesCredentials(getClass().getClassLoader().getResourceAsStream("AwsCredentials.properties"));
    		client = new AmazonDynamoDBClient(credentials);
    		client.setRegion(Region.getRegion(Regions.US_EAST_1)); 
    		mapper = new DynamoDBMapper(client);
    		dynamo_config = new DynamoDBMapperConfig(DynamoDBMapperConfig.ConsistentReads.EVENTUAL);
    	} 
    	catch (IOException e) 
    	{
    		e.printStackTrace();
    	}
    	myFirebaseRef = new Firebase("https://hacker-news.firebaseio.com/v0/updates");
    	
    	createExecutor();
    	cs.getServletContext().log("Executor service started !");
    	myFirebaseRef.addValueEventListener(new ValueEventListener() 
    	{
 			  @Override
 			  public void onDataChange(DataSnapshot snapshot) 
 			  {
 				  System.out.println("Data changed " + snapshot.getChildrenCount());
 				  ArrayList<String> str_value_al = null;
 				  ArrayList<Integer> int_value_al = null;
 				  
 				  for (DataSnapshot child : snapshot.getChildren())
 				  {
 					 /***
 					  *     _____ _____ ________  ___ _____ 
 					  *    |_   _|_   _|  ___|  \/  |/  ___|
 					  *      | |   | | | |__ | .  . |\ `--. 
 					  *      | |   | | |  __|| |\/| | `--. \
 					  *     _| |_  | | | |___| |  | |/\__/ /
 					  *     \___/  \_/ \____/\_|  |_/\____/ 
 					  *                                     
 					  */
 					  if(child.getKey().equals("items"))
 					  {
 						  int_value_al = child.getValue(ArrayList.class);
 						  System.out.println(child.getKey() + " " + int_value_al.toString());
 						  HNItemItem hnii = null;
 						  String result = null;
 						  Iterator<Integer> it = int_value_al.iterator();
 						  Integer item = null;
 						  while(it.hasNext())
 						  {  
 							  item = it.next();
 							  if(item != null) // strangely, values in the array CAN be null.
 							  {  
 								  try
 								  {
 									  Response r = Jsoup
 											  .connect("https://hacker-news.firebaseio.com/v0/item/" + item.intValue()  + ".json")
 											  .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
 											  .ignoreContentType(true).execute();
 									  result = r.body();
 									  hnii = mapper.load(HNItemItem.class, item.intValue(), dynamo_config);
 									  if(hnii == null) // item does not already exist.
 									  {
 										  System.out.println("item: " + item.intValue() + " does not exist. Creating.");
 										  hnii = createItemFromHNAPIResult(result);
 										  if(hnii != null)
 											  mapper.save(hnii);
 									  }
 									  else // item already exists.
 									  {
 										  System.out.println("item: " + item.intValue() + " " + " already exists. Creating.");
 										  HNItemItem new_hnii = createItemFromHNAPIResult(result);
 										  /*** check here for difference between hnii and new_hnii if you want to fire alerts or what-have-you ***/
 										  
 										  if(new_hnii != null)
 											  mapper.save(new_hnii, new DynamoDBMapperConfig(SaveBehavior.CLOBBER));
 									  }
 								  }
 								  catch(IOException ioe)
 								  {
 									  System.err.println("IOException getting item " + item.intValue() + ", but execution should continue.");
 								  }
 							  }		
 						  }
 					  }
 					  /***
 					   *    ____________ ___________ _____ _      _____ _____ 
 					   *    | ___ \ ___ \  _  |  ___|_   _| |    |  ___/  ___|
 					   *    | |_/ / |_/ / | | | |_    | | | |    | |__ \ `--. 
 					   *    |  __/|    /| | | |  _|   | | | |    |  __| `--. \
 					   *    | |   | |\ \\ \_/ / |    _| |_| |____| |___/\__/ /
 					   *    \_|   \_| \_|\___/\_|    \___/\_____/\____/\____/ 
 					   *                                                      
 					   */
 					  else if(child.getKey().equals("profiles"))
 					  {	  
 						  str_value_al = child.getValue(ArrayList.class);
 						  System.out.println(child.getKey() + " " + str_value_al.toString());
 						  HNUserItem useritem = null;
 						  String result = null;
 						  Iterator<String> it = str_value_al.iterator();
 						  String screenname = null;
 						  while(it.hasNext())
 						  {
 							  screenname = it.next();	
 							  if(screenname != null) // strangely, values in the array CAN be null.
 							  {
 								  try
 								  {
 									  result = Jsoup
 											  .connect("https://hacker-news.firebaseio.com/v0/user/" + screenname  + ".json")
 											  .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
 											  .ignoreContentType(true).execute().body();
 									  
 									  // see if the user is already in the database. That way, we can check for changes.
 									  useritem = mapper.load(HNUserItem.class, screenname, dynamo_config);
 									  if(useritem == null) // user not found.
 									  {
 										  System.out.println("Creating " + screenname + ". ");
 										  useritem = createUserFromHNAPIResult(result);
 										  mapper.save(useritem);
 									  }
 									  else // user already found.
 									  {
 										  System.out.println("Updating " + screenname + ". ");
 										  HNUserItem new_useritem = createUserFromHNAPIResult(result);
 										  
 										  /*** check here for difference between useritem and new_useritem if you want to fire alerts or what-have-you ***/
 										  
 										  if(new_useritem != null)	 
 											  mapper.save(new_useritem, new DynamoDBMapperConfig(SaveBehavior.CLOBBER));
 									  }
 								  }
 								  catch(IOException ioe)
 								  {
 									  System.err.println("IOException getting user " + screenname + ", but execution should continue.");
 								  }
 							  }
 						  }
 					  }
 					  else 
 					  {
 						  System.err.println("child.getName() was something other than \"items\" or \"profiles\"");
 					  }
 				  }  
 			  }

 			  @Override public void onCancelled(FirebaseError error) 
 			  {
 				  System.out.println("onCancelled called");
 			  }
    	});
    }
 
    @Override
    public void contextDestroyed(ServletContextEvent cs) 
    {
    	executor.shutdown();
    	cs.getServletContext().log("Executor service shutdown !");
    }
 
    public static synchronized void submitTask(Runnable runnable) 
    {
    	if (executor == null) {
    		createExecutor();
    	}
    	executor.submit(runnable);
    }
 
    public static synchronized Future<String> submitTask(Callable<String> callable) 
    {
    	if (executor == null) {
    		createExecutor();
    	}
    	return executor.submit(callable);
    }
 
    static void  createExecutor() 
    {
        executor = new ThreadPoolExecutor(1, 3, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /* HNItemItem fields
    private long id; 
	private String by;
	private long time; // stored in seconds, just like the HN API, not milliseconds
	private String type;
	private boolean dead;
	private boolean deleted;
	private long parent;
	private long score;
	private Set<Long> kids;
	private String url;
	*/
    
    private HNItemItem createItemFromHNAPIResult(String unchecked_result)
    {
    	if(unchecked_result == null || unchecked_result.isEmpty())
    	{
    		System.err.println("Error trying to create new item in DB: result string from HN api was null or empty");
    		return null;
    	}
    	try{
    		HNItemItem hnii = null;
    		JSONObject new_jo = new JSONObject(unchecked_result);
    		// these are the required fields (as far as we're concerned)
    		// without them, we can't even make sense of what to do with it
    		if(new_jo.has("id") && new_jo.has("by") && new_jo.has("time") && new_jo.has("type")) 
    		{
				  //** THESE FIELDS MUST MATCH HNItemItem EXACTLY ***
    			hnii = new HNItemItem();
    			hnii.setId(new_jo.getLong("id"));
    			hnii.setBy(new_jo.getString("by"));
    			hnii.setTime(new_jo.getLong("time"));
    			hnii.setType(new_jo.getString("type"));
    			if(new_jo.has("dead") && new_jo.getBoolean("dead") == true)
    				hnii.setDead(true);
    			else
    				hnii.setDead(false);
    			if(new_jo.has("deleted") && new_jo.getBoolean("deleted") == true)
    				hnii.setDeleted(true);
    			else
    				hnii.setDeleted(false);
    			if(new_jo.has("parent"))
    				hnii.setParent(new_jo.getLong("parent"));
    			if(new_jo.has("score"))
    				hnii.setScore(new_jo.getLong("score"));
    			if(new_jo.has("kids"))
    			{
    				HashSet<Long> kids_ts = new HashSet<Long>();
    				JSONArray ja = new_jo.getJSONArray("kids");
    				if(ja != null && ja.length() > 0)
    				{	  
    					int x = 0;
    					while(x < ja.length())
    					{
    						kids_ts.add(ja.getLong(x));
    						x++;
    					}
    					if(kids_ts.size() == ja.length()) // if the number of items has changed for some reason, just skip bc something has messed up
    					{
    						System.out.println("createHNItemFromHNAPIResult setting kids=" + kids_ts.size());
    						hnii.setKids(kids_ts);
    					}
    				}
    				else
    					hnii.setKids(null);
    			}
    			if(new_jo.has("url"))
    				hnii.setURL(new_jo.getString("url"));
    			return hnii;
    		}
    		else
    		{
    			System.err.println("Error trying to create new item in DB: missing required id, by, time or type values");
    			return null;
    		}
		  }
		  catch(JSONException jsone)
		  {
			  System.err.println("Error trying to create new item in DB: result string was not valid JSON.");
			  return null;
		  }
    }
        
    /* HNUserItem fields
    private String id;
	private long created;
	private String created_hr;
	private int karma;		   // this is set on login and every 20 minutes by getUserSelf
	private String about;
	private int delay;
	private Set<String> submitted; 
    */
    
    private HNUserItem createUserFromHNAPIResult(String result)
    {
    	if(result == null || result.isEmpty())
    		return null;
    	try 
    	{ 
    		HNUserItem useritem = new HNUserItem();
    		JSONObject profile_jo = new JSONObject(result);
    		useritem.setId(profile_jo.getString("id"));
    		useritem.setCreated(profile_jo.getLong("created"));
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    		sdf.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
    		useritem.setCreatedHumanReadable(sdf.format(profile_jo.getInt("karma")*1000));
    		useritem.setKarma(profile_jo.getInt("karma"));
    		if(profile_jo.has("about"))
    			useritem.setAbout(profile_jo.getString("about"));
    		useritem.setDelay(profile_jo.getInt("delay"));
    		if(profile_jo.has("submitted"))
    		{
    			JSONArray ja = profile_jo.getJSONArray("submitted");
    			HashSet<String> hs = new HashSet<String>();
    			if (ja != null) 
    			{ 
    				int len = ja.length();
    				for (int i=0;i<len;i++)
    				{ 
    					hs.add(ja.get(i).toString());
    				} 
    				useritem.setSubmitted(hs);
    			} 
    		}
    		return useritem;
    	} 
    	catch (JSONException e) 
    	{
    		e.printStackTrace();
    		return null;
    	}
    }
}