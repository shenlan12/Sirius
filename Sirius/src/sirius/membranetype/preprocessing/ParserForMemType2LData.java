package sirius.membranetype.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import sirius.utils.Utils;

public class ParserForMemType2LData {
	/*
	 * First, I used pdf box from Apache to convert the supplementary data from pdf to txt
	 * Then I will be using this class to parse them
	 */
	
	public static void parseMemType2LData(){
		try{			
			String textFile = Utils.selectFile("Please select MemType2L supp data in text format");
			String outputDir = Utils.selectDirectory("Please select output directory");
			BufferedReader input = new BufferedReader(new FileReader(textFile));
			String line;
			BufferedWriter output = null;
			//BufferedWriter output = new BufferedWriter(new FileWriter(outputDir + "A.fasta"));
			while((line = input.readLine()) != null){
				char firstChar = line.charAt(0);
				switch(firstChar){
				case ' ': break;//page number from pdf file - do nothing				
				case '(':
					int index = line.indexOf(")");
					String outputFilename = line.substring(index + 1).trim() + ".fasta";
					if(output != null) output.close();
					output = new BufferedWriter(new FileWriter(outputDir + outputFilename));
					break;//information about the sequences to follow
				case '>': //header file
				default:
					output.write(line);
					output.newLine();
					break;//sequence
				}
			}
			input.close();
			if(output != null) output.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		parseMemType2LData();
	}
}
