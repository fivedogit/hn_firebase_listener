package club.hackbook.hnfbl;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/* BTW, this is called HNItemItem because the "Item" suffix is the convention for signifying a DynamoDBMapper Item Class */

@DynamoDBTable(tableName="hn_items")
public class HNItemItem implements java.lang.Comparable<HNItemItem> {   
	
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
	
	@DynamoDBHashKey(attributeName="id")  
	public long getId() {return id; }
	public void setId(long id) { this.id = id; }
	
	@DynamoDBAttribute(attributeName="by") 
	@DynamoDBIndexHashKey(attributeName="by", globalSecondaryIndexName="by-time-index") 
	public String getBy() {return by; }
	public void setBy(String by) { this.by = by; }
	
	@DynamoDBAttribute(attributeName="time") 
	@DynamoDBIndexRangeKey(attributeName="time", globalSecondaryIndexName="by-time-index") 
	public long getTime() {return time; }
	public void setTime(long time) { this.time = time; } // stored in seconds, just like the HN API, not milliseconds
	
	@DynamoDBAttribute(attributeName="type") 
	public String getType() {return type; }
	public void setType(String type) { this.type = type; }
	
	@DynamoDBAttribute(attributeName="dead") 
	public boolean getDead() {return dead; }
	public void setDead(boolean dead) { this.dead = dead; }
	
	@DynamoDBAttribute(attributeName="deleted") 
	public boolean getDeleted() {return deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }
	
	@DynamoDBAttribute(attributeName="parent") 
	public long getParent() {return parent; }
	public void setParent(long parent) { this.parent = parent; }
	
	@DynamoDBAttribute(attributeName="score") 
	public long getScore() {return score; }
	public void setScore(long score) { this.score = score; }
	
	@DynamoDBAttribute(attributeName="kids")  
	public Set<Long> getKids() { return kids; }
	public void setKids(Set<Long> kids) { this.kids = kids; }
	
	@DynamoDBAttribute(attributeName="url") 
	public String getURL() {return url; }
	public void setURL(String url) { this.url = url; }
	
	@DynamoDBIgnore
	public int compareTo(HNItemItem o) // this makes more recent comments come first
	{
	    long othertime = ((HNItemItem)o).getTime();
	    if(othertime < getTime()) // this is to prevent equals
	    	return 1;
	    else if(othertime > getTime())
	    	return -1;
	    else
	    	return 0;
	}
	
}
