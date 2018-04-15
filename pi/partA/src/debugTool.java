import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;


public class debugTool {
	public static String filename;
	public static int T_SUPPORT;
	public static int T_CONFIDENCE;
	
    public static void main(String[] args) throws IOException {
    	// Threshold for support, defaults to 3.
    	// Support is the total number of times a pair of functions appear together.
    	T_SUPPORT = 3;
    	// Threshold for confidence, defaults to 65%.
    	// Confidence is the percentage of the number of times a pair of functions 
    	// appear together divided by the number of times a function appears alone.
    	// f.ex support {a, b} / support {a}.
    	T_CONFIDENCE = 65;
    	// The input file that we are working with.
    	filename = "";
    	// If only one argument is provided we use the default values for support and confidence.
    	// First argument corresponds to the filename.
    	if(args.length == 1) {
            filename = args[0];
        }
    	// If three arguments are provided the first corresponds to the filename
    	// the second corresponds to the support and the third to the confidence.
        else if(args.length == 3) {
            filename = args[0];
            T_SUPPORT = Integer.parseInt(args[1]);
            T_CONFIDENCE = Integer.parseInt(args[2]);
        }
    	// If an incorrect number of arguments is provided.
        else{
        	System.out.println("Input arguments must be of the form <bitcode file> or <bitcode file><SUPPORT><CONFIDENCE>");
            System.exit(-1);
        }
    	start();
    }
        
         // A map that for each function registers what functions are called from it.
        // <key:  The caller function> <value: A Set of called functions>.
    	static Map<String, ? extends Set<String>> calledFromFunctionMap = new HashMap<String, Set<String>>();
        
        // A map that registers how often a function is called from inside another function
        // <key: name of the function> <value: the number of times it is called>
        static Map<String,Integer> numberOfTimesCalled = new HashMap<String, Integer>();
        
        // Getter for numberOfTimesCalled
        static Map<String, Integer> getNumberOfTimesCalled() {
        	return numberOfTimesCalled;
        }
        
        // A map that registers how often a pair of functions are called together, 
        // that is the support for a pair of functions.
        // <key: the name of the first function> <value: A map with <key: name of the second function> <value: number of occurrences>.
        static Map<String, HashMap<String, Integer>> pairOccurrenceMap = new HashMap<String, HashMap<String, Integer>>();
        
        // Getter for pairOccurrenceMap
        static Map<String, HashMap<String, Integer>> getPairOccurrenceMap() {
        	return pairOccurrenceMap;
        }

        static void start()  {
            // Parse the callgraph file into a Map
        	HashMap<String, HashSet<String>> newCalledFromFunctionMap = parse(filename);
        	calledFromFunctionMap = newCalledFromFunctionMap;
        	// At this point we have a map that contains all functions and the functions called from each one.
        	// Now we need to count how often each function is called from another functions.
        	countOccurrences();
        	
        	// At this point we have both a map of all functions and who they call
        	// and a map of how often each function is called.
        	// Now we need to count how often pairs of functions appear together.
        	countPairOccurrences();
        }
        
        static void countPairOccurrences() {
        	Map<String, HashMap<String, Integer>> pairOccurrenceMap = getPairOccurrenceMap();
        	//Map<String, ? extends Set<String>> calledFromFunctionMap = getCalledFromFunctionMap();
        	// Go through all functions and the functions they call.
        	for (Entry<String, ? extends Set<String>> entry : calledFromFunctionMap.entrySet()){
        		// Go though the called functions and generate all pairs.
        		Object[] entryList = entry.getValue().toArray();
    	    	for (int i = 0; i < entry.getValue().size(); i++) {
    	    		// The first value of the pair.
    	    		String first = (String) entryList[i];
    	            for (int j = i + 1; j < entry.getValue().size(); j++) {
    	            	// The second value of the pair.
    	            	String second = (String) entryList[j];
    	            	// For each pair, we add the occurrences of those pairs to the Map. (a,b) and (b,a).
    	            	growHash(first, second);
    	            	growHash(second, first);
    	            }
    	    	}
        	}
        	
        	// Find all bugs that satisfy bug criteria(support, confidence).
        	//ArrayList<String> bugs = new ArrayList();
        	// For all callers in the map of functions and the functions they call.
        	for (String caller : calledFromFunctionMap.keySet()) {
        		// Get a set of callers get a set of all callers that caller calls.
        		Set<String> allCallees = calledFromFunctionMap.get(caller);
        		// then loop through all the callees in that set.
        		for (String callee : allCallees) {
        			// Guard against looking in fn that is not called, otherwise nullPointerEx.
            		if (numberOfTimesCalled.get(callee) == null){
        				continue;
        			}
            		// Get the total number of calls to the fn. F.ex support(A).
        			int count = numberOfTimesCalled.get(callee);
        			
        			// Create a map that contains a function and the number of times it is called along with the current callee.
        			HashMap<String, Integer> countInPair = pairOccurrenceMap.get(callee);
            		// Guard against getting number of pair occurrences where none exist , otherwise nullPointerEx.
        			if (pairOccurrenceMap.get(callee) == null){
        				continue;
        			}
        			// Loop through all occurrences of a pair being called together
        			for(String occurrence : countInPair.keySet()){
        				// Guard against printing duplicate bugs. Only where the violation occurs
        				if (allCallees.contains(occurrence)){
        					continue;
        				}
        				// Get the support value for the pair
        				int howOftenInPair = countInPair.get(occurrence);
                        

        				
        				// print the bug
        				if(howOftenInPair >= T_SUPPORT){
                            // Calculate the confidence level for that pair
                            double confidence = ((double)howOftenInPair / count) * 100;
                            
                            if(confidence >= T_CONFIDENCE ){
            					String msg = "bug: " + callee + " in " + caller + ", pair: ";
            					String[] pairArr = {callee, occurrence};
            					Arrays.sort(pairArr);
            					String pair = "(" + pairArr[0] + ", " + pairArr[1] + ")";
            					msg += pair;
            					// Fix confidence so that it prints two decimal places
            					//https://stackoverflow.com/questions/8819842/best-way-to-format-a-double-value-to-2-decimal-places
            					DecimalFormat df = new DecimalFormat("#.00"); 
            					msg += ", " + "support: " + howOftenInPair + ", confidence: " + df.format(confidence) + "%";
            					System.out.println(msg);
        				}
        			}
                }
                
        		}
        	}

    	}
        
