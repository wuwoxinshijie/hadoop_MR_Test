package org.cyt.hadoop.day0814;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Map side join for small file using distributed cache.
 * @author zkpk
 */
public class MapJoin extends Configured implements Tool {

	public static class JoinMapper extends Mapper<Object, Text, Text, Text> {
		private Hashtable<String, String> joinData = new Hashtable<String, String>();

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			// TODO Auto-generated method stub
			try {
				//parse cache file
				Path[] cacheFiles = DistributedCache.getLocalCacheFiles(context.getConfiguration());
				if (cacheFiles != null && cacheFiles.length > 0) {
					String line;
					String[] tokens;
					BufferedReader joinReader = new BufferedReader(
							new FileReader(cacheFiles[0].toString()));
					try {
						while ((line = joinReader.readLine()) != null) {
							tokens = line.split("\t", -1);
							//add data to collection
							joinData.put(tokens[0], tokens[1]);
						}
					} finally {
						joinReader.close();
					}
				}
			} catch (IOException e) {
				System.err.println("Exception reading DistributedCache: " + e);
			}
		}
		

		@Override
		protected void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String [] arr=value.toString().split("\t",-1);
			// TODO Auto-generated method stub
			String joinValue = joinData.get(arr[1]);
			if(joinValue!=null){
				context.write(new Text(arr[1]),new Text(arr[2]));
			}
			
		}


		
	}

	public int run(String[] args) throws Exception {
		Configuration conf = getConf();
		//using DistributedCache
		DistributedCache.addCacheFile(new Path("/test/uid-sample.txt").toUri(), conf);

		Job job = new Job(conf);
		job.setJobName("MapJoin with DistributedCache");
		job.setJarByClass(MapJoin.class);
		job.setMapperClass(JoinMapper.class);
		job.setNumReduceTasks(0);
		//job.setInputFormatClass(KeyValueTextInputFormat.class);
		//job.setOutputFormatClass(TextOutputFormat.class);
		//job.set("key.value.separator.in.input.line", "\t");
		Path in = new Path(args[0]);
		Path out = new Path(args[1]);
		FileInputFormat.setInputPaths(job, in);
		FileOutputFormat.setOutputPath(job, out);
		return job.waitForCompletion(true)?0:1;
	}

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new MapJoin(), args);
		System.exit(res);
	}

}
