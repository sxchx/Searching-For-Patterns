/**
 *  Pattern Searching
 *  Compiler.java
 *  Accepts a regexp pattern as a command-line argument (enclosed within double-quotes),
 *	and produces, as standard output, a description of the corresponding FSM, such that 
 *	each line of output includes four things: the state-number, the input-symbol(s) 
 *	this state must match (or branch-state indicator), and two numbers indicating the 
 *	two possible next states if a match is made.
 *  
 *  GRAMMAR
 *	--------
 *	E -> A
 *	E -> A E
 *  A -> T
 *	A -> T|A
 *	T -> F 
 *	T -> F*
 *	T -> F?
 *	T -> F+
 *  F -> \V 
 *  F -> V
 *  F -> .
 *  F -> (E)
 *  F -> [L]
 *  F -> ![L]!
 *
 *	E = expression 
 *	A = alternation 
 *  T = term 
 *	F = factor 
 *	V = vocab/literal
 *  L = list of literals
 *
 * 
 *  Authors: Sacha Raman and Elizabeth Macken
 *  
 */
public class Compiler {
	
	//the position in the regexp
	private static int j;
	//an array to store the regexp
	private static char[] p;
	//array to store character expected to read
	private static char[] ch = new char[100];
	//array to store all special symbols
	private static char[] special = new char[] {'.', '*', '+', '?', '|', '(', ')', '[', ']', '!', '\\'};
	//array to store next two states
	private static int[] next1 = new int[100];
	private static int[] next2 = new int[100];
	//state number
	private static int state = 1;
	//flag for closure
	private static boolean closure = false;
	//counter for parentheses ( )
	private static int parentheses = 0;
	//char for empty - right ways dashed arrow
	private static char empty = '\u21E2';
	//char for branch - branch shaped symbol
	private static char branch = '\u2387';
	//char for finished - rightways arrow to bar
	private static char finished = '\u21E5';
	//char for wild - tiny star
	private static char wild = '\u2b51';
	
	
	//takes a state number, a character and the next two states to store into our next two arrays
	private static void setState(int s, char c, int n1, int n2){
		//store values into array for that state
		ch[s] = c;
		next1[s] = n1;
		next2[s] = n2;
	}
	
	//checks if a symbol is a literal (vocab) or is special
	//returns true if vocab or false if special
	private static boolean isVocab(char c) {
		for(int i = 0; i < special.length; i++) {
			if(c == special[i]) {
				return false;
			}
		}
		return true;
	}
	
	//print the three arrays pretty to standard error
	private static void printFSMPretty() {
		System.err.println("s   ch  1   2  ");
		System.err.println("---+---+---+---");
		for(int i = 0; i <= state; i++) {	
			System.err.println(i + "  | " + ch[i] + "  " + next1[i] + "   " + next2[i]);
		}
		System.err.println();
	}
	
	//print the fsm to standard output to be read in by the REsearch
	private static void printFSM() {
		String symbol;
		for(int i = 0; i <= state; i++) {
			if(ch[i] == empty || ch[i] == branch) {
				symbol = "BRANCH";
			}
			else if(ch[i] == ' ') {
				symbol = "SPACE";
			}
			else if(ch[i] == wild) {
				symbol = "WILD";
			}
			else if(ch[i] == finished) {
				symbol = "FINISHED";
			}
			else {
				symbol = Character.toString(ch[i]);
			}
			System.out.println(i + " " + symbol + " " + next1[i] + " " + next2[i]);
		}
	}
	
	//method called when an error occurs while parsing and compiling the fsm
	private static void error(String errorMessage) {
		System.err.println("Error: " + errorMessage);
		//return
		System.exit(0);		
	}

	//method called by main, sets the initial state, calls expression()
	private static void parse() {
		int initial;
		//set the 0 state (temporarily)
		setState(state - 1, empty, 1, 1);
		//call expression()
		initial = expression();
		//update the 0 state
		setState(0, empty, initial, initial);
		//set the final state to point to start
		setState(state, finished, 0, 0);
		//print out the fsm pretty to standard error and output
		printFSMPretty();
		printFSM();
	}
	
