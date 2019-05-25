# Searching-For-Patterns
A regular expression finite state machine compiler and corresponding pattern matcher. 

## Compiler.java
Takes a regular expression as a command line argument enclosed within double quotes.  Outputs to standard output a description of the corresponding finite state machine.  Each line of output includes the state numbeer, the symbol(s) this state must match (or branch state indicator) and two numbers indicating the two possible next states if a match
is made. 

## Usage
``` bash
$ java Compile "<regexp>" | java Search <filename>  
```

## Searcher.java
Takes the output of Compiler.java as standard input and searches for matching patterns within the text of the file that is specified as a command line argument.  Each line of the text file that contains a match is outputted to standardoutput once. 

## Regular Expression Speficication
1. Any symbol that does not have a special meaning (as given below) is a literal that matches itself
2. . is a wildcard symbol that matches any literal
3. Adjacent regexps are concatenated to form a single regexp
4. \* indicates closure (zero or more occurrences) on the preceding regexp
5. \+ indicates that the preceding regexp can occur one or more times
6. ? indicates that the preceding regexp can occur zero or one time
7. | is an infix alternation operator such that if r and e are regexps, then r|e is a regexp that matches one of either r ore
8. ( and ) may enclose a regexp to raise its precedence in the usual manner; such that if e is a regexp, then (e) is a regexp and is equivalent to e. e cannot be empty.
9. [ and ] may enclose a list of literals and matches one and only one of the enclosed literals. Any special symbols in the list lose their special meaning, except ] which must appear first in the list if it is a literal. The enclosed list cannot be empty.
10. ![ and ]! may enclose a list of literals and matches one and only one literal NOT included in the enclosed literals. Any special symbols in the list lose their special meaning, except ] which must appear first in the list if it is a literal. The enclosed list
cannot be empty.
11. \ is an escape character that matches nothing but indicates the symbol immediately following the backslash loses any special meaning and is to be interpretted as a literal symbol

## Precedence
Operator precedence is as follows (from high to low):
- Escaped characters (i.e. symbols preceded by \)
- Parentheses (i.e. the most deeply nested regexps have the highest precedence)
- List of alternative literals (i.e. [ and ] or ![ and ]!)
- Repetition/option operators (i.e. *, + and ?)
- Concatenation
- Alternation (i.e. |)

## Notes
- The alternation implmentation has the lowest precedence when each of the two halves are enclosed by ( ) Without the ( ), alternation is one place higher in the list of precedence - above concatenation, instead of below it. 
- With our implementation of ![ ]!, each symbol enclosed in this is added to our FSM with its next states pointing to -1 which is finally followed by a state with a wildcard symbol since by the time the wildcard has been read in, the Searcher already knows what it cannot match.
- An ! on its own is considered a literal. It is only special when it is used in the scenario ![ ]!
- For the terminal to accept an exclamation mark followed by an open square bracket - ![ ]!, 
  we used this command in the terminal: set +H
- The FSM final state points back to state 0 however the Searcher knows this is the final state.
