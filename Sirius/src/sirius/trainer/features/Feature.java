/*==========================================================================
	  SiriusPSB - A Generic System for Analysis of Biological Sequences
	        http://compbio.ddns.comp.nus.edu.sg/~sirius/index.php
============================================================================
	  Copyright (C) 2007 by Chuan Hock Koh

	  This program is free software; you can redistribute it and/or
	  modify it under the terms of the GNU General Public
	  License as published by the Free Software Foundation; either
	  version 3 of the License, or (at your option) any later version.

	  This program is distributed in the hope that it will be useful,
	  but WITHOUT ANY WARRANTY; without even the implied warranty of
	  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
	  General Public License for more details.

	  You should have received a copy of the GNU General Public License
	  along with this program.  If not, see <http://www.gnu.org/licenses/>.
==========================================================================*/

package sirius.trainer.features;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import sirius.main.ApplicationData;
import sirius.predictor.main.SiriusClassifier;
import sirius.trainer.main.ScoringMatrix;
import sirius.trainer.step2.Physiochemical2;
import sirius.utils.FastaFormat;
import sirius.utils.Utils;


//Used set (A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,Z)
// 1) K - Kgram with Xmistakes
// 2) L - Kgram with Xmistakes - Relative
// 3) R - Ratio of #X:#Y	
// 4) M - Multiple Kgram with Xmistakes, min Y and max Z gaps
// 5) N - Multiple Kgram with Xmistakes, min Y and max Z gaps - Relative
// 6) B - Basic PhysioChemical Features
// 7) C - Basic II PhysioChemical Features - No window
// 8) A - Advanced PhysioChemical Features - No window
// 9) P - Position-Specific Features
// 10) G - Physiochemical K-gram
// 11) H - Physiochemical K-gram - Relative
// 12) O - Physiochemical Ratio of #X:#Y
// 13) U - Physiochemical Multiple K-gram 
// 14) T - Physiochemical Multiple K-gram - Relative
// 15) S - Prosite Features ->
// 16) F - Pfam Features	-> Note that both Prosite and Pfam features are not properly integrated yet
// 17) D - Physiochemical II K-gram	-> Physiochemical II is meant to replace Physiochemical eventually
// 18) E - Physiochemical II K-gram - Relative
// 19) Q - Physiochemical II Ratio of #X:#Y
// 20) I - Physiochemical II Multiple K-gram
// 21) J - Physiochemical II Multiple K-gram - Relative
// 22) Z - Classifier Features	

abstract public class Feature{
	static final long serialVersionUID = sirius.Sirius.version;

	protected char type;
	protected String name;		
	protected String details;
	protected Boolean box;
	protected int windowFrom;
	protected int windowTo;
	//this indicates if the windowFrom and windowTo are percentage of the sequence
	protected boolean isPercentage;
	protected double score;//Total GA Score
	protected double fitnessScore;//Used by GA
	protected double correlationScore;//Used by GA	
	//minCutOff, maxCutOff are used for NNSearcher
	private Double minCutOff;
	private Double maxCutOff;
	//Used by Genetic Algorithm
	private double[] valueList;
	private double mutualInformation;//with class


	final public void setValueList(double[] dList){this.valueList = dList;}
	final public double[] getValueList(){return this.valueList;}
	final public void setMI(double mi){this.mutualInformation = mi;}
	final public double getMutualInformation(){return this.mutualInformation;}
	
	final public Double getMinCutOff(){ return this.minCutOff; }
	final public Double getMaxCutOff(){ return this.maxCutOff; }
	final public void setMinCutOff(Double value){ this.minCutOff = value; }
	final public void setMaxCutOff(Double value){ this.maxCutOff = value; }

	public Feature(String name, String details, char type){ this(name, details, type, false); }

	public Feature(String name, String details, char type, boolean box){
		this.name = name;
		this.details = details;
		this.type = type;
		this.box = box;
	}

	//This method is used for reading features from files	
	final public static Feature loadSettings(ApplicationData applicationData, String saveDirectory,Feature feature){	
		try{			
			return Feature.loadSettings(applicationData,saveDirectory, feature.saveString(saveDirectory));
		}catch(Exception ex){ex.printStackTrace(); return null;}
	}
	
	final public static List<Feature> loadSettings(String fileLocation){		
		try{
			List<Feature> featureList = new ArrayList<Feature>();
			BufferedReader input = new BufferedReader(new FileReader(fileLocation));
			String line;
			while((line = input.readLine()) != null){
				featureList.add(loadFeature(line, null));
			}
			input.close();
			return featureList;
		}catch(Exception e){e.printStackTrace(); return null;}		
	}
	
	final public static Feature loadFeature(String line, String loadDirectory) throws Exception{
		String typeString = line.substring(line.indexOf("Type: ") + ("Type: ").length(),
				line.indexOf("Type: ") + ("Type: ").length() + 1);
		char type = typeString.charAt(0);
		if(type == 'D' || type == 'E' || type == 'K' || type == 'L' || type == 'G' || type == 'H')//k-gram 
			return KGramFeature.loadSettings(line, true, type);				
		else if(type == 'R' || type == 'O' || type == 'Q')//ratio 
			return RatioOfKGramFeature.loadSettings(line, true, type);
		else if(type == 'M' || type == 'N' || type == 'U' || type == 'T' || type == 'I' || type == 'J')//multiple-kgram
			return MultipleKGramFeature.loadSettings(line, true, type);
		else if(type == 'B')
			return new BasicPhysiochemicalFeature(line, type);
		else if(type == 'C')
			return new Basic2PhysiochemicalFeature(line, type);		
		else if(type == 'A')		
			return new AdvancedPhysiochemicalFeature(line, type);
		else if(type == 'P')
			return new PositionSpecificFeature(line, type);
		else if(type == 'Z')
			return ClassifierFeature.loadSettings(line, loadDirectory);
		else if(type == 'X')
			return MetaFeature.loadSettings(line, type);
		else
			throw new Exception("Error in Loading due to incorrect format");
	}

	final public static Feature loadSettings(ApplicationData applicationData, String loadDirectory, 
			String line) throws Exception{
		String typeString = line.substring(line.indexOf("Type: ") + ("Type: ").length(),
				line.indexOf("Type: ") + ("Type: ").length() + 1);
		char type = typeString.charAt(0);
		if(type == 'D' || type == 'E' || type == 'K' || type == 'L' || type == 'G' || type == 'H')//k-gram 
			return KGramFeature.loadSettings(line, applicationData.isLocationIndexMinusOne, type);				
		else if(type == 'R' || type == 'O' || type == 'Q')//ratio 
			return RatioOfKGramFeature.loadSettings(line, applicationData.isLocationIndexMinusOne, type);
		else if(type == 'M' || type == 'N' || type == 'U' || type == 'T' || type == 'I' || type == 'J')//multiple-kgram
			return MultipleKGramFeature.loadSettings(line, applicationData.isLocationIndexMinusOne, type);
		else if(type == 'B')
			return new BasicPhysiochemicalFeature(line, type);
		else if(type == 'C')
			return new Basic2PhysiochemicalFeature(line, type);		
		else if(type == 'A')		
			return new AdvancedPhysiochemicalFeature(line, type);
		else if(type == 'P')
			return new PositionSpecificFeature(line, type);
		else if(type == 'Z')
			return ClassifierFeature.loadSettings(line, loadDirectory);
		else if(type == 'X')
			return MetaFeature.loadSettings(line, type);
		else
			throw new Exception("Error in Loading due to incorrect format");			
	}

	final public static String saveFeatureViaName(String name){
		return loadFeatureViaName(name).saveString(null);			
	}