	//this method calls term and returns an integer r
	private static int expression() {
		
		int r, final1, start;
		//call the alternation method
		r = alternation();
		final1 = state - 1;
		//System.err.println("f1: " + final1);
		if(j < p.length) {
			//if the current character is a literal or anything else in factor or is the start of a expression
			if(isVocab(p[j]) || p[j] == '(' || p[j] == '\\' || p[j] == '[' || p[j] == '!' || p[j] == '.') {				
				//call expression
				start = expression();
				//update pointers
				if(next1[final1] == next2[final1] ) {
					next1[final1] = start;
				}
				next2[final1] = start;
			}
			else if(p[j] == ']') {
				error("Missing [");
			}
			else if (parentheses == 0) {
				error("Missing (");
			}
		}
		return r;
	}
	
	/*
	operator precedence is as follows (from high to low):
		escaped characters (i.e. symbols preceded by \)
		parentheses (i.e. the most deeply nested regexps have the highest precedence)
		list of alternative literals (i.e. [ and ] or ![ and ]!)
		repetition/option operators (i.e. *, + and ?)
		concatenation
		alternation (i.e. |)
	*/
	
	//this method calls term, it deals with alternation | and returns r
	private static int alternation() {
		int r, start1, final1, start2, final2;
		
		r = start1 = term();	
		
		//the end of that term should be the state before our current state
		final1 = state - 1;
		//make r point to the start of the first term too
		//r = start1;
		
		//if it is alternation
		if(j < p.length && p[j] == '|') {
			//if we just did * or + then we dont want to update all states pointing to r
			//just the ones up to r-1
			if(closure == true) {
				for(int i = 0; i < r - 1; i++) {
					if(next1[i] == r) {
					next1[i] = state;
					}
					if(next2[i] == r) {
						next2[i] = state;
					}
				}
				closure = false;
			}
			else {
				//update the pointer of the state before r to point to our new branching state we will create
				for(int i = 0; i < state; i++) {
					//if a state points to r (so the state before r) make it point to the current state
					if(next1[i] == r) {
						next1[i] = state;
					}
					if(next2[i] == r) {
						next2[i] = state;
					}
				}
			}
			
			//move past the |
			j++;
			//r will now keep track of our branching state that will point to the two halves
			r = state;
			//increment state to move past the branching state we are yet to actually create
			state++;
			//call this function and get the start of the second half, stored in start2
			start2 = alternation();
			//set that first branching state we stored in r. 
			//n1 = the start of the first half, stored in t1
			//n2 = the start of the second half, stored in t2
			setState(r, branch, start1, start2);
			//now we need to make another state to bring the ends of the two halfs of | together
			//n1 & n2 will just point to the next state
			setState(state, branch, state + 1, state + 1);
			//the end of the second half of < already points to this new state but not the first half
			//end of the first half was stored in f  (do we need if statement ??)
			if(next1[final1] == next2[final1] ) {
				next1[final1] = state;
			}
			next2[final1] = state;
			//inc state because we made that state to bring both halves together
			state++;
			//dont update r as it already points to the start of the alternation (first branching state)
			
			
			//get the end of the second half, should be the state before our current state
			//final2 = state - 1;			
		}
		return r;
	}
	
