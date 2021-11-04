//Evaluator.java
//Jeremy McMahan 2611029
//Summary: this program acts as an interpreter for numberical expressions inputed on a command line-like interface

//edit to loop over expression until a period is hit!
import java.util.Scanner;

public class Evaluator {
	public static void main(String[] args) {
		while (true) {
			Tree expression;
			System.out.print("$ ");
			Grammar G = new Grammar();
			if (G.line[0].equals(".")) {
				break;
			}
			expression = G.Expr();
			System.out.print("  ");
			expression.preFix();
			System.out.println("= " + Evaluate(expression));
		}
	}
	
	public static int Evaluate(Tree t) {
		if (t.element.equals("+")) {
			return Evaluate(t.Left) + Evaluate(t.Right);
		} else if (t.element.equals("-")) {
			return Evaluate(t.Left) - Evaluate(t.Right);
		} else if (t.element.equals("*")) {
			return Evaluate(t.Left) * Evaluate(t.Right);
		} else if (t.element.equals("/")) {
			return Evaluate(t.Left) / Evaluate(t.Right);
		} else {
			return Integer.valueOf(t.element);
		}
	}
}

class Tree {
	String element;
	Tree Left;
	Tree Right;
	
	public Tree() {
		
	}
	
	public Tree(String s) {
		element = new String(s);
	}
	
	void preFix() {
		System.out.print(element + " ");
		if (Left != null) {
			Left.preFix();
		}
		if (Right != null) {
			Right.preFix();	
		}
	}
}
//check for newlines!
class Grammar {
	String nextToken;
	String[] line;
	int linePos;
	
	public Grammar() {
		line = (new Scanner(System.in)).nextLine().split("[ ]+");
		lexme();
	}
	
	void lexme() {
		if (linePos < line.length) {
			nextToken = line[linePos++];
		} else {
			nextToken = "";
		}
	}
	
	void match(String s) {
		if (!nextToken.equals(s)) {
			System.out.println("Error!");
		} else {
			//lexme();
		}
	} 
	
	Tree Expr() {
		Tree LeftNode = Term();
		while (nextToken.equals("+") || nextToken.equals("-")) {
			Tree node = new Tree(nextToken);
			lexme();
			node.Right = Term();
			node.Left = LeftNode;
			LeftNode = node;
		}
		return LeftNode;
	}
	
	Tree Term() {
		Tree LeftNode = Factor();
		while (nextToken.equals("*") || nextToken.equals("/")) {
			Tree node = new Tree(nextToken);
			lexme();
			node.Right = Factor();
			node.Left = LeftNode;
			LeftNode = node;
		}
		return LeftNode;
	}
	
	Tree Factor() {
		Tree node = null;
		if (nextToken.matches("[0-9]+")) {
			node = new Tree(nextToken);
		} else if (nextToken.equals("(")) {
			lexme();
			node = Expr();
			match(")");
		}
		lexme();
		return node;
	}
}