	final public static Feature loadFeatureViaName(String name){
		char type = name.charAt(0);
		if(type == 'D' || type == 'E' || type == 'K' || type == 'L' || type == 'G' || type == 'H')//k-gram 
			return new KGramFeature(name);				
		else if(type == 'R' || type == 'O' || type == 'Q')//ratio
			return new RatioOfKGramFeature(name);
		else if(type == 'M' || type == 'N' || type == 'U' || type == 'T' || type == 'I' || type == 'J')//multiple-kgram
			return new MultipleKGramFeature(name);
		else if(type == 'B')
			return new BasicPhysiochemicalFeature(name);
		else if(type == 'C')
			return new Basic2PhysiochemicalFeature(name);		
		else if(type == 'A')		
			return new AdvancedPhysiochemicalFeature(name);
		else if(type == 'P')
			return new PositionSpecificFeature(name);
		else if(type == 'Z')
			throw new Error("Do not support saving/loading of classifierFeature via Name");
		else if(type == 'X')
			throw new Error("Do not support saving/loading of metaFeature via Name");
		else
			throw new Error("Error in Saving/Loading due to unhandled type: " + type);	
	}
	
	//Used by LevelOneClassifierPane
	final public static Feature levelOneClassifierPane(String name) throws Exception{		
		StringTokenizer st = new StringTokenizer(name,"_");	
		char type = st.nextToken().charAt(0);
		switch(type){
		case 'D': case 'E': case 'K': case 'L': case 'G': case 'H': return new KGramFeature(name);
		case 'R': case 'O': case 'Q': return new RatioOfKGramFeature(name);
		case 'M': case 'N': case 'U': case 'T': case 'I': case 'J': return (new MultipleKGramFeature(name));
		case 'B': return new BasicPhysiochemicalFeature(name);
		case 'C': return new Basic2PhysiochemicalFeature(name);
		case 'A': return new AdvancedPhysiochemicalFeature(name);
		case 'P': return new PositionSpecificFeature(name);
		case 'X': return new MetaFeature(name);
		case 'Z': String cFile = Utils.selectFile("Please select " + name + " classifier file");
				return SiriusClassifier.loadClassifierAsFeature(name, cFile);
		default: throw new Exception("Unknown Type" + type); 
		}
	}					
	//GA
	private static int[] doesWindowOverlap(Feature feature1, Feature feature2){
		if(
			(feature1.windowFrom >= feature2.windowFrom && feature1.windowFrom <= feature2.windowTo) ||
			(feature2.windowFrom >= feature1.windowFrom && feature2.windowFrom <= feature1.windowTo)
		){
			int[] window = new int[2];
			window[0] = Math.min(feature1.windowFrom, feature2.windowFrom);
			window[1] = Math.max(feature1.windowTo, feature2.windowTo);
			return window;
		}
		else
			return null;
	}
	
