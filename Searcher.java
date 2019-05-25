// Authors: Elizabeth Macken and Sacha Raman

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.io.PrintWriter;
import java.io.FileReader;

public class Searcher {
	public static void main(String[] args) {
		// Checking that at least one argument was passed (the file)
		if (args.length != 1) {
			// If not, printing out an error message and exiting the program
			System.err.println("ERROR - Correct usage: java Searcher [fileToSearch]");
			System.exit(1);
		}
		else {
			try {
				// Getting the finite state machine descriptions coming in from the REcompile program
				BufferedReader fsmDescription = new BufferedReader(new InputStreamReader(System.in));

				// Creating a finite state machine object
				FSM fsm = new FSM();

				// While there is still data coming in...
				String newState = null;
				while ((newState = fsmDescription.readLine()) != null) {
					// Create an array to hold the 4 pieces of information each line provides
					String[] arrNewState = new String[4];
					// Split the line at the spaces and store in this array
					arrNewState = newState.split(" ");
					// Checks the line has the right amount of information, else exits
					if (arrNewState.length != 4) {
						System.err.println("ERROR - Input does not have correct number of items (4) per line");
						System.exit(1);
					}
					// Adds that state information to the FSM
					fsm.add(arrNewState[1], Integer.parseInt(arrNewState[2]), Integer.parseInt(arrNewState[3]));
				}

				// No more data coming in, so we create a finished state and close the system.in reader
				//fsm.add("FINISHED", -1, -1);

				fsmDescription.close();

				// Create a new reader to read from the passed argument file to search
				BufferedReader reader = new BufferedReader(new FileReader(args[0]));
				// Create new writer to output to system.out
				PrintWriter writer = new PrintWriter(System.out);

				// Initialise variables outside the loop
				String line = null;
				boolean found;
				int mark;
				int index;
				String[] lineArr;
				// While there is a new line to be read in the file to search...
				while ((line = reader.readLine()) != null) {
					// Reset variables
					found = false;
					mark = 0;
					index = 0;
					// Split the line at each character and store in an array
					lineArr = line.split("");
					// Create 2 linkedLists to store possible current states and possible next states
					LinkedList<Integer> possCurrStates = new LinkedList<Integer>();
					LinkedList<Integer> possNextStates = new LinkedList<Integer>();
					// While we still havent found a match
					while (found == false && mark <= lineArr.length) {
						// Start at the start of the FSM (state 0)
						possCurrStates.add(0);

						// While we still have possible current states we can be in...
						while (possCurrStates.size() > 0) {	
							// Get the state number of the first in the list of possible current states
							int currStateNum = possCurrStates.remove();
							String currStateData = fsm.getData(currStateNum);
							// Check whether that state is the end state...
							if (currStateData.equals("FINISHED")) {
								// If so, say we have found a match, clear the possible next states, and break out of the inner loop
								found = true;
								possNextStates.clear();
								break;
							}
							// Check whether the current state is a branching state...
							else if (currStateData.equals("BRANCH")) {
								// If so, add the next states to the list of possible current states
								int[] nextStates = fsm.getNextStates(currStateNum);
								possCurrStates.addFirst(nextStates[0]);
								possCurrStates.addFirst(nextStates[1]);
							}
							// Check if data of the current state equals the character we are currently reading from the line...
							else if (currStateData.equals("WILD") || (mark + index < lineArr.length && currStateData.equals(lineArr[mark + index]))) {
								// If it does, add the next states of this state to the list of possible next states
								int[] nextStates = fsm.getNextStates(currStateNum);
								if (nextStates[0] != -1) {
									possNextStates.addFirst(nextStates[0]);
									// First, check if the two next states are not the same, if they are the same then don't readd the second one
									if (nextStates[0] != nextStates[1]) {
										possNextStates.addFirst(nextStates[1]);
									}
								}
								// If the next state indicates this state is a trap state, because we have already matched then we have failed this match
								else {
									// So delete all possible current states and next states
									possCurrStates.clear();
									possNextStates.clear();
								}
							}
							// If we have no more possible current states we can be in...
							if (possCurrStates.size() == 0) {
								// Check if we still have possible next states we could be in
								if (possNextStates.size() != 0) {
									// If so, add all the possible next states to the list of possible current states
									for (int i = 0; i < possNextStates.size(); i++) {
										possCurrStates.add(i, possNextStates.get(i));
									}
									// Then clear the list of possible next states
									possNextStates.clear();
									// Increment the character in the line to search we are trying to match
									index++;
								}
								// Otherwise, we were unable to find a match starting at this point in the line
								else {
									// So increment the point we are starting from in the line
									mark++;
									// And reset the index to be the start of the FSM
									index = 0;
								}
							}
						}
					}
					// If we exited the above loop, check whether we exited because we found a match
					if (found == true) {
						// If so, output the line we found a match in to system.out
						writer.println(line);
						writer.flush();
					}
				}

				// Finished searching the file so close the reader and writer objects
				reader.close();
				writer.close();
			}
			catch (Exception ex) {
				// Catching all exceptions in main and printing relevant information
				System.err.println("ERROR - " + ex.getMessage());
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}
}

class FSM {
	// A linkedList of states (in order)
	private LinkedList<Node> states_;

	// Public constructor that initialises the linkedList
	public FSM() {
		states_ = new LinkedList<Node>();
	}

	// Public method that adds a new state, with the string value for that state and the two next states
	public void add(String data, int nextState1, int nextState2) {
		// Checking if the data equals "SPACE", indicating a " " is the actual data value
		if (data.equals("SPACE")) {
			// If so, changing it to equal " "
			data = " ";
		}
		// Then creating and adding a new node with the data value, and 2 next states
		Node toAdd = new Node(data, nextState1, nextState2);
		states_.add(toAdd);
	}

	// Returns the next states for the state at the position given in the FSM (not recursive)
	public int[] getNextStates(int index) {
		return states_.get(index).getNextStates();
	}

	// Returns the data value for the states at the position given in the FSM (not recursive)
	public String getData(int index) {
		return states_.get(index).getData();
	}

	// Private inner class of a Node, which stores a max of 2 next states, and a string of data
	private class Node {
		private String data_;
		private int[] nextStates_;

		// Constructor, takes a string value as data, and 2 integer values for next state numbers
		public Node(String data, int state1, int state2) {
			nextStates_ = new int[2];
			nextStates_[0] = state1;
			nextStates_[1] = state2;
			data_ = data;
		}

		// Returns the array of next states of this node
		public int[] getNextStates() {
			return nextStates_;
		}

		// Returns the data value of this node
		public String getData() {
			return data_;
		}
	}
}