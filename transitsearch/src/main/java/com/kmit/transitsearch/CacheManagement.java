package com.kmit.transitsearch;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;

public class CacheManagement {
	private static final int WAITING = 0;
	private static final int OPTIONSELECTED = 1;
	private static final int LOADFST = 2;
	private static final int SEARCHTTRANSITTIME = 3;
	private static final int CACHESTATS = 3;
	CacheFST cache = new CacheFST();

	private int state = WAITING;

	public String processInput(String theInput) throws IOException, ExecutionException {
		String theOutput = null;

		if (state == WAITING) {
			theOutput = "Enter 1 to load an FST. Enter 2 to search for transit time. 3 for cache stats. x `to exit";
			state = OPTIONSELECTED;
		} else if (state == OPTIONSELECTED) {
			if (theInput.equalsIgnoreCase("1")) {
				String cacheKey = "fst";
				cache.createFST(cacheKey);
				theOutput = "FST loaded successfully";
				state = WAITING;
			} else if (theInput.equalsIgnoreCase("2")) {
				theOutput = "Enter From and To Zip code, example 500001,700002";
				state = SEARCHTTRANSITTIME;
			} else if (theInput.equalsIgnoreCase("3")) {
				// theOutput = "Enter Business, Transit Type, From and To Zip code, example BA,
				// Air, 500001,700002";
				state = CACHESTATS;
			} else if (theInput.equalsIgnoreCase("x")) {
				theOutput = "Bye";
			} else {
				theOutput = "Select an option " + "Try again";
				state = WAITING;
			}
		} else if (state == SEARCHTTRANSITTIME) {
			String zip_from_to[] = theInput.trim().split(",");
			if (zip_from_to.length == 2) {
				LoadingCache<String, FST<CharsRef>> localCache = cache.getCache();
				FST<CharsRef> transitTimeFST = localCache.get("fst");
				if (transitTimeFST == null) {
					theOutput = "Enter 1 to load an FST before you perform a search";
				} else {
					CharsRef value = Util.get(transitTimeFST,
							new BytesRef(zip_from_to[0].trim() + zip_from_to[1].trim()));
					// System.out.println(value);
					theOutput = "ETA Value of " + zip_from_to[0] + " and " + zip_from_to[1] + " is: " + value;
					state = WAITING;
					// theOutput = "Issue loading FST from cache";
					// state = WAITING;
				}
			} else {
				theOutput = "Enter From and To Zip code, example 500001,700002";
				state = WAITING;
			}

		} else if (state == CACHESTATS) {
			LoadingCache<String, FST<CharsRef>> guavaCache = cache.getCache();
			CacheStats stats = guavaCache.stats();
			theOutput = "Hit Count:" + stats.hitCount() + " Load Count:" + stats.loadCount() + " Miss Count:"
					+ stats.missCount();
		}
		return theOutput;
	}

	private String getCacheKey(String carrier, String transitType) {
		// TODO decide how to store the cache keys here, for now cache key = carrier +
		// transitType
		return carrier.concat(transitType);
	}
}