	public abstract Feature mutate(Random rand, int windowMin, int windowMax);
	final public static void crossoverMain(List<Feature> newGeneration, Feature feature1, Feature feature2,Random rand){
		if(feature1 instanceof KGramFeature){ 
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature1, (KGramFeature) feature2, rand);
			
			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature1, (MultipleKGramFeature) feature2, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (KGramFeature) feature1, (RatioOfKGramFeature) feature2, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (KGramFeature) feature1, (PositionSpecificFeature) feature2, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (KGramFeature) feature1, (BasicPhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (KGramFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (KGramFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			

		}else if(feature1 instanceof MultipleKGramFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (MultipleKGramFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (MultipleKGramFeature) feature2, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (RatioOfKGramFeature) feature2, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (PositionSpecificFeature) feature2, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (BasicPhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			
		}else if(feature1 instanceof RatioOfKGramFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (RatioOfKGramFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature2, (RatioOfKGramFeature) feature1, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature1, (RatioOfKGramFeature) feature2, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature1, (PositionSpecificFeature) feature2, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature1, (BasicPhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			
		}else if(feature1 instanceof PositionSpecificFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (PositionSpecificFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature2, (PositionSpecificFeature) feature1, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature2, (PositionSpecificFeature) feature1, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature1, (PositionSpecificFeature) feature2, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature1, (BasicPhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			
		}else if(feature1 instanceof BasicPhysiochemicalFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (BasicPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature2, (BasicPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature2, (BasicPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature2, (BasicPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (BasicPhysiochemicalFeature) feature1, (BasicPhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (BasicPhysiochemicalFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (BasicPhysiochemicalFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			
		}else if(feature1 instanceof Basic2PhysiochemicalFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (Basic2PhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature2, (Basic2PhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature2, (Basic2PhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature2, (Basic2PhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (BasicPhysiochemicalFeature) feature2, (Basic2PhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (Basic2PhysiochemicalFeature) feature1, (Basic2PhysiochemicalFeature) feature2, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (Basic2PhysiochemicalFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");			
		}else if(feature1 instanceof AdvancedPhysiochemicalFeature){
			if(feature2 instanceof KGramFeature) 
				Feature.crossover(newGeneration, (KGramFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof MultipleKGramFeature) 
				Feature.crossover(newGeneration, (MultipleKGramFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof RatioOfKGramFeature)
				Feature.crossover(newGeneration, (RatioOfKGramFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof PositionSpecificFeature)
				Feature.crossover(newGeneration, (PositionSpecificFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof BasicPhysiochemicalFeature)
				Feature.crossover(newGeneration, (BasicPhysiochemicalFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof Basic2PhysiochemicalFeature)
				Feature.crossover(newGeneration, (Basic2PhysiochemicalFeature) feature2, (AdvancedPhysiochemicalFeature) feature1, rand);

			else if(feature2 instanceof AdvancedPhysiochemicalFeature)
				Feature.crossover(newGeneration, (AdvancedPhysiochemicalFeature) feature1, (AdvancedPhysiochemicalFeature) feature2, rand);

			else
				throw new Error("Unhandled case");		
		}else
			throw new Error("Unhandled case");
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, KGramFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new KGramFeature(feature1.name);
		feature2 = new KGramFeature(feature2.name);
		//just swap the windowFrom,windowTo and isPercentage				
		//Feature 1 kgram and Feature 2 window
		//normal or Physiochemical
		if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
			newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
					feature2.windowFrom,feature2.windowTo,feature2.isPercentage));			
		//Physiochemical2
		else if(feature1.type == 'D' || feature1.type == 'E')
			newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
					feature2.windowFrom,feature2.windowTo,feature1.getP2(),feature2.isPercentage));							
		else
			throw new Error("Unknown type");
		//Feature 2 kgram and Feature 1 window
		//normal or Physiochemical
		if(feature2.type == 'K' || feature2.type == 'L' || feature2.type == 'G' || feature2.type == 'H')	
			newGeneration.add(new KGramFeature(feature2.type,feature2.getKGram(),feature2.getMistakeAllowed(),
					feature1.windowFrom,feature1.windowTo,feature1.isPercentage));			
		//Physiochemical2
		else if(feature2.type == 'D' || feature2.type == 'E')
			newGeneration.add(new KGramFeature(feature2.type,feature2.getKGram(),feature2.getMistakeAllowed(),
					feature1.windowFrom,feature1.windowTo,feature2.getP2(),feature1.isPercentage));							
		else
			throw new Error("Unknown type");
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, MultipleKGramFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new KGramFeature(feature1.name);
		feature2 = new MultipleKGramFeature(feature2.name);
		//if type is the same, take a kgram from multiple and put into the window
		//else just take the window from multiple
		//feature 1: swap window location
		if(
				((feature1.type == 'K' || feature1.type == 'L') && (feature2.type == 'M' || feature2.type == 'N')) ||
				((feature1.type == 'G' || feature1.type == 'H') && (feature2.type == 'U' || feature2.type == 'T')) ||
				((feature1.type == 'D' || feature1.type == 'E') && (feature2.type == 'I' || feature2.type == 'J') 
						&& feature1.getP2() == feature2.getP2()) 
		){
			int index = rand.nextInt(feature2.getFeatureListSize());
			//normal or Physiochemical		
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,feature2.getFeatureAt(index),feature2.getMistakeAt(index),
						feature1.windowFrom,feature1.windowTo,feature1.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,feature2.getFeatureAt(index),feature2.getMistakeAt(index),
						feature1.windowFrom,feature1.windowTo,feature1.getP2(),feature1.isPercentage));							
			else
				throw new Error("Unknown type");
		}else{
			//normal or Physiochemical		
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,feature2.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,feature1.getP2(),feature2.isPercentage));							
			else
				throw new Error("Unknown type");
		}

		//feature 2: 
		//if type is the same, extend multiple-kgram
		//and if window overlap, merge it
		//else just swap window location
		if(
				((feature1.type == 'K' || feature1.type == 'L') && (feature2.type == 'M' || feature2.type == 'N')) ||
				((feature1.type == 'G' || feature1.type == 'H') && (feature2.type == 'U' || feature2.type == 'T')) ||
				((feature1.type == 'D' || feature1.type == 'E') && (feature2.type == 'I' || feature2.type == 'J') 
						&& feature1.getP2() == feature2.getP2()) 
		){
			//type is the same
			int insertIndex = rand.nextInt(feature2.getFeatureListSize() + 1);//+1 to allow insertion at the end
			feature2.addFeatureAt(insertIndex, feature1.getKGram());
			feature2.addMistakeAt(insertIndex, feature1.getMistakeAllowed());
			int windowSize = feature2.windowTo - feature2.windowFrom;
			int minGap = rand.nextInt(windowSize/2);
			int maxGap = Feature.randomBetween(minGap, windowSize/2, rand);
			feature2.addMinGapAt(minGap);
			feature2.addMaxGapAt(maxGap);	
			if(feature1.isPercentage == feature2.isPercentage){
				int[] window = Feature.doesWindowOverlap(feature1, feature2);
				if(window != null){
					feature2.windowFrom = window[0];
					feature2.windowTo = window[1];
				}
			}			
			String[] kField = new String[feature2.getFeatureListSize()];
			for(int x = 0; x < feature2.getFeatureListSize(); x++)
				kField[x] = feature2.getFeatureAt(x);
			int[] xField = new int[feature2.getMistakeListSize()];
			for(int x = 0; x < feature2.getMistakeListSize(); x++)
				xField[x] = feature2.getMistakeAt(x);
			int[] yField = new int[feature2.getMinGapListSize()];
			for(int x = 0; x < feature2.getMinGapListSize(); x++)
				yField[x] = feature2.getMinGapAt(x);
			int[] zField = new int[feature2.getMaxGapListSize()];
			for(int x = 0; x < feature2.getMaxGapListSize(); x++)
				zField[x] = feature2.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature2.type == 'M' || feature2.type == 'N' || feature2.type == 'U' || feature2.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature2.isPercentage));						
			}
			//Physiochemical2
			else if(feature2.type == 'I' || feature2.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature2.getP2(),feature2.isPercentage));			
			}
			else
				throw new Error("Unknown type");
		}else{
			//different type, hence just swap window
			String[] kField = new String[feature2.getFeatureListSize()];
			for(int x = 0; x < feature2.getFeatureListSize(); x++)
				kField[x] = feature2.getFeatureAt(x);
			int[] xField = new int[feature2.getMistakeListSize()];
			for(int x = 0; x < feature2.getMistakeListSize(); x++)
				xField[x] = feature2.getMistakeAt(x);
			int[] yField = new int[feature2.getMinGapListSize()];
			for(int x = 0; x < feature2.getMinGapListSize(); x++)
				yField[x] = feature2.getMinGapAt(x);
			int[] zField = new int[feature2.getMaxGapListSize()];
			for(int x = 0; x < feature2.getMaxGapListSize(); x++)
				zField[x] = feature2.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature2.type == 'M' || feature2.type == 'N' || feature2.type == 'U' || feature2.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature1.isPercentage));						
			}
			//Physiochemical2
			else if(feature2.type == 'I' || feature2.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature2.getP2(),feature1.isPercentage));			
			}
			else
				throw new Error("Unknown type");
		}
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, RatioOfKGramFeature feature2, Random rand){	
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new KGramFeature(feature1.name);
		feature2 = new RatioOfKGramFeature(feature2.name);
		//if type is the same, take a kgram from multiple and put into the window
		//else just take the window from multiple
		//feature 1: swap window location
		if(
				((feature1.type == 'K' || feature1.type == 'L') && (feature2.type == 'R')) ||
				((feature1.type == 'G' || feature1.type == 'H') && (feature2.type == 'O')) ||
				((feature1.type == 'D' || feature1.type == 'E') && (feature2.type == 'Q') 
						&& feature1.getP2() == feature2.getP2()) 
		){
			int index = rand.nextInt(2);
			//normal or Physiochemical		
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,feature2.getFeatureAt(index),feature2.getMistakeAt(index),
						feature1.windowFrom,feature1.windowTo,feature1.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,feature2.getFeatureAt(index),feature2.getMistakeAt(index),
						feature1.windowFrom,feature1.windowTo,feature1.getP2(),feature1.isPercentage));							
			else
				throw new Error("Unknown type");
		}else{
			//normal or Physiochemical		
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,feature2.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,feature1.getP2(),feature2.isPercentage));							
			else
				throw new Error("Unknown type");
		}

		//feature 2: 
		//if type is the same, take the kgram and put into ratio			
		//else just swap window location
		if(
				((feature1.type == 'K' || feature1.type == 'L') && (feature2.type == 'R')) ||
				((feature1.type == 'G' || feature1.type == 'H') && (feature2.type == 'O')) ||
				((feature1.type == 'D' || feature1.type == 'E') && (feature2.type == 'Q') 
						&& feature1.getP2() == feature2.getP2()) 
		){
			//type is the same			
			String kgram = feature2.getFeature1();
			String kgram2 = feature2.getFeature2();
			int m1 = feature2.getMistakeAt(0);
			int m2 = feature2.getMistakeAt(1);
			if(rand.nextBoolean()){
				kgram = feature1.getKGram();
				m1 = feature1.getMistakeAllowed();
			}else{
				kgram2 = feature1.getKGram();
				m2 = feature1.getMistakeAllowed();
			}
			//normal or Physiochemical
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,kgram,kgram2,feature2.windowFrom,feature2.windowTo,
						m1,m2,feature2.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,kgram,kgram2,feature2.windowFrom,feature2.windowTo,
						m1,m2,feature2.isPercentage,feature2.getP2()));		
			}
			else
				throw new Error("Unknown type");
		}else{
			//different type, hence just swap window			
			//normal or Physiochemical			
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),
						feature1.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),
						feature1.isPercentage,feature2.getP2()));
			}
			else
				throw new Error("Unknown type");
		}
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, PositionSpecificFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new KGramFeature(feature1.name);
		feature2 = new PositionSpecificFeature(feature2.name);
		//if type is the same, then can extract the kgram and swap with position and vice versa
		//else just switch around the window
		if(
				((feature1.type == 'K' || feature1.type == 'L') && feature2.getP2() == 0) ||
				((feature1.type == 'D' || feature1.type == 'E') && feature1.getP2() == feature2.getP2())
		){
			//same type
			//Feature1 & Feature2 together
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			String featureName = st.nextToken() + "_" + st.nextToken() + "_" + st.nextToken();
			String kgram = "";
			while(st.countTokens() > 1){
				featureName += "_" + feature1.getKGram().charAt(rand.nextInt(feature1.getKGram().length()));
				String temp = st.nextToken();
				kgram += temp.charAt(rand.nextInt(temp.length()));
			}
			featureName += "_" + st.nextToken();			
			//normal or Physiochemical
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,kgram,Feature.getMistakeAllowed(rand, kgram.length()),
						feature1.windowFrom,feature1.windowTo,feature1.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,kgram,Feature.getMistakeAllowed(rand, kgram.length()),
						feature1.windowFrom,feature1.windowTo,feature1.getP2(),feature1.isPercentage));							
			else
				throw new Error("Unknown type");
			//Feature2
			newGeneration.add(new PositionSpecificFeature(featureName));
		}else{
			//different type, just swap windowLocation
			//Feature1
			//normal or Physiochemical
			if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,false));			
			//Physiochemical2
			else if(feature1.type == 'D' || feature1.type == 'E')
				newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
						feature2.windowFrom,feature2.windowTo,feature1.getP2(),false));							
			else
				throw new Error("Unknown type");
			//Feature2		
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			st.nextToken();//P
			int positionFrom = Integer.parseInt(st.nextToken());//positionFrom
			int positionTo = Integer.parseInt(st.nextToken());//positionTo
			int length = positionTo - positionFrom;
			int start = Feature.randomBetween(feature1.windowFrom, feature1.windowTo, rand);
			int end = start + length;
			String featureName = "P_" + start + "_" + end;
			while(st.hasMoreTokens())
				featureName += "_" + st.nextToken();
			newGeneration.add(new PositionSpecificFeature(featureName));
		}
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, BasicPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new KGramFeature(feature1.name);
		feature2 = new BasicPhysiochemicalFeature(feature2.name);
		//Just swap the window
		//Feature1
		//normal or Physiochemical
		if(feature1.type == 'K' || feature1.type == 'L' || feature1.type == 'G' || feature1.type == 'H')	
			newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
					feature2.windowFrom,feature2.windowTo,feature2.isPercentage));			
		//Physiochemical2
		else if(feature1.type == 'D' || feature1.type == 'E')
			newGeneration.add(new KGramFeature(feature1.type,feature1.getKGram(),feature1.getMistakeAllowed(),
					feature2.windowFrom,feature2.windowTo,feature1.getP2(),feature2.isPercentage));							
		else
			throw new Error("Unknown type");
		//Feature2
		StringTokenizer st = new StringTokenizer(feature2.name,"_");
		String featureName = st.nextToken() + "_" + st.nextToken() + "_" + feature1.isPercentage + "_" + 
		feature1.windowFrom + "_" + feature1.windowTo;								
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new KGramFeature(feature1.name);
		//feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		//Tough to cross these two features				
		//hence do nothing
	}
	final private static void crossover(List<Feature> newGeneration, KGramFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new KGramFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to cross these two features				
		//hence do nothing
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, MultipleKGramFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new MultipleKGramFeature(feature1.name);
		feature2 = new MultipleKGramFeature(feature2.name);		
		//if it is the same type, swap kgram
		//else just swap windowLocation
		if(
				((feature1.type == 'M' || feature1.type == 'N') && (feature2.type == 'M' || feature2.type == 'N')) ||
				((feature1.type == 'U' || feature1.type == 'T') && (feature2.type == 'U' || feature2.type == 'T')) ||
				((feature1.type == 'I' || feature1.type == 'J') && (feature2.type == 'I' || feature2.type == 'J') && 
						feature1.getP2() == feature2.getP2())
		){
			//same type - swap kgram			
			String[] kField1 = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField1[x] = feature1.getFeatureAt(x);
			int[] xField1 = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField1[x] = feature1.getMistakeAt(x);
			int[] yField1 = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField1[x] = feature1.getMinGapAt(x);
			int[] zField1 = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField1[x] = feature1.getMaxGapAt(x);

			String[] kField2 = new String[feature2.getFeatureListSize()];
			for(int x = 0; x < feature2.getFeatureListSize(); x++)
				kField2[x] = feature2.getFeatureAt(x);
			int[] xField2 = new int[feature2.getMistakeListSize()];
			for(int x = 0; x < feature2.getMistakeListSize(); x++)
				xField2[x] = feature2.getMistakeAt(x);
			int[] yField2 = new int[feature2.getMinGapListSize()];
			for(int x = 0; x < feature2.getMinGapListSize(); x++)
				yField2[x] = feature2.getMinGapAt(x);
			int[] zField2 = new int[feature2.getMaxGapListSize()];
			for(int x = 0; x < feature2.getMaxGapListSize(); x++)
				zField2[x] = feature2.getMaxGapAt(x);

			//swapping
			int index1 = rand.nextInt(feature1.getFeatureListSize());
			int index2 = rand.nextInt(feature2.getFeatureListSize());
			String tempString = kField1[index1];
			int tempInt = xField1[index1];
			kField1[index1] = kField2[index2];
			xField1[index1] = xField2[index2];
			kField2[index2] = tempString;
			xField2[index2] = tempInt;

			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField1,xField1,yField1,zField1,
						feature1.windowFrom,feature1.windowTo,feature1.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField1,xField1,yField1,zField1,
						feature1.windowFrom,feature1.windowTo,feature1.getP2(),feature1.isPercentage));			
			}
			else
				throw new Error("Unknown type");

			//normal or Physiochemical
			if(feature2.type == 'M' || feature2.type == 'N' || feature2.type == 'U' || feature2.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField2,xField2,yField2,zField2,
						feature2.windowFrom,feature2.windowTo,feature2.isPercentage));						
			}
			//Physiochemical2
			else if(feature2.type == 'I' || feature2.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField2,xField2,yField2,zField2,
						feature2.windowFrom,feature2.windowTo,feature2.getP2(),feature2.isPercentage));			
			}
			else
				throw new Error("Unknown type");
		}else{
			//different type - swap windowLocation
			String[] kField = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField[x] = feature1.getFeatureAt(x);
			int[] xField = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField[x] = feature1.getMistakeAt(x);
			int[] yField = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField[x] = feature1.getMinGapAt(x);
			int[] zField = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField[x] = feature1.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature2.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature1.getP2(),feature2.isPercentage));			
			}
			else
				throw new Error("Unknown type");

			kField = new String[feature2.getFeatureListSize()];
			for(int x = 0; x < feature2.getFeatureListSize(); x++)
				kField[x] = feature2.getFeatureAt(x);
			xField = new int[feature2.getMistakeListSize()];
			for(int x = 0; x < feature2.getMistakeListSize(); x++)
				xField[x] = feature2.getMistakeAt(x);
			yField = new int[feature2.getMinGapListSize()];
			for(int x = 0; x < feature2.getMinGapListSize(); x++)
				yField[x] = feature2.getMinGapAt(x);
			zField = new int[feature2.getMaxGapListSize()];
			for(int x = 0; x < feature2.getMaxGapListSize(); x++)
				zField[x] = feature2.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature2.type == 'M' || feature2.type == 'N' || feature2.type == 'U' || feature2.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature1.isPercentage));						
			}
			//Physiochemical2
			else if(feature2.type == 'I' || feature2.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature2.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature2.getP2(),feature1.isPercentage));			
			}
			else
				throw new Error("Unknown type");
		}
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, RatioOfKGramFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new MultipleKGramFeature(feature1.name);
		feature2 = new RatioOfKGramFeature(feature2.name);		
		//if same type, swap kgram
		//else swap window
		if(
				((feature1.type == 'M' || feature1.type == 'N') && (feature2.type == 'R')) || 
				((feature1.type == 'U' || feature1.type == 'T') && (feature2.type == 'O')) ||
				((feature1.type == 'I' || feature1.type == 'J') && (feature2.type == 'Q') && (feature1.getP2() == feature2.getP2()))
		){
			//same type
			//Feature1
			String[] kField = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField[x] = feature1.getFeatureAt(x);
			int[] xField = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField[x] = feature1.getMistakeAt(x);
			int[] yField = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField[x] = feature1.getMinGapAt(x);
			int[] zField = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField[x] = feature1.getMaxGapAt(x);
			//swapping
			int indexR = rand.nextInt(2);
			int indexM = rand.nextInt(feature1.getFeatureListSize());
			kField[indexM] = feature2.getFeatureAt(indexR);
			xField[indexM] = feature2.getMistakeAt(indexR);

			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature1.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature1.windowFrom,feature1.windowTo,
						feature1.getP2(),feature1.isPercentage));			
			}
			else
				throw new Error("Unknown type");

			//normal or Physiochemical
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature1.getFeatureAt(indexM),feature2.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(indexM),feature2.getMistakeAt(1),feature2.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature1.getFeatureAt(indexM),feature2.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(indexM),feature2.getMistakeAt(1),
						feature2.isPercentage,feature2.getP2()));		
			}
			else
				throw new Error("Unknown type");
		}else{
			//different type
			//Feature1
			String[] kField = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField[x] = feature1.getFeatureAt(x);
			int[] xField = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField[x] = feature1.getMistakeAt(x);
			int[] yField = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField[x] = feature1.getMinGapAt(x);
			int[] zField = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField[x] = feature1.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature2.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature1.getP2(),feature2.isPercentage));			
			}
			else
				throw new Error("Unknown type");

			//normal or Physiochemical
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),feature1.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),
						feature1.isPercentage,feature2.getP2()));		
			}
			else
				throw new Error("Unknown type");
		}
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, PositionSpecificFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new MultipleKGramFeature(feature1.name);
		feature2 = new PositionSpecificFeature(feature2.name);		
		//if same type extract kgram from multiple
		//else just swap window
		if(
				((feature1.type == 'M' || feature1.type == 'N') && feature2.getP2() == 0) ||
				((feature1.type == 'I' || feature1.type == 'J') && feature1.getP2() == feature2.getP2())
		){
			//same type
			//Feature1 & Feature2 together
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			String featureName = st.nextToken() + "_" + st.nextToken() + "_" + st.nextToken();
			int indexP = rand.nextInt(feature1.getFeatureListSize());
			String kgram = "";
			while(st.countTokens() > 1){
				featureName += "_" + feature1.getFeatureAt(indexP).charAt(rand.nextInt(feature1.getFeatureAt(indexP).length()));
				String temp = st.nextToken();
				kgram += temp.charAt(rand.nextInt(temp.length()));
			}
			featureName += "_" + st.nextToken();			
			//Feature1
			//same type - swap kgram			
			String[] kField1 = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField1[x] = feature1.getFeatureAt(x);
			int[] xField1 = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField1[x] = feature1.getMistakeAt(x);
			int[] yField1 = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField1[x] = feature1.getMinGapAt(x);
			int[] zField1 = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField1[x] = feature1.getMaxGapAt(x);					

			//swapping
			int indexM = rand.nextInt(feature1.getFeatureListSize());
			kField1[indexM] = kgram;
			xField1[indexM] = Feature.getMistakeAllowed(rand, kgram.length());

			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField1,xField1,yField1,zField1,
						feature1.windowFrom,feature1.windowTo,feature1.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField1,xField1,yField1,zField1,
						feature1.windowFrom,feature1.windowTo,feature1.getP2(),feature1.isPercentage));			
			}
			else
				throw new Error("Unknown type");
			//Feature2
			newGeneration.add(new PositionSpecificFeature(featureName));
		}else{
			//different
			//Feature1
			String[] kField = new String[feature1.getFeatureListSize()];
			for(int x = 0; x < feature1.getFeatureListSize(); x++)
				kField[x] = feature1.getFeatureAt(x);
			int[] xField = new int[feature1.getMistakeListSize()];
			for(int x = 0; x < feature1.getMistakeListSize(); x++)
				xField[x] = feature1.getMistakeAt(x);
			int[] yField = new int[feature1.getMinGapListSize()];
			for(int x = 0; x < feature1.getMinGapListSize(); x++)
				yField[x] = feature1.getMinGapAt(x);
			int[] zField = new int[feature1.getMaxGapListSize()];
			for(int x = 0; x < feature1.getMaxGapListSize(); x++)
				zField[x] = feature1.getMaxGapAt(x);
			//normal or Physiochemical
			if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature2.isPercentage));						
			}
			//Physiochemical2
			else if(feature1.type == 'I' || feature1.type == 'J'){				
				newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
						feature1.getP2(),feature2.isPercentage));			
			}
			else
				throw new Error("Unknown type");

			//Feature2
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			st.nextToken();//P
			int positionFrom = Integer.parseInt(st.nextToken());//positionFrom
			int positionTo = Integer.parseInt(st.nextToken());//positionTo
			int length = positionTo - positionFrom;
			int start = Feature.randomBetween(feature1.windowFrom, feature1.windowTo, rand);
			int end = start + length;
			String featureName = "P_" + start + "_" + end;
			while(st.hasMoreTokens())
				featureName += "_" + st.nextToken();
			newGeneration.add(new PositionSpecificFeature(featureName));
		}
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, BasicPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new MultipleKGramFeature(feature1.name);
		feature2 = new BasicPhysiochemicalFeature(feature2.name);
		//swap the window		
		//Feature1
		String[] kField = new String[feature1.getFeatureListSize()];
		for(int x = 0; x < feature1.getFeatureListSize(); x++)
			kField[x] = feature1.getFeatureAt(x);
		int[] xField = new int[feature1.getMistakeListSize()];
		for(int x = 0; x < feature1.getMistakeListSize(); x++)
			xField[x] = feature1.getMistakeAt(x);
		int[] yField = new int[feature1.getMinGapListSize()];
		for(int x = 0; x < feature1.getMinGapListSize(); x++)
			yField[x] = feature1.getMinGapAt(x);
		int[] zField = new int[feature1.getMaxGapListSize()];
		for(int x = 0; x < feature1.getMaxGapListSize(); x++)
			zField[x] = feature1.getMaxGapAt(x);
		//normal or Physiochemical
		if(feature1.type == 'M' || feature1.type == 'N' || feature1.type == 'U' || feature1.type == 'T'){			
			newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
					feature2.isPercentage));
		}
		//Physiochemical2
		else if(feature1.type == 'I' || feature1.type == 'J'){				
			newGeneration.add(new MultipleKGramFeature(feature1.type,kField,xField,yField,zField,feature2.windowFrom,feature2.windowTo,
					feature1.getP2(),feature2.isPercentage));			
		}
		else
			throw new Error("Unknown type");
		//Feature2
		StringTokenizer st = new StringTokenizer(feature2.name,"_");
		String featureName = st.nextToken() + "_" + st.nextToken() + "_" + feature1.isPercentage + "_" + 
		feature1.windowFrom + "_" + feature1.windowTo;								
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new MultipleKGramFeature(feature1.name);
		//feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing
	}
	final private static void crossover(List<Feature> newGeneration, MultipleKGramFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new MultipleKGramFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing		
	}
	final private static void crossover(List<Feature> newGeneration, RatioOfKGramFeature feature1, RatioOfKGramFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new RatioOfKGramFeature(feature1.name);
		feature2 = new RatioOfKGramFeature(feature2.name);
		//if same type, swap kgram
		//else just swap windowlocation
		if(feature1.type != feature2.type || (feature1.type == feature2.type && feature1.type == 'Q' && feature1.getP2() != feature2.getP2())){
			//just swap windowlocation
			//normal or Physiochemical
			if(feature1.type == 'R' || feature1.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
						feature2.windowFrom,feature2.windowTo,
						feature1.getMistakeAt(0),feature1.getMistakeAt(1),feature2.isPercentage));
			}
			//Physiochemical2
			else if(feature1.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
						feature2.windowFrom,feature2.windowTo,
						feature1.getMistakeAt(0),feature1.getMistakeAt(1),feature2.isPercentage,feature1.getP2()));
			}
			else
				throw new Error("Unknown type");

			//normal or Physiochemical
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),feature1.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature2.getFeature1(),feature2.getFeature2(),
						feature1.windowFrom,feature1.windowTo,feature2.getMistakeAt(0),feature2.getMistakeAt(1),
						feature1.isPercentage,feature2.getP2()));		
			}
			else
				throw new Error("Unknown type");			
		}else{
			//swap kgram
			//normal or Physiochemical
			if(feature1.type == 'R' || feature1.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature2.getFeature1(),feature1.getFeature2(),
						feature1.windowFrom,feature1.windowTo,
						feature2.getMistakeAt(0),feature1.getMistakeAt(1),feature1.isPercentage));
			}
			//Physiochemical2
			else if(feature1.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature2.getFeature1(),feature1.getFeature2(),
						feature1.windowFrom,feature1.windowTo,
						feature2.getMistakeAt(0),feature1.getMistakeAt(1),feature1.isPercentage,feature1.getP2()));
			}
			else
				throw new Error("Unknown type");

			//normal or Physiochemical
			if(feature2.type == 'R' || feature2.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature1.getFeature1(),feature2.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature2.getMistakeAt(1),feature2.isPercentage));
			}
			//Physiochemical2
			else if(feature2.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature2.type,feature1.getFeature1(),feature2.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature2.getMistakeAt(1),
						feature2.isPercentage,feature2.getP2()));		
			}
			else
				throw new Error("Unknown type");			
		}
	}
	final private static void crossover(List<Feature> newGeneration, RatioOfKGramFeature feature1, PositionSpecificFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new RatioOfKGramFeature(feature1.name);
		feature2 = new PositionSpecificFeature(feature2.name);		
		//if same type extract kgram from ratio
		//else just swap window
		if((feature1.type == 'R' && feature2.getP2() == 0) || (feature1.type == 'Q' && feature1.getP2() == feature2.getP2())){
			//same type - extract kgram
			//Feature1 & Feature2 together
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			String featureName = st.nextToken() + "_" + st.nextToken() + "_" + st.nextToken();
			int indexP = rand.nextInt(2);
			String kgram = "";
			while(st.countTokens() > 1){
				featureName += "_" + feature1.getFeatureAt(indexP).charAt(rand.nextInt(feature1.getFeatureAt(indexP).length()));
				String temp = st.nextToken();
				kgram += temp.charAt(rand.nextInt(temp.length()));
			}
			featureName += "_" + st.nextToken();			
			String kgram1 = feature1.getFeature1();
			String kgram2 = feature1.getFeature2();
			int m1 = feature1.getMistakeAt(0);
			int m2 = feature1.getMistakeAt(1);
			int indexR = rand.nextInt(2);
			if(indexR == 0){
				kgram1 = kgram;
				m1 = Feature.getMistakeAllowed(rand, kgram1.length());
			}else{
				kgram2 = kgram;
				m2 = Feature.getMistakeAllowed(rand, kgram2.length());
			}
			//normal or Physiochemical
			if(feature1.type == 'R' || feature1.type == 'O')	
				newGeneration.add(new RatioOfKGramFeature(feature1.type,kgram1,kgram2,
						feature1.windowFrom,feature1.windowTo,m1,m2,feature1.isPercentage));			
			//Physiochemical2
			else if(feature1.type == 'Q')
				newGeneration.add(new RatioOfKGramFeature(feature1.type,kgram1,kgram2,
						feature1.windowFrom,feature1.windowTo,m1,m2,feature1.isPercentage,feature1.getP2()));						
			else
				throw new Error("Unknown type");
			//Feature2
			newGeneration.add(new PositionSpecificFeature(featureName));
		}else{
			//different type - swap window
			//Feature1 
			//normal or Physiochemical
			if(feature1.type == 'R' || feature1.type == 'O'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature1.getMistakeAt(1),feature2.isPercentage));
			}
			//Physiochemical2
			else if(feature1.type == 'Q'){				
				newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
						feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature1.getMistakeAt(1),
						feature2.isPercentage,feature1.getP2()));		
			}
			else
				throw new Error("Unknown type");
			//Feature2			
			StringTokenizer st = new StringTokenizer(feature2.name,"_");
			st.nextToken();//P
			int positionFrom = Integer.parseInt(st.nextToken());//positionFrom
			int positionTo = Integer.parseInt(st.nextToken());//positionTo
			int length = positionTo - positionFrom;
			int start = Feature.randomBetween(feature1.windowFrom, feature1.windowTo, rand);
			int end = start + length;
			String featureName = "P_" + start + "_" + end;
			while(st.hasMoreTokens())
				featureName += "_" + st.nextToken();
			newGeneration.add(new PositionSpecificFeature(featureName));
		}
	}
	final private static void crossover(List<Feature> newGeneration, RatioOfKGramFeature feature1, BasicPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new RatioOfKGramFeature(feature1.name);
		feature2 = new BasicPhysiochemicalFeature(feature2.name);		
		//Just swap the window
		//Feature1 
		//normal or Physiochemical
		if(feature1.type == 'R' || feature1.type == 'O'){				
			newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
					feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature1.getMistakeAt(1),feature2.isPercentage));
		}
		//Physiochemical2
		else if(feature1.type == 'Q'){				
			newGeneration.add(new RatioOfKGramFeature(feature1.type,feature1.getFeature1(),feature1.getFeature2(),
					feature2.windowFrom,feature2.windowTo,feature1.getMistakeAt(0),feature1.getMistakeAt(1),
					feature2.isPercentage,feature1.getP2()));		
		}
		else
			throw new Error("Unknown type");
		//Feature2
		StringTokenizer st = new StringTokenizer(feature2.name,"_");
		String featureName = st.nextToken() + "_" + st.nextToken() + "_" + feature1.isPercentage + "_" + 
		feature1.windowFrom + "_" + feature1.windowTo;								
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
	}
	final private static void crossover(List<Feature> newGeneration, RatioOfKGramFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new RatioOfKGramFeature(feature1.name);
		//feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing	
	}
	final private static void crossover(List<Feature> newGeneration, RatioOfKGramFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new RatioOfKGramFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing	
	}
	final private static void crossover(List<Feature> newGeneration, PositionSpecificFeature feature1, PositionSpecificFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new PositionSpecificFeature(feature1.name);
		feature2 = new PositionSpecificFeature(feature2.name);		
		//Just swap the windowLocation
		//Feature1 & Feature2 Together		
		StringTokenizer st = new StringTokenizer(feature1.name,"_");
		st.nextToken();//P
		int positionFrom1 = Integer.parseInt(st.nextToken());//positionFrom
		int positionTo1 = Integer.parseInt(st.nextToken());//positionTo
		int length1 = positionTo1 - positionFrom1;

		StringTokenizer st2 = new StringTokenizer(feature2.name,"_");
		st2.nextToken();//P
		int positionFrom2 = Integer.parseInt(st2.nextToken());//positionFrom
		int positionTo2 = Integer.parseInt(st2.nextToken());//positionTo
		int length2 = positionTo2 - positionFrom2;

		int start1 = Feature.randomBetween(positionFrom2, positionTo2, rand);
		int end1 = start1 + length1;

		int start2 = Feature.randomBetween(positionFrom1, positionTo1, rand);
		int end2 = start2 + length2;

		String featureName1 = "P_" + start1 + "_" + end1;
		while(st.hasMoreTokens())
			featureName1 += "_" + st.nextToken();
		newGeneration.add(new PositionSpecificFeature(featureName1));

		String featureName2 = "P_" + start2 + "_" + end2;
		while(st.hasMoreTokens())
			featureName2 += "_" + st.nextToken();
		newGeneration.add(new PositionSpecificFeature(featureName2));		
	}
	final private static void crossover(List<Feature> newGeneration, PositionSpecificFeature feature1, BasicPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new PositionSpecificFeature(feature1.name);
		feature2 = new BasicPhysiochemicalFeature(feature2.name);		
		//Feature1		
		StringTokenizer st = new StringTokenizer(feature1.name,"_");
		st.nextToken();//P
		int positionFrom = Integer.parseInt(st.nextToken());//positionFrom
		int positionTo = Integer.parseInt(st.nextToken());//positionTo
		int length = positionTo - positionFrom;
		int start = Feature.randomBetween(feature2.windowFrom, feature2.windowTo, rand);
		int end = start + length;
		String featureName = "P_" + start + "_" + end;
		while(st.hasMoreTokens())
			featureName += "_" + st.nextToken();
		newGeneration.add(new PositionSpecificFeature(featureName));
		//Feature2
		st = new StringTokenizer(feature2.name,"_");
		featureName = st.nextToken() + "_" + st.nextToken() + "_" + feature1.isPercentage + "_" + 
		feature1.windowFrom + "_" + feature1.windowTo;								
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
	}
	final private static void crossover(List<Feature> newGeneration, PositionSpecificFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new PositionSpecificFeature(feature1.name);
		//feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing		
	}
	final private static void crossover(List<Feature> newGeneration, PositionSpecificFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new PositionSpecificFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing
	}
	final private static void crossover(List<Feature> newGeneration, BasicPhysiochemicalFeature feature1, BasicPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new BasicPhysiochemicalFeature(feature1.name);
		feature2 = new BasicPhysiochemicalFeature(feature2.name);
		//if one of the feature is global BPC then do nothing
		if(feature1.isPercentage == false || feature2.isPercentage == false) 
			return;
		String[][] crossoverString = new String[2][5];
		StringTokenizer st = new StringTokenizer(feature1.name,"_");		
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[0][x] = st.nextToken();
		st = new StringTokenizer(feature2.name,"_");
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[1][x] = st.nextToken();
		String featureName = "B";
		for(int x = 1; x < 3; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		featureName += "_" + crossoverString[1][3] + "_" + crossoverString[1][4];
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
		featureName = "B";
		for(int x = 1; x < 3; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		featureName += "_" + crossoverString[0][3] + "_" + crossoverString[0][4];
		newGeneration.add(new BasicPhysiochemicalFeature(featureName));
	}
	final private static void crossover(List<Feature> newGeneration, BasicPhysiochemicalFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new BasicPhysiochemicalFeature(feature1.name);
		//feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing		
	}
	final private static void crossover(List<Feature> newGeneration, BasicPhysiochemicalFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new BasicPhysiochemicalFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing	
	}
	final private static void crossover(List<Feature> newGeneration, Basic2PhysiochemicalFeature feature1, Basic2PhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new Basic2PhysiochemicalFeature(feature1.name);
		feature2 = new Basic2PhysiochemicalFeature(feature2.name);
		String[][] crossoverString = new String[2][5];
		StringTokenizer st = new StringTokenizer(feature1.name,"_");
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[0][x] = st.nextToken();
		st = new StringTokenizer(feature2.name,"_");
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[1][x] = st.nextToken();
		String featureName = "C";
		for(int x = 1; x < 5; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		newGeneration.add(new Basic2PhysiochemicalFeature(featureName));
		featureName = "C";
		for(int x = 1; x < 5; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		newGeneration.add(new Basic2PhysiochemicalFeature(featureName));	
	}
	final private static void crossover(List<Feature> newGeneration, Basic2PhysiochemicalFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		//feature1 = new Basic2PhysiochemicalFeature(feature1.name);
		//feature2 = new AdvancedPhysiochemicalFeature(feature2.name);
		//Tough to crossover hence do nothing		
	}
	final private static void crossover(List<Feature> newGeneration, AdvancedPhysiochemicalFeature feature1, AdvancedPhysiochemicalFeature feature2, Random rand){
		//For safety reasons, just in case, i edit the elite features values
		feature1 = new AdvancedPhysiochemicalFeature(feature1.name);
		feature2 = new AdvancedPhysiochemicalFeature(feature2.name);		
		String[][] crossoverString = new String[2][5];
		StringTokenizer st = new StringTokenizer(feature1.name,"_");
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[0][x] = st.nextToken();
		st = new StringTokenizer(feature2.name,"_");
		for(int x = 0; st.hasMoreTokens(); x++)
			crossoverString[1][x] = st.nextToken();
		String featureName = "A";
		for(int x = 1; x < 5; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		newGeneration.add(new AdvancedPhysiochemicalFeature(featureName));
		featureName = "A";
		for(int x = 1; x < 5; x++)
			featureName += "_" + crossoverString[rand.nextInt(2)][x];
		newGeneration.add(new AdvancedPhysiochemicalFeature(featureName));
	}

	public double getScore(){ return this.score; }
	protected static int randomBetween(int min, int max, Random rand){
		return (int)(((max - min) * rand.nextDouble()) + min);
	}
	protected static int[] getWindow(int windowFrom, int windowTo, Random rand){
		int[] window = new int[2];		
		window[0] = randomBetween(windowFrom, windowTo, rand);
		window[1] = randomBetween(window[0], windowTo, rand);
		return window;
	}
	private static String physiochemical2Gram(Random rand, int length, int p2){
		String gram = "";
		Physiochemical2 p = new Physiochemical2(Physiochemical2.indexToName(p2));
		for(int x = 0; x < length; x++)
			//-1 ensures that X is not included in the kgram
			gram += p.getLetter(rand.nextInt(p.getClassificationLetter().size()-1));
		return gram;
	}
	private static char physiochemical2Gram(Random rand, int p2){		
		Physiochemical2 p = new Physiochemical2(Physiochemical2.indexToName(p2));		
		//Note that X is included here because I wanted to allow mutation to be able to mutate to X
		return p.getLetter(rand.nextInt(p.getClassificationLetter().size()));		
	}
	private static String physiochemicalGram(Random rand, int length){
		String gram = "";
		//Took out X because I do not want to include that in the kgram 
		String physiochemical = "HPAOLNKD";
		for(int x = 0; x < length; x++)
			gram += physiochemical.charAt(rand.nextInt(physiochemical.length()));
		return gram;
	}
	private static char physiochemicalGram(Random rand){		
		//Included X here because I wanted to allow mutation to be able to mutate to X 
		String physiochemical = "HPAOLNKDX";		
		return physiochemical.charAt(rand.nextInt(physiochemical.length()));		
	}
	protected static String getGram(int type, Random rand, int physio2){
		//type == 0 is for normal
		//type == 1 is for physiochemical
		//type == 2 is for physiochemical2
		//length would be from have a mean of 1
		int length = (int) (Math.abs(rand.nextGaussian() * 10) + 1);
		switch(type){
		case 0: return physiochemical2Gram(rand,length,0);
		case 1: return physiochemicalGram(rand,length);
		case 2: return physiochemical2Gram(rand,length,physio2);
		default: throw new Error("Unhandled case: " + type);
		}		
	}
	protected static int getMistakeAllowed(Random rand, int length){
		//limit the mistakeAllowed to be from 0 to length/2 with a exponential distribution*
		int mistakeAllowed = (int)(rand.nextGaussian()*10);
		if(mistakeAllowed < 0)
			mistakeAllowed = 0;
		else if(mistakeAllowed > length*0.5)
			mistakeAllowed = (int)(length*0.5);
		return mistakeAllowed;
	}
	//public void setScore(double score){this.score = score;}	
	final public double getFitnessScore(){return this.fitnessScore;}
	final public double getCorrelationScore(){return this.correlationScore;}
	final public void setFitnessScore(double fitnessScore){this.fitnessScore = fitnessScore; this.score = fitnessScore;}
	final public void setCorrelationScore(double correlationScore){this.correlationScore = correlationScore;}
	final public void increaseCorrelationScore(double increment){this.correlationScore += increment;}
	final public void computeScore(double maxFitnessScore, double totalCorrelationScore){
		/*
		 * Based on fitness score and correlation
		 */
		if(totalCorrelationScore > 0 && maxFitnessScore > 0)
			this.score = ((this.fitnessScore/maxFitnessScore) * 0.5) + ((1 - (this.correlationScore / totalCorrelationScore)) * 0.5);
		else 
			this.score = this.fitnessScore;
	}
	protected void mutateWindow(Random rand, int windowMin, int windowMax){			
		if(this.isPercentage){//move by 0-10%
			do{
				int moveBy = rand.nextInt(10);
				windowFrom = randomBetween(windowFrom - moveBy, windowFrom + moveBy, rand);
				windowTo = randomBetween(windowTo - moveBy, windowTo + moveBy, rand);
			}while(windowFrom > windowTo || windowFrom < 0 || windowTo > 100);
		}else{//move by 0-30 positions					
			do{
				int moveBy = rand.nextInt(30);
				windowFrom = randomBetween(windowFrom - moveBy, windowFrom + moveBy, rand);
				windowTo = randomBetween(windowTo - moveBy, windowTo + moveBy, rand);
			}while(windowFrom > windowTo || windowFrom < windowMin || windowTo > windowMax);
		}
	}
	private String clearEndsX(String kgram){
		//get rid of front X
		while(kgram.length() >= 1 && kgram.charAt(0) == 'X'){
			if(kgram.length() >= 2)
				kgram = kgram.substring(1);
			else
				kgram = "";
		}
		//get rid of ending X
		while(kgram.length() >= 1 && kgram.charAt(kgram.length() - 1) == 'X'){
			if(kgram.length() >= 2)
				kgram = kgram.substring(0,kgram.length() - 1);
			else
				kgram = "";
		}
		return kgram;
	}
	final protected String mutateKgram(String kgram, Random rand, int type, int physio2){		
		int numOfCase = 6;
		if(kgram.length() == 1)
			numOfCase -= 4;//since if length is 1, it cannot remove or swap
		int index = rand.nextInt(numOfCase);	
		String returnString;
		switch(index){
		//insertion
		case 0: returnString = clearEndsX(add(kgram,rand,type,physio2)); break;			
		//mutation
		case 1: returnString = clearEndsX(change(kgram,rand,type,physio2)); break;
		//swap
		case 2: returnString = clearEndsX(swap(kgram,rand)); break;
		//remove - I am giving more chance to reducing kgram length because shorter kgram would be more understandable and generic
		case 3: 
		case 4:
		case 5: returnString = clearEndsX(remove(kgram,rand)); break;			
		default: throw new Error("Unhandled case");
		}
		if(returnString.length() == 0)
			returnString = Feature.getGram(type,rand,physio2);
		return returnString;
	}
	private String change(String kgram, Random rand, int type, int physio2){
		char changeChar;
		switch(type){
		case 0: changeChar = physiochemical2Gram(rand,0); break;
		case 1: changeChar = physiochemicalGram(rand); break;
		case 2: changeChar = physiochemical2Gram(rand,physio2); break;
		default: throw new Error("Unhandled case: " + type);
		}		
		int changeIndex = rand.nextInt(kgram.length());		
		String returnString = "";
		for(int x = 0; x < kgram.length(); x++)
			if(changeIndex == x)
				returnString += changeChar;
			else
				returnString += kgram.charAt(x);
		return returnString;
	}
	private String add(String kgram, Random rand, int type, int physio2){
		char addChar;
		switch(type){
		case 0: addChar = physiochemical2Gram(rand,0); break;
		case 1: addChar = physiochemicalGram(rand); break;
		case 2: addChar = physiochemical2Gram(rand,physio2); break;
		default: throw new Error("Unhandled case: " + type);
		}		
		int addIndex = rand.nextInt(kgram.length());		
		String returnString = "";
		for(int x = 0,y = 0; x < kgram.length() + 1; x++)
			if(addIndex == x)
				returnString += addChar;
			else{
				returnString += kgram.charAt(y);
				y++;
			}
		return returnString;
	}
	private String swap(String kgram, Random rand){				
		int index1 = rand.nextInt(kgram.length());
		int index2 = rand.nextInt(kgram.length());
		while(index2 == index1)
			index2 = rand.nextInt(kgram.length());		
		char char1 = kgram.charAt(index1);
		char char2 = kgram.charAt(index2);
		String returnString = "";
		for(int x = 0; x < kgram.length(); x++)
			if(x == index1)
				returnString += char2;
			else if(x == index2)
				returnString += char1;
			else
				returnString += kgram.charAt(x);		
		return returnString;
	}
	private String remove(String kgram, Random rand){
		int mutatePoint = rand.nextInt(kgram.length());
		if(mutatePoint != kgram.length() - 1)
			return kgram.substring(0, mutatePoint) + kgram.substring(mutatePoint+1);
		else 
			return kgram.substring(0, mutatePoint);
	}
	//Non-GA
	//String used to saved into Step2_Settings.txt
	//Note that as of 3 August 2009 - Only ClassifierFeature uses saveDirectory
	abstract public String saveString(String saveDirectory);	
	abstract public Object computeDNA(FastaFormat fastaFormat);
	abstract public Object computeProtein(FastaFormat fastaFormat,
			int scoringMatrixIndex,int countingStyleIndex,ScoringMatrix scoringMatrix);	

	final public Object get(int col){
		if(col == 1)
			return box;
		else if(col == 2)
			return name;
		else 
			return details;
	}

	final public boolean isPercentage(){ return this.isPercentage; }
	final public String getDetails(){ return this.details; }
	final public String getName(){ return this.name; }
	final public void setName(String name){this.name = name;}
	final public void invertBox(){ this.box = !this.box; }
	final public void setBox(boolean box){ this.box = box; }	
	final public boolean isMarked(){ return this.box; }	
	final public int getWindowFrom(){ return this.windowFrom; }	
	final public void setWindowFrom(int windowFrom){ this.windowFrom = windowFrom; }
	final public int getWindowTo(){ return this.windowTo; }
	final public void setWindowTo(int windowTo){ this.windowTo = windowTo; }
	final public char getType(){ return this.type; }
	final public void setType(char type){ this.type = type; }	
	//note that this is okie if we do not consider 0 within our window
	final public int windowSize(){ return (this.windowTo - this.windowFrom + 1); }

}