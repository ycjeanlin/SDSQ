/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.tool.driver;

import net.librec.conf.Configuration;
import net.librec.data.model.ArffAttribute;
import net.librec.job.RecommenderJob;
import net.librec.tool.LibrecTool;
import net.librec.util.DriverClassUtil;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * RecDriver
 *
 * @author WangYuFeng
 */
public class RecDriver implements LibrecTool {
	
	/**
     * Execute the command with the given arguments.
     *
     * @param args command specific arguments.
     * @return exit code.
     * @throws Exception if error occurs
     */
    public int run(String[] args) throws Exception{
    	long startTime = System.currentTimeMillis();
		long endTime;
		double totalTime;
		int p=2;
		
        // init options
        Options options = new Options();
        options.addOption("build", false, "build model");
        options.addOption("load", false, "load model");
        options.addOption("save", false, "save model");
        options.addOption("exec", false, "run job");
        options.addOption("conf", true, "the path of configuration file");
        options.addOption("jobconf", true, "a specified key-value pair for configuration");
        options.addOption("D", true, "a specified key-value pair for configuration");
        options.addOption("datapath", true, "the path to the raw dataset");
        options.addOption("p", true, "constraints on number of users");
        
        // parse options
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, false);
        // init configuration
        Configuration conf = new Configuration();

        
        if (cmd.hasOption("conf")) {
            String confFilePath = cmd.getOptionValue("conf");
            Properties prop = new Properties();
            prop.load(new FileInputStream(confFilePath));
            for (String name : prop.stringPropertyNames()) {
                conf.set(name, prop.getProperty(name));
                System.out.println(name+ ", "+ prop.getProperty(name));
            }
        }
        
        if (cmd.hasOption("p")) {
            p = Integer.parseInt(cmd.getOptionValue("p"));
        }
        
        if (cmd.hasOption("jobconf")) {
            String[] optionValues = cmd.getOptionValues("jobconf");
            for (String optionValue : optionValues) {
                String[] keyValuePair = optionValue.split("=");
                conf.set(keyValuePair[0], keyValuePair[1]);
            }
            
        }
        
        if (cmd.hasOption("D")) {
            String[] optionValues = cmd.getOptionValues("D");
            for (String optionValue : optionValues) {
                String[] keyValuePair = optionValue.split("=");
                conf.set(keyValuePair[0], keyValuePair[1]);
            }
        }
        
        if (!(cmd.hasOption("D") || cmd.hasOption("conf"))){
        	//Default: run item-based CF
        	Properties prop = new Properties();
            prop.load(new FileInputStream("./src/main/resources/rec/cf/itemknn-testranking.properties"));
            for (String name : prop.stringPropertyNames()) {
                conf.set(name, prop.getProperty(name));
            }
        }
        
        if(cmd.hasOption("datapath")){
        	String optionValues = cmd.getOptionValue("datapath");
        	parseInputData(optionValues);
        }
        
        //run job
        RecommenderJob job = new RecommenderJob(conf);
        job.runJob();
        
        //Feasibility
        endTime = System.currentTimeMillis();
		totalTime = (endTime - startTime)/1000.0;
		
		String outputPath = "./list.txt";
		calFeasibility(outputPath, totalTime, p);
		
