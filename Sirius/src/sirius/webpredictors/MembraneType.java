package sirius.webpredictors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.List;

import sirius.membranetype.MembraneTypePrediction;
import sirius.predictor.main.ClassifierData;
import sirius.predictor.main.SiriusClassifier;
import sirius.trainer.features.Feature;
import sirius.utils.FastaFileReader;
import sirius.utils.FastaFormat;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;

public class MembraneType {
	public static void main(String[] args){
		try{
			String[] memtypeString = new String[9];
			memtypeString[0] = "NonMembrane715";
			memtypeString[1] = "TypeI";
			memtypeString[2] = "TypeII";
			memtypeString[3] = "TypeIII";
			memtypeString[4] = "TypeIV";
			memtypeString[5] = "MultiPass";
			memtypeString[6] = "LipidAnchor";
			memtypeString[7] = "GPIAnchor";
			memtypeString[8] = "Peripheral";
			int choice = Integer.parseInt(args[0]);
			switch(choice){			
			case 0: loadAndDisplayClassifierII(memtypeString, Integer.parseInt(args[1]), Integer.parseInt(args[2])); break;
			case 1: runPredictionII(memtypeString, args[1]); break;
			}			
		}catch(Exception e){
			e.printStackTrace();
			try {
				PrintStream eOutput = new PrintStream(new FileOutputStream(
						"." + File.separator + "log/error.txt", true));				
				e.printStackTrace(eOutput);
				eOutput.close();
			} catch (IOException e1) {				
			}
		}
	}
	
	public static void loadAndDisplayClassifierII(String[] memtypeString, int classifierNum, int numOfClassifiers) throws Exception{		
		/*		 
		 * Load the requested classifier and display all it in rules		 
		 */		
		//String classifierDir = Utils.selectDirectory("Select Classifier Dir");
		String classifierDir = "./classifiers/membranetype/";
		if(classifierNum == -1){
			/*
			 * Load and display all
			 */
			for(int i = 0; i < memtypeString.length; i++){
				displayClassifier(classifierDir, memtypeString, numOfClassifiers, i);
			}
		}else{
			/*
			 * Load and display specific
			 */						
			displayClassifier(classifierDir, memtypeString, numOfClassifiers, classifierNum);
		}
	}
	
	private static void displayClassifier(String classifierDir, String[] memtypeString, int numOfClassifiers, int i)
		throws Exception{
		for(int j = 0; j < numOfClassifiers; j++){
			String name = memtypeString[i] + "_" + j;
			ClassifierData cData = 
				SiriusClassifier.loadClassifier(
						classifierDir + name + "_J48.classifierone");
			if(i == 0){
				System.out.println("NonMembrane (pos) vs Membrane (neg)");
			}else{
				System.out.println(memtypeString[i] + " (pos) vs Non" + memtypeString[i] + " (neg)");
			}
//			((J48)(cData.getClassifierOne())).toStringAHFU();
			((J48)(cData.getClassifierOne())).toString();
		}		
	}
	
	public static void runPredictionII(String[] memtypeString, String fastaFilename) throws Exception{
		//String classifierDir = Utils.selectDirectory("Select Classifier Dir");
		String classifierDir = "./classifiers/membranetype/";
		//String featureDir = Utils.selectDirectory("Select Feature Dir");
		String featureDir = "./features/membranetype/";
		//String fastaFile = Utils.selectFile("Select Fasta File");		
		String fastaFile = "./input/" + fastaFilename;
				
		/*
		 * Settings
		 */
		int numOfClassifiers = 1;
		String classifierName = "J48";
		boolean useVoting = false;
		double layerOneThreshold = 0.5;
		double votingThreshold = 0.5;
		/*
		 * Load Classifiers and Features
		 */		
		Hashtable<String, Classifier> classifierHashtable = new Hashtable<String, Classifier>();
		Hashtable<String, List<Feature>> featureHashtable = new Hashtable<String, List<Feature>>();
		for(String s:memtypeString){
			for(int a = 0; a < numOfClassifiers; a++){
				String name = s + "_" + a;
				classifierHashtable.put(name, SiriusClassifier.loadClassifier(
						classifierDir + name + "_" + classifierName + ".classifierone").getClassifierOne());
				featureHashtable.put(name, Feature.loadSettings(featureDir + name + ".features"));
			}
		}
		/*
		 * Run Classifiers
		 */
		List<FastaFormat> fastaList = FastaFileReader.readFastaFile(fastaFile);
		List<MembraneTypePrediction> pList = SiriusClassifier.predictMembraneType(memtypeString, classifierHashtable, 
				numOfClassifiers, featureHashtable, fastaList, useVoting, layerOneThreshold, votingThreshold);
		for(int x = 0; x < fastaList.size(); x++){
			System.out.println(fastaList.get(x).getHeader());
			System.out.println(fastaList.get(x).peekSequence());
			System.out.println(pList.get(x).toStringWithRulesWithFeatureValuesEmbedded());
		}
	}
	
}