package com.kmit.transitsearch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Outputs;
import org.apache.lucene.util.fst.PairOutputs;
import org.apache.lucene.util.fst.PositiveIntOutputs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class CacheFST {

	public static final int CACHEEXPIRY = 60;
	private static String FSTLOAD_DIR = "C:\\Users\\Shashidar\\Downloads"; // directory to persist FST
	private LoadingCache<String, FST<CharsRef>> transitTimeCache;
	static AmazonS3 s3;
	
	public CacheFST() {
		AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (C:\\Users\\hp\\.aws\\credentials), and is in valid format.",
                    e);
        }

        s3 = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .withRegion("us-east-2")
            .build();
	}

	/*
	 * public void buildFST(String cacheKey) throws ExecutionException {
	 * transitTimeCache.put(cacheKey, createFST(cacheKey)); }
	 */

	public void createFST(String cacheKey) throws ExecutionException {

		transitTimeCache = CacheBuilder.newBuilder().maximumSize(100) // maximum
																		// 100
																		// records

				.expireAfterAccess(CACHEEXPIRY, TimeUnit.MINUTES) // cache will expire after 30 minutes of access
				.recordStats().build(new CacheLoader<String, FST<CharsRef>>() { // build the cacheloader

					@Override
					public FST<CharsRef> load(String cacheKey) throws Exception {
						// make the expensive call
						return getFST(cacheKey);
					}
				});
		return;
	}

	private static FST<CharsRef> getFST(String cacheKey) throws IOException {
 
		String fstBucket = "buildfstbkt";
		String fstkey = getS3Key();
		S3Object response = s3.getObject(new GetObjectRequest(fstBucket, fstkey));
		
		FST<CharsRef> fst;
		Map<String, FST<CharsRef>> database = new HashMap<String, FST<CharsRef>>();
		Path p = FileSystems.getDefault().getPath(FSTLOAD_DIR);
		Directory dir = FSDirectory.open(p);

		Outputs<CharsRef> output = CharSequenceOutputs.getSingleton();

		IndexInput in = dir.openInput("BD_AIR.bin", null);
		try {
			fst = new FST<CharsRef>(in, output);
			database.put(cacheKey, fst);
		} finally {
			in.close();
		}

		return database.get(cacheKey);
	}

	private static String getS3Key() {
		// Retrieving version from Version Control Table
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build();

		DynamoDB dynamoDB = new DynamoDB(client);

		// Retrieving the instances of dynamodb tables
		Table tableVC = dynamoDB.getTable("VersionControl");
		Table tableDB = dynamoDB.getTable("dynamo234");

		// To scan VersionControl table
		ScanRequest scanRequest = new ScanRequest().withTableName("VersionControl");
		ScanResult result = client.scan(scanRequest);

		// To Store Bin file names
		ArrayList<String> binlist = new ArrayList<>();

		for (Map<String, AttributeValue> item : result.getItems()) {
			List<String> l = new ArrayList<>(item.keySet());

			if (!item.get(l.get(0)).getN().toString().equals("-1") && item.get(l.get(1)).getS().equals("IND/BD/BD_AIR/IND_IND")) {

				// Getting the latest version bin file name
				GetItemSpec spec = new GetItemSpec().withPrimaryKey("transport type", item.get(l.get(1)).getS(),
						"version", "v_" + item.get(l.get(0)).getN().toString());
				Item outcome = tableDB.getItem(spec);

				// Adding the latest version bin file name
				binlist.add(outcome.get("bin loc").toString());
			}

		}
		return binlist.get(0);
	}

	public void setCache(LoadingCache<String, FST<CharsRef>> transitTimeCacheParam) {
		this.transitTimeCache = transitTimeCacheParam;
	}

	public LoadingCache<String, FST<CharsRef>> getCache() {
		return transitTimeCache;
	}

}