	//this method calls factor, it deals with | alternation, * (closure) and two 
	//other variations of closure symbolised by + and ?
	//returns r
	private static int term() {
		
		int r;
		int t1;
		int t2 = 0;
		int f;
		
		f = state - 1;
		
		//call the factor method
		r = t1 = factor();
		
		// * - indicates closure (zero or more occurrences) on the preceding regexp
		if(j<p.length && p[j] == '*') {
			//create a branching state. n1 = start of what is repeated. n2 = next state
			setState(state, branch, r, state + 1);		
			
			//WE WANT THE STATE THAT POINTS TO R (the state before r). 
			//r stores the start of whatever it is we are repeating
			//THIS IS NOT NECESSARILY ALWAYS GOING TO BE STATE R-1
			//so the following four lines of code won't exactly work
		//	if(next1[r-1] == next2[r-1]) {
		//		next2[r-1] = state;
		//	}
		//	next1[r-1] = state;
		
			//instead we will iterate through all states to find the state that points to r 
			for(int i = 0; i < state; i++) {
				//if it points to r make it point to the current state
				if(next1[i] == r) {
					next1[i] = state;
				}
				if(next2[i] == r) {
					next2[i] = state;
				}
			}
			//update r
			r = state;
			//update t1
			t1 = state;
			j++;
			state++;
			closure = true;
		}
		// ? - indicates that the preceding regexp can occur zero or one time
		else if (j <p.length && p[j] == '?') {
			//create a branching state. n1 = the start of what is repeated. n2 = the next state
			setState(state, branch, r, state + 1);
			//iterate through all states
			for(int i = 0; i < state; i++) {				
				//if any states point to the current state
				// then make it point instead to the next state (to be created)
				if(next1[i] == state) {
					next1[i] = state + 1;
				}
				if(next2[i] == state) {
					next2[i] = state + 1;
				}				
				//if it points to r make it point to the current state
				if(next1[i] == r) {
					next1[i] = state;
				}
				if(next2[i] == r) {
					next2[i] = state;
				}
			}			
			//update r
			r = state;
			//update t1
			t1 = state;
			j++;
			state++;
			setState(state, branch, state + 1, state + 1);
			state++;
		}
		// + - indicates that the preceding regexp can occur one or more times
		else if (j < p.length && p[j] == '+') {
			//create a branching state. n1 = the start of what is repeated. n2 = the next state
			setState(state, branch, r, state + 1);
			//we do not update r in this situation as it remains unchanging
			j++;
			state++;
			closure = true;
		}
		
		return r;
	}
	
	//this method is called in [ ] to deal with the last two items in the list of literals
	//it takes: - the literals stored in a StringBuilder
	// - the index of the char in the StringBuilder
	// - the final state in the fsm
	public static void finalListItems(StringBuilder sb, int index, int finalState) {
		//set a branching state pointing to the next two states
		setState(state, branch, state + 1, state + 2);
		state++;
		//set the one character from our list, pointing to final state
		setState(state, sb.charAt(index), finalState, finalState);
		state++;
		//set another character from our list, also pointing to final state
		setState(state, sb.charAt(index+1), finalState, finalState);
		state++;
		//set a branching state going to the next state
		setState(state, branch, state + 1, state + 1);
		state++;
	}
	
