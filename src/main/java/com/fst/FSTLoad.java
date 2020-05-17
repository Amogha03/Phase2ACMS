package com.fst;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.IntsRefBuilder;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.CharSequenceOutputs;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.Util;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.model.S3Object;

import org.apache.lucene.util.fst.Outputs;
import java.util.UUID;


public class FSTLoad {
	public FST<CharsRef> fstBuild(S3Object response) throws IOException {

		final String FST_FILE = UUID.randomUUID().toString(); // random file name for FST

		Outputs<CharsRef> output = CharSequenceOutputs.getSingleton();
		Builder<CharsRef> builder1 = new Builder<>(FST.INPUT_TYPE.BYTE1, output);
		
		CharsRef value;

        BufferedReader br = new BufferedReader(new InputStreamReader(response.getObjectContent()));
		String line;
		while ((line = br.readLine()) != null) {
			line=line.substring(1,line.length()-1);
			String column [] = line.split(",");
			BytesRefBuilder scratchBytes = new BytesRefBuilder();
			IntsRefBuilder scratchInts = new IntsRefBuilder();

			scratchBytes.copyChars(column[0]+column[1]);
			//System.out.println(line);
			// TODO - adding a string instead of literal blow up FST size, weird "java"
			builder1.add(Util.toIntsRef(scratchBytes.toBytesRef(), scratchInts), new CharsRef(column[2]));
		}
		br.close();

		FST<CharsRef> fstMemory = builder1.finish();
		
		/*System.out.println("Retrieval from FST in memory:");
		value = Util.get(fstMemory, new BytesRef("110017535161"));
		System.out.println(value);

		value = Util.get(fstMemory, new BytesRef("110078423303"));
		System.out.println(value);

		IndexOutput out = dir.createOutput(FST_FILE + ".bin", null);
		fstMemory.save(out);
		out.close();*/
		
		return fstMemory;
	}
}