        static void growHash(String first, String second){
        	Map<String, HashMap<String, Integer>> newPairOccurrenceMap = getPairOccurrenceMap();
        	// If the map of pairs does not contain the first key then.
        	if(!newPairOccurrenceMap.containsKey(first)){
        		// create a new map of occurrences for second item in the pair.
        		Map<String, Integer> occurrenceSecond = new HashMap<String, Integer>();
        		// Create a new entry for the second item in the pair
        		occurrenceSecond.put(second, 1);
        		// Add the map of occurrenceSecond to the map that counts pairs.
        		newPairOccurrenceMap.put(first, (HashMap<String, Integer>) occurrenceSecond);
        	}
        	// If the map of pairs contains a key for the first fn in pair.
        	else{
        		Map<String, Integer> occurrenceSecond = newPairOccurrenceMap.get(first);
        		if(!occurrenceSecond.containsKey(second)){
        			// Create a new entry for the second item in the pair.
        			occurrenceSecond.put(second, 1);
        		}
        		else{
        			// if it exists then increment it.
        			occurrenceSecond.put(second, occurrenceSecond.get(second) + 1);
        		}
        	}
        	pairOccurrenceMap = newPairOccurrenceMap;
        }

    	static void countOccurrences() {
        	Map<String, Integer> newNumberOfTimesCalled = getNumberOfTimesCalled();
        	// https://stackoverflow.com/questions/46898/how-to-efficiently-iterate-over-each-entry-in-a-map
    		for (Entry<String, ? extends Set<String>> entry : calledFromFunctionMap.entrySet()){
    		    for (String callee : entry.getValue()) {
    		    	//https://stackoverflow.com/questions/4363665/hashmap-implementation-to-count-the-occurences-of-each-character
    		    	if(numberOfTimesCalled.containsKey(callee)){
    		    		// If the entry exists, increment the number of occurrences by one
    		    		numberOfTimesCalled.put(callee, numberOfTimesCalled.get(callee) + 1);
    		    	}
    		    	else{
    		    		// Enter the callee into the map
    		    		numberOfTimesCalled.put(callee, 1);
    		    	}
    			}
    		}
    		numberOfTimesCalled = newNumberOfTimesCalled;
    	}

    	// Parse the Callgraph
        static HashMap<String, HashSet<String>> parse(String filename)  {
        	HashMap<String, HashSet<String>> calledFromFunctionMap = new HashMap<String, HashSet<String>>();;
        	// Reader that reads the callgraph.
        	FileReader fileReader = null;
    		BufferedReader bufferedReader = null;
    		try {
    			fileReader = new FileReader(filename);
    			bufferedReader = new BufferedReader(fileReader);
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    			System.out.println(e);
    		}
            String singleLine = "";
            String caller = "";
            // Flag for knowing if the current method is a null function
            boolean isNullFn = false;
            HashSet<String> calledInside = new HashSet<String>();
            // Read every line of the callgraph.
            try {
    			while ((singleLine = bufferedReader.readLine()) != null){
    				// Ignore null function nodes entirely.
    				if (singleLine.contains("<<null function>>")){
    					isNullFn = true;
    					continue;
    				}
    				
    				// Extract the name of the caller function.
    				if (singleLine.contains("Call graph node for function")){
    					isNullFn = false;
    					// Create array of words.
    			        String[] wordArray = singleLine.split(" ");
    			        // Get the name of the caller from the array.
    			        caller = wordArray[5].substring(1, wordArray[5].indexOf("'", 1));
    				}
    				
    			    // Extract all called functions.
    			    else if (!isNullFn && singleLine.contains("calls function")){    
    			    	// Create array of words.
    			        String[] wordArray = singleLine.split(" ");
    			        // Get the name of the called function from the array
    			        String calleeName = wordArray[5].substring(1, wordArray[5].indexOf("'", 1));
    			        // Add the called function to the set of called functions
    			        calledInside.add(calleeName);
    			    }
    				
    			    // If we find whitespace the callgraph for the current function we know it has ended.
    			    else if ((singleLine.isEmpty() && caller != "")) {
    			    	// Add all the called functions for the current caller to the map
    			    	if(calledInside.size() > 0){
    			    		calledFromFunctionMap.put(caller, calledInside);
    			    	}
    			        calledInside = new HashSet<String>();
    			    }
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
            try {
    			bufferedReader.close();
    			fileReader.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    			System.out.println("Unable to close reader");
    		}
            return calledFromFunctionMap;
        }
    }