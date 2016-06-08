package com.pivotal.awsiot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AwsIotCollectorController {

	// this is a prefix for all keys created by this system, for easy key attribution
	public static final String IOTKEY = "AWSIOT";
	
	// # entries to keep in the FIFO queue that is our backend
	public static final long LISTMAX = 500L;
	
    @Autowired
    private RedisTemplate<String, String> template;

    /**
     * This method receives a JSON document representing each atom entry pushed through
     * the system. This is the destination entry point for the AWS Lambda method that
     * receives the event data from the IoT devices via the AWS IoT framework.
     * @param feed the feed tag associated with this entry; this tells you the identity of the original collector.
     * @param body the JSON document that represents this entry
     * @return a JSON document containing a message about the disposition of the request
     */
	@RequestMapping(method = RequestMethod.POST, value = "/f/{feed}", produces = "application/json")
	public String handleFeedEntry(@PathVariable("feed") String feed, @RequestBody String body) {
		String cleaned = body.replace("\\n", "\r\n");
		System.out.println("got a call on feed: " + feed);
		System.out.println("body was: " + cleaned);
		
		BoundListOperations<String, String> listops = template.boundListOps(ck(IOTKEY, feed));
		// we push new entries at the front of the list
		listops.leftPush(cleaned);
		
		// if the list has grown to exceed its max size, we pop items off from the end, i.e. it is a FIFO
		while (listops.size() > LISTMAX) listops.rightPop(); 
		
		return "{ \"status\": \"ok\", \"feedsize\": "+ listops.size() + " }";
	}

	/**
	 * This method allows a user to see the entry at a given index for a given feed. The entries are stored
	 * in a FIFO sorted by publication timestamp descending.   
	 * @param feed the feed tag whose entries you wish to access
	 * @param index the index of the entry you wish to access
	 * @return a JSON document containing a message about the disposition of the request
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/f/{feed}/{index}", produces = "application/json")
	public String handleGetFeed(@PathVariable("feed") String feed, @PathVariable("index") int index) {
		if (template.hasKey(ck(IOTKEY, feed))) {
			BoundListOperations<String, String> listops = template.boundListOps(ck(IOTKEY, feed));
			if (index < listops.size()) return listops.index(index);
			else return "{ \"message\": \"no such entry found\" }";
		}
		return "{ \"message\": \"no list at feedkey found\" }";
	}
	
	private String ck(String redisKey, String feedKey) {
		return redisKey + ":" + feedKey;
	}
	
}
