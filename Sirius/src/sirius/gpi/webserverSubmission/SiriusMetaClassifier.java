package sirius.gpi.webserverSubmission;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sirius.predictor.main.ClassifierData;
import sirius.predictor.main.SiriusClassifier;
import sirius.trainer.features.Feature;
import sirius.utils.Arff;
import sirius.utils.FastaFileReader;
import sirius.utils.FastaFormat;
import sirius.utils.PredictionStats;
import sirius.utils.Utils;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class SiriusMetaClassifier {
	public static void main(String[] args){
		try{
			trainClassifierTuneThemAndRunThemOnValidationData();
		}catch(Exception e){e.printStackTrace();}		
	}
	
	public static void trainClassifierTuneThemAndRunThemOnValidationData() throws Exception{
		/*
		 * Steps Needed
		 * 1) Build classifiers using training data
		 * 2) Tune them using x-validation on training data
		 * 3) Combine them into voting or averaging classifier
		 * 4) Finally run them on validation data and output the scores file
		 * PS: Use ScoreFileComputation to do neccessary post predictions
		 */
		/*
		 * Inputs required
		 * 1) Directory which contains all the features files
		 * 2) Pos and Neg Training Data
		 * 3) Pos and Neg Validation Data
		 *
		 * Settings required
		 * 1) 5 or 10 fold X-Validation or 
		 * 	should I build using full training data and tune on full training data?
		 * 
		 */		
		/*
		 * Obtain neccessary input from User
		 */
		int numOfFeatureSet = 10;
		System.out.println("Obtain Input");
		String featureDir = Utils.selectDirectory("Please select dir with feature files");
		String posTrainingFile = Utils.selectFile("Please select Pos Training file");
		String negTrainingFile = Utils.selectFile("Please select Neg Training file");
		String posValidationFile = Utils.selectFile("Please select Pos Validation file");
		String negValidationFile = Utils.selectFile("Please select Neg Validation file");		
		/*
		 * Load Training Sequences
		 */
		System.out.println("Load Training");
		List<FastaFormat> posTrainingFastaList = new FastaFileReader(posTrainingFile).getData();
		List<FastaFormat> negTrainingFastaList = new FastaFileReader(negTrainingFile).getData();
		List<FastaFormat> fullTrainingFastaList = new ArrayList<FastaFormat>();
		fullTrainingFastaList.addAll(posTrainingFastaList);
		fullTrainingFastaList.addAll(negTrainingFastaList);
		double[] trainingClassList = new double[fullTrainingFastaList.size()];
		for(int x = 0; x < fullTrainingFastaList.size(); x++){
			if(x < posTrainingFastaList.size()) trainingClassList[x] = 0.0;
			else trainingClassList[x] = 1.0;
		}
		/*
		 * Load Validation Sequences
		 */
		System.out.println("Load Validation");
		List<FastaFormat> posValidationFastaList = new FastaFileReader(posValidationFile).getData();
		List<FastaFormat> negValidationFastaList = new FastaFileReader(negValidationFile).getData();
		List<FastaFormat> fullValidationFastaList = new ArrayList<FastaFormat>();
		fullValidationFastaList.addAll(posValidationFastaList);
		fullValidationFastaList.addAll(negValidationFastaList);
		double[] validationClassList = new double[fullValidationFastaList.size()];
		for(int x = 0; x < fullValidationFastaList.size(); x++){
			if(x < posValidationFastaList.size()) validationClassList[x] = 0.0;
			else validationClassList[x] = 1.0;
		}
		/*
		 * Load Features
		 */
		System.out.println("Load Features");
		List<List<Feature>> featureListList = new ArrayList<List<Feature>>();		
		for(int x = 0; x < numOfFeatureSet; x++){
			featureListList.add(Feature.loadSettings(featureDir + x + File.separator + 
					"maxScoreFeature.features"));
		}
		/*
		 * Obtain features' values for training sequences
		 */
		System.out.println("Obtain Training Values");
		for(int x = 0; x < featureListList.size(); x++){
			System.out.println(x + " / " + featureListList.size());
			SiriusClassifier.obtainFeatureValue(featureListList.get(x), fullTrainingFastaList);
		}
		/*
		 * Train Classifiers
		 */
		int numFolds = 10;		
		Random random = new Random(0);
		System.out.println("Train Classifiers");
		List<Classifier> classifierList = new ArrayList<Classifier>();
		List<Double> thresholdList = new ArrayList<Double>();
		for(int x = 0; x < featureListList.size(); x++){
			System.out.println(x + " / " + featureListList.size());
			File f = SiriusClassifier.generateArffFromFeature(featureListList.get(x), trainingClassList);
			Instances trainInstances = Arff.getAsInstances(f);		
			trainInstances.setClassIndex(trainInstances.numAttributes() - 1);			
			J48 j48 = new J48();
			j48.setUseLaplace(true);
			j48.buildClassifier(trainInstances);
			classifierList.add(j48);
			/*
			 * Use X-Validation to obtain the threshold for voting
			 */
			File tempFile = File.createTempFile("SiriusMetaClassifier_", ".scores");
			BufferedWriter tempOutput = new BufferedWriter(new FileWriter(tempFile));
			trainInstances.stratify(numFolds);
			List<Integer> classList = new ArrayList<Integer>();
			List<Double> predictionList = new ArrayList<Double>();
			for(int fold = 0; fold < numFolds; fold++){							
				Instances train = trainInstances.trainCV(numFolds, fold, random);
				J48 j = new J48();
				j.setUseLaplace(true);
				j.buildClassifier(train);
				Instances test = trainInstances.testCV(numFolds, fold);
				for(int i = 0; i < test.numInstances(); i++){					
					classList.add((int)test.instance(i).classValue());
					predictionList.add(j.distributionForInstance(test.instance(i))[0]);
				}
				PredictionStats stats = new PredictionStats(classList, predictionList, 0.5);
				thresholdList.add(stats.getMaxMCCThreshold());
			}
			tempOutput.close();
		}		
		/*
		 * Overwrite features' values for validation sequences
		 */
		System.out.println("Obtain validation values");
		List<Instances> instancesList =  new ArrayList<Instances>();
		for(int x = 0; x < featureListList.size(); x++){
			System.out.println(x + " / " + featureListList.size());
			SiriusClassifier.obtainFeatureValue(featureListList.get(x), fullValidationFastaList);
			File f = SiriusClassifier.generateArffFromFeature(featureListList.get(x), validationClassList);
			Instances validationInstances = Arff.getAsInstances(f);
			validationInstances.setClassIndex(validationInstances.numAttributes() - 1);
			instancesList.add(validationInstances);
		}		
		/*
		 * Obtain score file of the validation file
		 */
		System.out.println("Running classifiers on validation file");
		List<BufferedWriter> outputAggregateList = new ArrayList<BufferedWriter>();
		List<BufferedWriter> outputVotingList = new ArrayList<BufferedWriter>();
		for(int x = 0; x < numOfFeatureSet; x++){
			outputAggregateList.add(new BufferedWriter(new FileWriter(featureDir + 
					"Aggregate_0_to_" + x + ".scores")));
			outputVotingList.add(new BufferedWriter(new FileWriter(featureDir + 
					"Voting_0_to_" + x + ".scores")));
		}
		for(int x = 0; x < fullValidationFastaList.size(); x++){
			System.out.println(x + " / " + fullValidationFastaList.size());
			for(int y = 0; y < numOfFeatureSet; y++){
				outputAggregateList.get(y).write(fullValidationFastaList.get(x).getHeader());
				outputAggregateList.get(y).newLine();
				outputAggregateList.get(y).write(fullValidationFastaList.get(x).getSequence());
				outputAggregateList.get(y).newLine();
				
				outputVotingList.get(y).write(fullValidationFastaList.get(x).getHeader());
				outputVotingList.get(y).newLine();
				outputVotingList.get(y).write(fullValidationFastaList.get(x).getSequence());
				outputVotingList.get(y).newLine();
				
				if(validationClassList[x] == 0.0){
					outputAggregateList.get(y).write("pos,0=");
					outputVotingList.get(y).write("pos,0=");
				}else{
					outputAggregateList.get(y).write("neg,0=");
					outputVotingList.get(y).write("neg,0=");
				}
			}
						
			double[] score = new double[numOfFeatureSet];
			double[] vote = new double[numOfFeatureSet];
			for(int y = 0; y < classifierList.size(); y++){
				double currentScore =
					classifierList.get(y).distributionForInstance(instancesList.get(y).instance(x))[0];
				for(int z = 0; z < numOfFeatureSet; z++){
					if(z >= y){
						score[z] += currentScore;
						if(currentScore >= thresholdList.get(y)){
							vote[z]++;
						}
					}
				}
			}			
			for(int y = 0; y < numOfFeatureSet; y++){
				//Aggregate
				score[y] /= (y+1);				
				outputAggregateList.get(y).write(score[y] + "");
				outputAggregateList.get(y).newLine();
				//Voting			
				vote[y] /= (y+1);
				outputVotingList.get(y).write(vote[y] + "");
				outputVotingList.get(y).newLine();
			}
		}
		for(int x = 0; x < numOfFeatureSet; x++){
			outputAggregateList.get(x).close();
			outputVotingList.get(x).close();
		}		
	}
	
	public static void testOnValidationDataUsingBuiltClassifier(){
		/*
		 * Take X classifiers built using training data and run them through the validation data
		 * and output the scores files
		 * which requires ScoreFileComputation.java to sort
		 */
		String classifierDirectory = Utils.selectDirectory("Please select the classifiers directory");
		//String posTrainingFastaFile = Utils.selectFile("Please select pos training fasta file");
		//String negTrainingFastaFile = Utils.selectFile("Please select neg training fasta file");
		String posValidationFastaFile = Utils.selectFile("Please select pos validation fasta file");
		String negValidationFastaFile = Utils.selectFile("Please select neg validation fasta file");
		String outputDirectory = Utils.selectDirectory("Please select output directory");
		
		FastaFileReader posValidationReader = new FastaFileReader(posValidationFastaFile);
		FastaFileReader negValidationReader = new FastaFileReader(negValidationFastaFile);
		List<FastaFormat> posValidationFastaList = posValidationReader.getData();
		List<FastaFormat> negValidationFastaList = negValidationReader.getData();
		
		System.out.print("Loading classifiers..");
		List<ClassifierData> classifierDataList = new ArrayList<ClassifierData>();
		int numOfClassifiers = 20;
		for(int x = 0; x < numOfClassifiers; x++){
			try{
			classifierDataList.add(SiriusClassifier.loadClassifier(
					classifierDirectory + "J48_" + x + ".classifierone"));
			}catch(Exception e){e.printStackTrace();}
		}		
		System.out.println("Done!");
		
		for(ClassifierData cData:classifierDataList){
			SiriusClassifier.runType3Classifier(cData, posValidationFastaList,					
					outputDirectory, "PosValidation", false, 0.0, true);
			SiriusClassifier.runType3Classifier(cData, negValidationFastaList,
					outputDirectory, "NegValidation", false, 0.0, false);
		}		
	}
}
