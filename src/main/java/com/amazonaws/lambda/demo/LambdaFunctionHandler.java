package com.amazonaws.lambda.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.BytesReader;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.regions.Regions;

import com.fst.FSTLoad;

public class LambdaFunctionHandler implements RequestHandler<S3Event, String> {

	private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

	public LambdaFunctionHandler() {
	}

	// Test purpose only.
	LambdaFunctionHandler(AmazonS3 s3) {
		this.s3 = s3;
	}

	public String handleRequest(S3Event event, Context context) {
		context.getLogger().log("Received event: " + event);

		// Get the object from the event and show its content type
		String bucket = event.getRecords().get(0).getS3().getBucket().getName();
		String key = event.getRecords().get(0).getS3().getObject().getKey();

		final String FST_FILE = UUID.randomUUID().toString(); // random file name for FST

		String dstBucket = "fstbucket234";
		String dstKey = FST_FILE + ".bin";

		try {
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();
			context.getLogger().log("CONTENT TYPE: " + contentType);
			context.getLogger().log("path of s3 object: " + response.getRedirectLocation() + " " + response.toString());

			/*
			 * BufferedReader br = new BufferedReader(new
			 * InputStreamReader(response.getObjectContent())); // Calculate grade String
			 * csvOutput,csvResult=""; while ((csvOutput = br.readLine()) != null) {
			 * csvResult+=csvOutput+" "; }
			 */
			if (contentType.equals("text/csv")) {
				FSTLoad fl = new FSTLoad();
				FST<CharsRef> fst = fl.fstBuild(response);

				boolean status = new File("/tmp/fst/").mkdirs();
				if (status == true) {
					// File targetFile = File.createTempFile(FST_FILE , ".bin",new File("/tmp/"));

					Path p = FileSystems.getDefault().getPath("/tmp/fst/");
					Directory dir = FSDirectory.open(p);
					context.getLogger().log("dir created " + dir.toString());

					IndexOutput out = dir.createOutput(FST_FILE + ".bin", null);
					fst.save(out);
					out.close();

					context.getLogger()
							.log("out created " + out.getName() + " " + out.toString() + " " + out.getFilePointer());

				}

				ClassLoader classLoader = getClass().getClassLoader();
				File cityFile = new File("/tmp/fst/" + dstKey);

				System.out.println("Writing to: " + dstBucket + "/" + dstKey);
	
				try {
					s3.putObject(dstBucket, dstKey, cityFile);
				} catch (AmazonServiceException e) {
					System.err.println(e.getErrorMessage());
					System.exit(1);
				}
				System.out.println("Successfully and uploaded to " + dstBucket + "/" + dstKey);
				
				//retriving version from Version Control Table
//				AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//			            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000/", "us-east-2"))
//			            .build();
				AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
						.withRegion(Regions.US_EAST_2)
						.build();

			        DynamoDB dynamoDB = new DynamoDB(client);

			        Table table = dynamoDB.getTable("VersionControl");

			        String transport_type = "IND/BD/BD_AIR/IND_IND";

			        GetItemSpec spec = new GetItemSpec().withPrimaryKey("transport type", transport_type);
			        

			        try {
			            System.out.println("Attempting to read the item...");
			            Item outcome = table.getItem(spec);
			            
			            String s=outcome.get("version").toString();
			            int x=Integer.parseInt(s);
			            System.out.println("incremented version: " + x);
			            x++;
			            
			            UpdateItemSpec updateItemSpec = new UpdateItemSpec().withPrimaryKey("transport type", transport_type)
				                .withUpdateExpression("set version =:v")
				                .withValueMap(new ValueMap().withNumber(":v", x))
				                .withReturnValues(ReturnValue.UPDATED_NEW);
			            
			            System.out.println("update succeeded: " + updateItemSpec);
			            UpdateItemOutcome upoutcome = table.updateItem(updateItemSpec);
			            
			        }
			        catch (Exception e) {
			            System.err.println("Unable to read item: " + transport_type);
			            System.err.println(e.getMessage());
			        }

			}
			
			
			
			
			return contentType;

		} catch (Exception e) {
			e.printStackTrace();
			context.getLogger().log(String.format("Error getting object %s from bucket %s. Make sure they exist and"
					+ " your bucket is in the same region as this function.", key, bucket));
			return e.toString();
		}
		
		
		
		
		
		
	}
}