        System.out.print("Finished");
        return 0;
    }

    private void calFeasibility(String outputPath, double time, int p) throws IOException {
    	ArrayList<String> user = new ArrayList<String>();
    	HashMap<String, Integer> item_set = new HashMap<String,Integer>();
    	BufferedReader br = new BufferedReader(new FileReader(outputPath));
    	HashMap<String, Double> diversity = new HashMap<String, Double>();
    	
    	String line = null;
        String[] data = null;
    	while (true) {
    		line = br.readLine();
    		if (line == null) // finish reading
                break;
       	 	if (line.isEmpty() || line.startsWith("%")) // skip empty or
                // annotation
                continue;
       	 	
       	 	data = line.trim().split("[, \t]");
       	 	if(item_set.containsKey(data[1])){
       	 		item_set.put(data[1], item_set.get(data[1])+1);
			}else{
				item_set.put(data[1], 1);
			}
       	 	user.add(data[0]);
    	}
    	br.close();
    	
    	br = new BufferedReader(new FileReader("./temp_div.txt"));
    	while (true) {
    		line = br.readLine();
    		if (line == null) // finish reading
                break;
       	 	if (line.isEmpty() || line.startsWith("%")) // skip empty or
                // annotation
                continue;
       	 	
       	 	data = line.trim().split("[, \t]");
       	 	String key1 = data[0] + "," + data[1];
       	 	if(!diversity.containsKey(key1)){
       	 		diversity.put(key1, Double.parseDouble(data[2]));
			}
    	}
    	br.close();
    	
    	double num_items = item_set.size();
    	double valid_items = 0.0;
    	for (Map.Entry<String, Integer> pair : item_set.entrySet()) {
    		System.out.println(pair.getValue());
    	    if (pair.getValue() >= p){
    	    	valid_items += 1;
    	    }
    	}
    	
    	double totalDiv = 0.0;
    	for (Map.Entry<String, Double> pair : diversity.entrySet()) {
    		String[] items = pair.getKey().trim().split("[, \t]");
    	    if (item_set.containsKey(items[0]) && item_set.containsKey(items[1])){
    	    	totalDiv += pair.getValue();
    	    }
    	}
    	
    	BufferedWriter bw = new BufferedWriter(new FileWriter("../result/performance.csv"));
		bw.write("Time,"+Double.toString(time));
		bw.newLine();
		bw.write("Channel Ratio,"+Double.toString((double)(valid_items/num_items)));
		bw.newLine();
		bw.write("Viewer Ratio,"+Double.toString((double)(100/100)));
		bw.newLine();
		bw.write("Objective Value,"+Double.toString((double)(totalDiv/num_items)));
		bw.newLine();
		
		bw.close();
		
	}

	private void parseInputData(String dataPath) throws IOException {
		System.out.println(dataPath);
		BufferedReader br = new BufferedReader(new FileReader(dataPath));
        boolean dataFlag = false;
        boolean arrive_II = false;

        String line = null;
        String[] data = null;
        ArrayList<String> newOutputUIR = new ArrayList<String>();
        ArrayList<String> newOutputDiv = new ArrayList<String>();

        while (true) {
        	 line = br.readLine();
        	 if (line == null) // finish reading
                 break;
        	 if (line.isEmpty() || line.startsWith("%")) // skip empty or
                 // annotation
                 continue;
        	 
        	 data = line.trim().split("[ \t]");
        	 
             if(data.length == 3){
            	 arrive_II = true;
             }else if(arrive_II && data.length == 2) {
            	 arrive_II = false;
            	 dataFlag = true;
             }
             
        	 // parse DATA if valid
             if (dataFlag) {
            	 // parse RELATION
            	 String newLine = "";
            	 newLine = newLine.concat(data[1] + " " + data[0] + " 1");
            	 newOutputUIR.add(newLine);
             }
             
             if(arrive_II){
            	 // parse RELATION
            	 String newLine = "";
            	 newLine = newLine.concat(data[0] + " " + data[1] + " " + data[2]);
            	 newOutputDiv.add(newLine);
             }

         }
         br.close();
         
         BufferedWriter bw = new BufferedWriter(new FileWriter("./temp_div.txt"));
         for(String l:newOutputDiv){
        	 bw.write(l);
             bw.newLine();
         }
         bw.close();
         
         bw = new BufferedWriter(new FileWriter("../data/SDSQ/subcribe.txt"));
         for(String l:newOutputUIR){
        	 bw.write(l);
             bw.newLine();
         }
         bw.close();
	}


	public static void main(String[] args) throws Exception {
        LibrecTool tool = new RecDriver();
        
        Options options = new Options();
        options.addOption("build", false, "build model");
        options.addOption("load", false, "load model");
        options.addOption("save", false, "save model");
        options.addOption("exec", false, "run job");
        options.addOption("conf", true, "the path of configuration file");
        options.addOption("jobconf", true, "a specified key-value pair for configuration");
        options.addOption("D", true, "a specified key-value pair for configuration");
        options.addOption("datapath", true, "the path to the raw dataset");
        
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        if (cmd.hasOption("build")) {
            ;
        } else if (cmd.hasOption("load")) {
            ;
        } else if (cmd.hasOption("save")) {
            ;
        } else if (cmd.hasOption("exec")) {
            tool.run(args);
        }
        
		
        //System.out.println("Working Directory = " + System.getProperty("user.dir"));
    }
}
