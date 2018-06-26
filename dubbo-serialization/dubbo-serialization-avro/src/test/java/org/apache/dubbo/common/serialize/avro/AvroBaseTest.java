package org.apache.dubbo.common.serialize.avro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AvroBaseTest {
	private static InputStream schema_file;
	private static Schema schema;
	private static File file;

	@BeforeClass
	public static void beforeClass() throws IOException {
		schema_file=AvroBaseTest.class.getResourceAsStream("/user.avsc");
		schema = new Schema.Parser().parse(schema_file);
		file = new File("users.avro");
	}

	@Test
	public void serializeTest() throws IOException {
		GenericRecord user1 = new GenericData.Record(schema);
		user1.put("name", "Alyssa");
		user1.put("favorite_number", 256);
		// Leave favorite color null
		GenericRecord user2 = new GenericData.Record(schema);
		user2.put("name", "Ben");
		user2.put("favorite_number", 7);
		user2.put("favorite_color", "red");

		// Serialize user1 and user2 to disk
		File file = new File("users.avro");
		System.out.println(file.getAbsolutePath());
		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
		DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
		dataFileWriter.create(schema, file);
		dataFileWriter.append(user1);
		dataFileWriter.append(user2);
		dataFileWriter.close();
	}

	@Test
	public void deserializeTest() throws IOException {
		// Deserialize users from disk
		DatumReader<GenericRecord> datumReader = new GenericDatumReader<GenericRecord>(schema);
		DataFileReader<GenericRecord> dataFileReader = new DataFileReader<GenericRecord>(file, datumReader);
		GenericRecord user = null;
		while (dataFileReader.hasNext()) {
			// Reuse user object by passing it to next(). This saves us from
			// allocating and garbage collecting many objects for files with
			// many items.
			user = dataFileReader.next(user);
			System.out.println(user);
		}
	}

	@AfterClass
	public static void afterClass(){
		if(schema_file!=null){
			try {
				schema_file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			schema_file=null;
		}
		if(file!=null){
			file.deleteOnExit();
		}
	}
}
