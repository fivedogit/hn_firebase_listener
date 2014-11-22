package club.hackbook.hnfbl;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

@DynamoDBTable(tableName="hn_users")
public class HNUserItem implements java.lang.Comparable<HNUserItem> {

	// static parts of the database entry
	private String id;
	private long created;
	private String created_hr;
	private int karma;		   // this is set on login and every 20 minutes by getUserSelf
	private String about;
	private int delay;
	private Set<String> submitted;
	
	@DynamoDBHashKey(attributeName="id") 
	public String getId() {return id; }
	public void setId(String id) { this.id = id; }
	
	@DynamoDBAttribute(attributeName="created")  
	public long getCreated() {return created; }
	public void setCreated(long created) { this.created = created; }
	
	@DynamoDBAttribute(attributeName="created_hr")  
	public String getCreatedHumanReadable() {return created_hr; } // note this should not be used. Always format and return the msfe value instead.
	public void setCreatedHumanReadable(String created_hr) { this.created_hr = created_hr; }
	
	@DynamoDBAttribute(attributeName="karma")  
	public int getKarma() { return karma; }
	public void setKarma(int karma) { this.karma = karma; }
	
	@DynamoDBAttribute(attributeName="about")  
	public String getAbout() {return about; }  
	public void setAbout(String about) { this.about = about; }
	
	@DynamoDBAttribute(attributeName="delay")  
	public int getDelay() {return delay; }  
	public void setDelay(int delay) { this.delay = delay; }
	
	@DynamoDBAttribute(attributeName="submitted")  
	public Set<String> getSubmitted() { return submitted; }
	public void setSubmitted(Set<String> submitted) { this.submitted = submitted; }
		
	@DynamoDBIgnore
	public HashSet<HNItemItem> getHNItemsByd(int minutes_ago, DynamoDBMapper mapper, DynamoDBMapperConfig dynamo_config) { 
		// set up an expression to query screename#id
		DynamoDBQueryExpression<HNItemItem> queryExpression = new DynamoDBQueryExpression<HNItemItem>()
				.withIndexName("by-time-index")
				.withScanIndexForward(true)
				.withConsistentRead(false);
	        
		// set the user_id part
		HNItemItem key = new HNItemItem();
		key.setBy(getId());
		queryExpression.setHashKeyValues(key);
		
		// set the msfe range part
		if(minutes_ago > 0)
		{
			//System.out.println("Getting comment children with a valid cutoff time.");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, (minutes_ago * -1));
			long time_cutoff = cal.getTimeInMillis() / 1000;
			// set the msfe range part
			Map<String, Condition> keyConditions = new HashMap<String, Condition>();
			keyConditions.put("time",new Condition()
			.withComparisonOperator(ComparisonOperator.GT)
			.withAttributeValueList(new AttributeValue().withN(new Long(time_cutoff).toString())));
			queryExpression.setRangeKeyConditions(keyConditions);
		}	

		// execute
		List<HNItemItem> notificationitems = mapper.query(HNItemItem.class, queryExpression, dynamo_config);
		if(notificationitems != null && notificationitems.size() > 0)
		{	
			HashSet<HNItemItem> returnset = new HashSet<HNItemItem>();
			for (HNItemItem notificationitem : notificationitems) {
				returnset.add(notificationitem);
			}
			return returnset;
		}
		else
		{
			return null;
		}
	}

	@DynamoDBIgnore
	public int compareTo(HNUserItem o) // this makes more recent comments come first
	{
	    String otherscreenname = ((HNUserItem)o).getId();
	    int x = otherscreenname.compareTo(getId());
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
}