	// This method deals with literals, wildcard, escape characters, [ ], and ![ ]!
	//it returns r
	private static int factor() {
		
		int r = state;
		
		//check if it is a ! on its own and not actually the start of ![]!
		if(j < p.length && p[j] == '!') {
			//if the following character is not a [
			if(((j+1) < p.length && p[j+1] != '[') || (j+1) >= p.length) {
				//then we want to treat ! as a literal
				setState(state, p[j], state+1, state+1);
				j++;
				r = state;
				state++;
				return r;
			}
		}
		//if the character is an escaped character: \ .
		//in this case the symbol after this is considered as a literal
		if(j < p.length && p[j] == '\\') {
			//get the next symbol
			j++;
			//set it as if it were a literal
			setState(state, p[j], state + 1, state + 1);
			j++;
			r = state;
			state++;
		}
		//if the character is a literal or a wildcard
		else if(j < p.length && (isVocab(p[j]) || p[j] == '.')) {
			if(p[j] == '.') {
				setState(state, wild, state + 1, state + 1);
			}
			else {
				setState(state, p[j], state + 1, state + 1);
			}
			j++;
			r = state;
			state++;			
		}
		else {
			//if the character is the start of an expression
			if(j < p.length && p[j] == '(') {
				//increment parentheses counter
				parentheses++;
				j++;
				//if the next character we see is a ) then this is invalid
				if(j < p.length && p[j] == ')') {
					error("Empty ( )");
				}				
				r = expression();			
				//if the character is the end of an expression
				if(j < p.length && p[j] == ')') {
					//decrement the parentheses counter
					parentheses--;
					j++;
				}
				//if the open bracket dont have a closing bracket
				else {
					error("Opening bracket does not have a closing bracket");
				}
			}
			//[ and ] may enclose a list of literals and matches one and only one of the enclosed 
			//literals. Any special symbols in the list lose their special meaning, except ] which
			//must appear first in the list if it is a literal. The enclosed list cannot be empty.
			else if(j < p.length && p[j] == '[') {
				//make a new StringBuilder to store our list of literals
				StringBuilder sb = new StringBuilder();
				//inc j to move past [
				j++;
				//if ] is first literal inside
				if(j < p.length && p[j] == ']') {
					//then stick ] into the sb
					sb.append(p[j]);
					//inc j to move past ]
					j++;	
				}
				//while we have not reached the end
				while(j < p.length && p[j] != ']') {
					//stick p[j] into sb
					sb.append(p[j]);
					//inc j
					j++;
				}
				//if we come to the end and we dont see a ]
				if(j >= p.length) {
					error("No closing ]");
				}
				
				//we have seen a ] so now move past it
				j++;
				//get the length of our sb
				int stringLength = sb.length();
				//get the final state we will have to make for this [] thing
				int finalState = (stringLength * 2) + state - 1;
				//if there is only one thing in sb
				if(stringLength == 1) {
					//set the next state with this literal
					setState(state, sb.charAt(0), state + 1, state + 1);
					state++;
				}
				//if there are two things in sb
				else if (stringLength == 2) {
					finalListItems(sb, 0, finalState);
				}
				//if there are more than two things in sb
				else {
					int x = 0;
					//while we havnt passed the stringLength - 2
					while(x < stringLength - 2) {
						//create a branching state pointing to the next two states
						setState(state, branch, state + 1, state + 2);
						state++;
						//create a state for one of the list items pointing to the finalState
						setState(state, sb.charAt(x), finalState, finalState);
						state++;
						x++;
					}
					finalListItems(sb, stringLength - 2, finalState);
				}
				
			}		
			
			//![ and ]! may enclose a list of literals and matches one and only one literal 
			//NOT included in the enclosed literals. Any special symbols in the list lose 
			//their special meaning, except ] which must appear first in the list if it is a literal. 
			//The enclosed list cannot be empty.
			else if((j < p.length && p[j] == '!') && (j+1 < p.length && p[j+1] == '[')) {
				//a string builder to store our literals
				StringBuilder sb = new StringBuilder();
				//a counter to keep track of how much we inc j incase we gotta backtrack
				int counter = 0;
				//move past the ![
				j+=2;
				counter+=2;
				//if ] is first literal inside
				if(j < p.length && p[j] == ']') {
					//stick ] into the sb
					sb.append(p[j]);
					//inc j to move past ]
					j++;
					counter++;
				}
				//while we haven't reached the end
				while(j < p.length && p[j] != ']') {
					//stick p[j] into the sb
					sb.append(p[j]);
					//inc j 
					j++;
					counter++;
				}
				//now we have come to the end we expect a ]!
				if(j < p.length && p[j] == ']') {
					//move past ]
					j++;
					counter++;
					//check next character is a !
					if(j < p.length && p[j] == '!') {
						//move past !
						j++;
						counter++;
						//get the length of the sb
						int stringLength = sb.length();
						//for each literal in our sb
						for(int i = 0; i < stringLength; i++) {
							//create a branching state pointing to next two states
							setState(state, branch, state + 2, state + 1);
							state++;
							//create a state that goes essentially nowhere
							setState(state, sb.charAt(i), -1, -1);
							state++;
						}
						//create a wildcard state as it takes everything
						//the REsearch function knows to investigate next2 before next1
						setState(state, wild, state+1, state+1);
						state++;
					}
					//if the next character is not a ! then its probably just a literal ! followed by a []
					//in this case we want to back track, add in the ! as a literal
					else {
						//backtrack 
						j = j - counter;
						//add in the !
						setState(state, p[j], state+1, state+1);
						j++;
						r = state;
						state++;	
					}
				}
				//if we dont find a ] or have gone outta bounds, call error
				else {
					error("No closing ]");
				}
			}
			
			//if the character isnt a literal or an opening bracket
			else {
				error("Character is not a literal, wildcard or opening bracket");
			}
		}
		return r;
	}
	
	public static void main(String[] args) {
		if(args.length != 1) {
			//Check that we have received 1 argument
			System.err.println("Usage: java Compiler <regexp>");
			return;
		}
		
		//turn our input string into a char array
		String s = args[0];
		p = s.toCharArray();
		//initialize the index
		j = 0;
		
		//call the parse() method
		parse();		
	}
}
