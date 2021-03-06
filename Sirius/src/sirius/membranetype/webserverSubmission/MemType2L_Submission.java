package sirius.membranetype.webserverSubmission;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import sirius.utils.ClientHttpRequest;
import sirius.utils.FastaFileReader;
import sirius.utils.FastaFormat;
import sirius.utils.Utils;

/*
 */
public class MemType2L_Submission {
	
	public static void main(String[] args){
		try{
			String inputFile = Utils.selectFile("Please select the fasta file to submit to MemType2L");
			System.out.println(inputFile);
			String[] s = inputFile.split("\\" + File.separator);
			String outputDir = Utils.getDirOfFile(inputFile);
			int exceptionCount = 0;//Exception thrown but shall continue
			int notKnownCount = 0;//Not Known
			int nonMembraneCount = 0;//NOT a membrane protein
			int multiPassMembraneCount = 0;//multi-pass membrane
			int singlePassTypeICount = 0;//single-pass type I
			int singlePassTypeIICount = 0;//single-pass type II
			int singlePassTypeIIICount = 0;//single-pass type III
			int singlePassTypeIVCount = 0;//single-pass type IV
			int GPIAnchorCount = 0;//Gpi anchor membrane
			int peripheralCount = 0;//peripheral membrane
			int lipidAnchorCount = 0;//lipid anchor membrane
			String targetURL = "http://www.csbio.sjtu.edu.cn/cgi-bin/MemType.cgi";			
			FastaFileReader fileReader = new FastaFileReader(inputFile);
			List<FastaFormat> successList = new ArrayList<FastaFormat>();
			List<FastaFormat> connectionResetList = new ArrayList<FastaFormat>();
			List<FastaFormat> fastaList = fileReader.getData();
			BufferedWriter fullOutput = new BufferedWriter(new FileWriter(outputDir + s[s.length - 1] + "_MemType2L_FullOutput.txt"));
			for(int x = 0; x < fastaList.size(); x++, Thread.sleep(120000)){
				System.out.print(x + " / " + fastaList.size());
				fullOutput.write(x + " / " + fastaList.size());
				try{						
					ClientHttpRequest client = new ClientHttpRequest(targetURL);
					client.setParameter("mode", "string");
					client.setParameter("S1", fastaList.get(x).getSequence());
					//Get Response	
					InputStream is = client.post();	
					BufferedReader input = new BufferedReader(new InputStreamReader(is));
					String line; 
					String prefix = "<font size=4pt color='#5712A3' face='times new roman'>";
					while((line = input.readLine()) != null) {
						if(line.contains(prefix)){
							int startIndex = line.indexOf(prefix) + prefix.length();
							int endIndex = line.indexOf("<", startIndex);
							String resultString = line.substring(startIndex,endIndex);
							System.out.println(" - " + resultString);
							fullOutput.write(" - " + resultString);
							fullOutput.newLine();
							if(resultString.contains("NOT a membrane protein")){
								nonMembraneCount++;
							}else if(resultString.contains("multi-pass membrane")){
								multiPassMembraneCount++;
							}else if(resultString.contains("single-pass type IV")){	
								singlePassTypeIVCount++;
							}else if(resultString.contains("single-pass type III")){								
								singlePassTypeIIICount++;
							}else if(resultString.contains("single-pass type II")){
								singlePassTypeIICount++;
							}else if(resultString.contains("single-pass type I")){
								singlePassTypeICount++;
							}else if(resultString.contains("Gpi anchor membrane")){
								GPIAnchorCount++;
							}else if(resultString.contains("peripheral membrane")){
								peripheralCount++;
							}else if(resultString.contains("lipid anchor membrane")){
								lipidAnchorCount++;
							}else if(resultString.contains("Not Known")){
								notKnownCount++;
							}
						}					
					}
					successList.add(fastaList.get(x));
					input.close();	
					is.close();						
				}catch(SocketException se){
					exceptionCount++;				
					connectionResetList.add(fastaList.get(x));
					fullOutput.write(" - Socket Exception thrown but shall continue");
					fullOutput.newLine();
					System.out.println(" - Socket Exception thrown but shall continue");
				}catch(Exception e){
					exceptionCount++;
					e.printStackTrace();
					System.out.println(" - Exception thrown but shall continue");
					fullOutput.write(" - Exception thrown but shall continue");
					fullOutput.newLine();
				}
			}			
			fullOutput.close();
			int total = exceptionCount + notKnownCount + nonMembraneCount + singlePassTypeICount + 
				singlePassTypeIICount + singlePassTypeIIICount + singlePassTypeIVCount + 
				multiPassMembraneCount + lipidAnchorCount + peripheralCount + GPIAnchorCount;
			System.out.println("Exception: " + exceptionCount);
			System.out.println("Not Known: " + notKnownCount);
			System.out.println("Non Membrane: " + nonMembraneCount);
			System.out.println("SinglePass TypeI: " + singlePassTypeICount);
			System.out.println("SinglePass TypeII: " + singlePassTypeIICount);
			System.out.println("SinglePass TypeIII: " + singlePassTypeIIICount);
			System.out.println("SinglePass TypeIV: " + singlePassTypeIVCount);
			System.out.println("MultiPass: " + multiPassMembraneCount);
			System.out.println("LipidAnchor: " + lipidAnchorCount);
			System.out.println("Peripheral: " + peripheralCount);
			System.out.println("GPIAnchor: " + GPIAnchorCount);
			System.out.println("Total (Must match seqs submitted else means missing counts): " + total);
			BufferedWriter output = new BufferedWriter(new FileWriter(outputDir + s[s.length - 1] + "_MemType2L_Output.txt"));
			output.write("Exception: " + exceptionCount); output.newLine();
			output.write("Not Known: " + notKnownCount); output.newLine();
			output.write("Non Membrane: " + nonMembraneCount); output.newLine();
			output.write("SinglePass TypeI: " + singlePassTypeICount); output.newLine();
			output.write("SinglePass TypeII: " + singlePassTypeIICount); output.newLine();
			output.write("SinglePass TypeIII: " + singlePassTypeIIICount); output.newLine();
			output.write("SinglePass TypeIV: " + singlePassTypeIVCount); output.newLine();
			output.write("MultiPass: " + multiPassMembraneCount); output.newLine();
			output.write("LipidAnchor: " + lipidAnchorCount); output.newLine();
			output.write("Peripheral: " + peripheralCount); output.newLine();
			output.write("GPIAnchor: " + GPIAnchorCount); output.newLine();
			output.write("Total (Must match seqs submitted else means missing counts): " + total); output.newLine();
			output.write("Submitted: " + fastaList.size()); output.newLine();
			output.close();
			output = new BufferedWriter(new FileWriter(outputDir + s[s.length - 1] + "_MemType2L_Exception.txt"));
			for(FastaFormat f:connectionResetList){
				output.write(f.getHeader());
				output.newLine();
				output.write(f.getSequence());
				output.newLine();
			}
			output.close();
			output = new BufferedWriter(new FileWriter(outputDir + s[s.length - 1] + "_MemType2L_Success.txt"));
			for(FastaFormat f:successList){
				output.write(f.getHeader());
				output.newLine();
				output.write(f.getSequence());
				output.newLine();
			}
			output.close();
		}catch(Exception e){e.printStackTrace();}
	}
}
