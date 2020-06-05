package com.kmit.transitsearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import org.apache.lucene.util.fst.Outputs;
import java.util.UUID;


public class FSTLoad {
	public static void main(String[] args) throws IOException {

		//final String LOAD_FILENAME = "../projectData/excel/kar_guj_5m.csv"; //file name with csv records
		final String LOAD_FILENAME = "C:\\Users\\Shashidar\\Downloads\\UPS_Road.csv"; //file name with csv records
		final String FSTLOAD_DIR = "C:\\Users\\Shashidar\\Downloads"; // directory to persist FST
		final String FST_FILE = UUID.randomUUID().toString(); // random file name for FST

		Outputs<CharsRef> output = CharSequenceOutputs.getSingleton();
		Builder<CharsRef> builder1 = new Builder<>(FST.INPUT_TYPE.BYTE1, output);
		Path p = FileSystems.getDefault().getPath(FSTLOAD_DIR);
		Directory dir = FSDirectory.open(p);
		CharsRef value;
		
		BufferedReader br = new BufferedReader(new FileReader(LOAD_FILENAME));
		String line;
		while ((line = br.readLine()) != null) {

			String column [] = line.split(",");
			BytesRefBuilder scratchBytes = new BytesRefBuilder();
			IntsRefBuilder scratchInts = new IntsRefBuilder();

			scratchBytes.copyChars(column[0]);
			//int i = Integer.valueOf(column[1]);
			builder1.add(Util.toIntsRef(scratchBytes.toBytesRef(), scratchInts), new CharsRef(column[1]));		
		}
		br.close();

		FST<CharsRef> fstMemory = builder1.finish();
		System.out.println(fstMemory.ramBytesUsed());

		System.out.println("Retrieval from FST in memory:");
		value = Util.get(fstMemory, new BytesRef("360001360026"));
		System.out.println(value);

		value = Util.get(fstMemory, new BytesRef("560004574114"));
		System.out.println(value);

		value = Util.get(fstMemory, new BytesRef("591111574279"));
		System.out.println(value);

		IndexOutput out = dir.createOutput(FST_FILE + ".bin", null);
		fstMemory.save(out);
		out.close();

		FST<CharsRef> fstDisk;
		IndexInput in = dir.openInput(FST_FILE + ".bin", null);
		try {
			fstDisk = new FST<CharsRef>(in, output);

			System.out.println("Retrieval from FST from disk:");

			value = Util.get(fstDisk, new BytesRef("396386000000"));
			System.out.println(value);

			value = Util.get(fstDisk, new BytesRef("560004574114"));
			System.out.println(value);

			value = Util.get(fstDisk, new BytesRef("591111574279"));
			System.out.println(value);

			value = Util.get(fstDisk, new BytesRef("3600013600251A"));
			System.out.println(value);

		} finally {
			in.close();
			//dir.deleteFile(FST_FILE + ".bin"); //DO NOT DELETE
		}		
	}
}