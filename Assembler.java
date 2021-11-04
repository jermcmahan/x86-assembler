import java.io.*;
import java.util.HashMap;
import java.util.ArrayList; 
//Jeremy McMahan 2611029

public class Assembler {
	static int ProgramLength; //length of the input file
	static String FileName;	//name of the file
	static PrintWriter Owriter; //writes to the object file
	static PrintWriter Lwriter; //writes to the listing file
	static BufferedReader br;   //reads the input file
	static HashMap<String, Integer> Symbols; //contains the symbols and their locations
	static ArrayList<Integer> Locations; //holds the location of each line of code
	static final HashMap<String, OP> OPtable = new HashMap<String, OP>(){{
		put("ADDR", new OP(0x90, 2)); put("COMPR", new OP(0xA0, 2)); put("SUBR", new OP(0x94, 2)); put("MULR", new OP(0x98, 2)); put("DIVR", new OP(0x9C, 2)); put("ADD", new OP(0x18, 3)); put("SUB", new OP(0x1C, 3));
	    put("MUL", new OP(0x20, 3)); put("DIV", new OP(0x24, 3)); put("COMP", new OP(0x28, 3)); put("J", new OP(0x3C, 3)); put("JEQ", new OP(0x30, 3)); put("JGT", new OP(0x34, 3)); put("JLT", new OP(0x38, 3));
	    put("JSUB", new OP(0x48, 3)); put("LDCH", new OP(0x50, 3)); put("RSUB", new OP(0x4C, 3)); put("TIX", new OP(0x2C, 3)); put("TIXR", new OP(0xB8, 2)); put("RD", new OP(0xD8, 3)); put("TD", new OP(0xE0, 3));
	    put("WD", new OP(0xDC, 3)); put("STCH", new OP(0x54, 3)); put("CLEAR", new OP(0xB4, 2)); put("LD AX", new OP(0x00, 3)); put("LD BX", new OP(0x68, 3)); put("LD LX", new OP(0x08, 3));
	    put("LD SX", new OP(0x6C, 3)); put("LD TX", new OP(0x74, 3)); put("LD XX", new OP(0x04, 3)); put("ST AX", new OP(0x0C, 3)); put("ST BX", new OP(0x78, 3)); put("ST LX", new OP(0x14, 3));
	    put("ST SX", new OP(0x7C, 3)); put("ST TX", new OP(0x10, 3)); put("ST XX", new OP(0x10, 3));
		}
	}; //holds the instructions, their OP codes, and their lengths
 
	public static void main(String[] args) throws IOException { 
		FileName = args[0];
		br = new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
		Lwriter = new PrintWriter(args[0].substring(0, FileName.indexOf('.')) + ".lst", "UTF-8");
		Owriter = new PrintWriter(args[0].substring(0, FileName.indexOf('.')) + ".obj", "UTF-8");
		Lwriter.println("Listing " + args[0]);
		Symbols = new HashMap<String, Integer>();
		Locations = new ArrayList<Integer>();
		try {
			Pass1();
			Pass2();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		Lwriter.close();
		Owriter.close();
	}
	//the first pass calculates the locations and checks for simple errors
	public static void Pass1() throws Exception {
		int LOCCTR = 0;
		int START = 0;
		int i = 0;
		String line = br.readLine();
		String[] l = line.trim().replace("\t", "").split("[ ]+"); // split first line into words
		if (line.contains("START")) {
			for (i = 0; i < l.length; i++) {
				if (Character.isDigit(l[i].charAt(0))) {
					LOCCTR = Integer.decode("0x" + l[i]);
					break;
				}
			}
			START = LOCCTR;
		} else {
			LOCCTR += getFormat1(line, LOCCTR); // no START
		}
		Locations.add(new Integer(LOCCTR));
		while ((line = br.readLine()) != null) {
			String[] k = line.trim().replace("\t", " ").replace("+", "").replace("@","").replace("#","").replace(",", " ").split("[ ]+");
			// split the line into words and take out special characters
			Locations.add(new Integer(LOCCTR)); // put the previous location into the table
			LOCCTR += getFormat1(line, LOCCTR);
			if (k[0].equals("END")) {
				Locations.add(new Integer(LOCCTR));
				break;
			}
		}
		ProgramLength = LOCCTR - START; 
	}
	//the second pass generates the object code and then outputs the object and listing file
	public static void Pass2() throws Exception {
		br = new BufferedReader(new InputStreamReader(new FileInputStream(FileName)));
		String line = br.readLine(); // the current line of the input file
		String obj = ""; // the object code corresponding to the current line
		int i = 1; // the index for the location table, indicates the current line of the program
		int PC = 0; // program counter contents
		int cur = 0; // the location of the current line
		int B = -1; // the base register
		int start = -1; // the first instruction of the line in the object file
		boolean rsube = false; // checks for the special case when a +jsub is encountered
		int absStart = 0; // the first executable instruction of the program
		int format; // the format of the current line in bytes
		int curLength = 0; // length in bytes of the current object code
		int lineLength = 0; // the length of the current line to be printed to the object file
		String objLine = ""; // the next line of object codes to be printed to the object file
		PrintFirstObj();
		Lwriter.printf("%-8s%-10s%-26s%s\n", "Line", "Loc", "Source statement", "Object code");
		if (line.contains("START")) {
			Lwriter.println(String.format("%-8d", 1) + String.format("%-10.5s", String.format("%05X", Locations.get(0))) + String.format("%s", line.trim()));
		}
		while ((line = br.readLine()) != null) {
			cur = Locations.get(i++);
			if (i == Locations.size()) {
				break;
			}
			if (line.contains("+JSUB")) {
				rsube = true; // special case for rsub
			}
			PC = Locations.get(i);
			curLength = 0;
			format = getFormat2(line);
			if (start == -1 && format >= 2 && format <= 4) {
				start = cur; // set absolute start to this start.
				absStart = start;
			}
			if (format == -3) { // END
				break;
			} else if (format == -2) { // rsub exception
				if (rsube) {
					obj = "4F0000";
				} else {
					obj = "4C0000";
				}
				curLength = 3;
			} else if (format == -1) {
				PrintSpecialListing(i, line); // comment line 
				continue;
			} else if (format == 2) {
				obj = format2(line);
				curLength = format;
			} else if (format == 3 || format == 4) {
				obj = format34(line, PC, B); 
				curLength = format;
			} else if (format == 5) {
				B = -1; // turn off base relative
				obj = "";
			} else if (format >= 6) {
				B = format - 6; // set b to the location specified
				obj = "";
			} else if (format == 1) {
				obj = formatDir(line); // for byte or word
				if (line.contains("C'")) {
					curLength = obj.length()/2; //obj is the value of the characters, each
					//being 2 numbers, so divide by two to get the number of bytes
				} else if (line.contains("WORD")) {
					curLength = 3;
				} else { //must be a X'..'
					curLength = 1;
				}
			} else {
				obj = "";
			}
			
			if (!obj.equals("")) { 
				if ((objLine.length() + obj.length()) <= 60) { // max column length
					objLine += obj;
				} else {
					PrintObj(objLine, lineLength, start);
					lineLength = 0; 
					objLine = obj;
					start = cur;
				}
			} 
			lineLength += curLength;
			PrintListing(i, cur, line, obj);
		} 
		if (!objLine.equals("")) { // if there is more object code to print, print it
			PrintObj(objLine, lineLength, start);
		}
		Owriter.println("E" + String.format("%06X", absStart));
		Lwriter.println(String.format("%-18d", i) + String.format("%s", line.trim()));
		Lwriter.println("Program Length = " + String.format("%X", ProgramLength));
	}

	static int getFormat1(String Line, int cur) throws Exception { // get format for pass 1
		String[] k = Line.trim().replace("\t", " ").replace("+", "").replace("@","").replace("#","").replace(",", " ").split("[ ]+");
		int format = 0;
		if (k[0].equals("END") || k[0].charAt(0) == '.' || k[0].equals("BASE") || k[0].equals("NOBASE")) {
			return 0;
		} else if (k[0].equals("RSUB")) {
			return 3;
		} else if (k[1].equals("BYTE")) {
			Symbols.put(new String(k[0]), new Integer(cur));
			if (k[2].contains("C'")) {
				return k[2].substring(k[2].indexOf('\'')+1, k[2].lastIndexOf('\'')).length();
			} else if (k[2].contains("X'")){
				return 1;
			} else {
				throw new Exception("Invalid Line: " + Line);
			}
		} else if (k[1].equals("WORD")) {
			Symbols.put(new String(k[0]), new Integer(cur));
			return 3;
		} else if (k[1].equals("RESB")) {
			Symbols.put(new String(k[0]), new Integer(cur));
			return Integer.valueOf(k[2]);
		} else if (k[1].equals("RESW")) {
			Symbols.put(new String(k[0]), new Integer(cur));
			return 3*Integer.valueOf(k[2]);
		} else {
			format = getFormat(k, Line);
			if (format == -2) {
				throw new Exception("Invalid Line: " + Line);
			} else if (format == -1) {
				if (Symbols.containsKey(k[0])) {
					throw new Exception("Duplicate key: " + k[0]);
				}
				Symbols.put(new String(k[0]), new Integer(cur));
				format = getFormat(k, Line);
				if (format <= -1) {
					throw new Exception("Invalid line: " + Line);
				}
			}
			return format;
		}
	}

	static int getFormat2(String Line) { // simple format getter for pass 2.
		String[] k = Line.trim().replace("\t", " ").replace("+", "").replace("@","").replace("#","").replace(",", " ").split("[ ]+");
		if (k[0].equals("END")) {
			return -3; // fix
		} else if (k[0].charAt(0) == '.') {
			return -1;
		} else if (k[0].equals("BASE")) {
			return Symbols.get(k[1])+6; // set base + 6 to distinguish between other formats in case loc of B is < 5
		} else if (k[0].equals("NOBASE")) {
			return 5; // turn off base
		} else if (k[0].equals("RSUB")) {
			return -2;
		} else if (k[1].equals("BYTE")) { 
			return 1;
		} else if (k[1].equals("WORD")) {
			return 1;
		} else if (k[1].equals("RESB")) {
			return 0;
		} else if (k[1].equals("RESW")) {
			return 0;
		} else { 
			return getFormat(k, Line);
		}
	}

	// gets the format for instructions
	static int getFormat(String[] k, String Line) { 
		OP temp = null;
		int i = 0; // index of the op code
		int ld = 0; // special case for LD
		if (Symbols.containsKey(k[0])) {
			i = 1; // instruction isn't the first word so must be the next
		}
		ld = i;
		temp = OPtable.get(k[i]);
		if (temp == null) { // must be a LD or ST
			temp = OPtable.get(k[i] + " " + k[i+1]); // LD instruction
			if (temp == null && k[i].equals("ST")) { // ST instruction
				if (k.length > 3+i) { // if true then is a ST with indexed addressing
					temp = OPtable.get(k[i] + " " + k[i+3]);
				} else { // otherwise just a normal ST instruction
					temp = OPtable.get(k[i] + " " + k[i+2]);
				}
			} else if (temp != null && !k[i].equals("LD")) {
				return -2; // invalid instruction, indicate an error
			} else {
				ld = i+1; // is an LD so the operand is 2 words from the instruction
			}
			if (temp == null) { 
				return -1; // not a valid instruction, however, may just be off 
				//by a symbol so indicate this special case.
			}
		}
		if (temp.Length == 2 && (Line.contains("+") || Line.contains("#") || Line.contains("@"))) {
			return -2; // format 2 instructions can not have these characters
		}

		if ((Line.contains("#") && !Line.contains("#" + k[ld+1])) || (Line.contains("@") && !Line.contains("@" + k[ld+1]))) {
			return -2; // chech that these characters come before the operand
		} 
		if (Line.contains("+")) {
			if (!Line.contains("+" + k[i])) {
				return -2; // make sure the plus comes right before the instruction name
			} else {
				return 4; // must be extended mode, so return 4
			}
		}
		return temp.Length; // return the length of instruction
	}

	// get the object code for a directive, namely, for bytes
	static String formatDir(String Line) {
		String[] k = Line.trim().replace("+", "").replace("#", "").replace("@", "").replace(",", " ").split("[ ]+");
		String temp = new String();
		String obj = new String();
		if (k[2].contains("C'")) {
			temp = k[2].substring(k[2].indexOf('\'')+1, k[2].lastIndexOf('\''));
			for (int i = 0; i < temp.length(); i++) {
				obj += String.format("%02X", Integer.valueOf(temp.charAt(i)));
			}
		} else if (Line.contains("WORD")) { // the obj is just the value
			String t = String.format("%06X", Integer.valueOf(k[2]));
			obj = t.substring(t.length()-6);
		} else { //X'...' so obj is just the byte between the apostrophes
			obj = k[2].substring(k[2].indexOf('\'')+1, k[2].lastIndexOf('\''));
		}
		return obj;
	}
	
	// get the object code for a format 2 instruction
	static String format2(String Line) { 
		int code = 0;
		int r1 = 0;
		int r2 = 0;
		int i = 0;
		String obj = null;
		String[] k = Line.trim().replace("\t", " ").replace(",", " ").split("[ ]+");
		if (Symbols.containsKey(k[0])) {
			i = 1;
		}
		code = OPtable.get(k[i]).Code;
		if (code == 0xB4 || code == 0xB8) { // CLEAR or TIXR
			obj = String.format("%02X", code) + getReg(k[i+1]) + "0";
		} else {
			obj = String.format("%02X", code) + getReg(k[i+1]) + getReg(k[i+2]);
		}
		return obj;
	}
	// gets the number of the register in r
	static String getReg(String r) {
		if (r.equals("AX")) {
			return "0";
		} else if (r.equals("XX")) {
			return "1";
		} else if (r.equals("LX")) {
			return "2";
		} else if (r.equals("BX")) {
			return "3";
		} else if (r.equals("SX")) {
			return "4";
		} else if (r.equals("TX")) {
			return "5";
		} else {
			return null;
		}
	}
	// gets the object code of a format 3 or 4 instruction
	static String format34(String Line, int current, int base) throws Exception {
		String[] k = Line.trim().replace("\t", " ").replace("+", "").replace("#", "").replace("@", "").
			replace(",", " ").split("[ ]+"); // splits the line by words
		int xbpe = 0; // keeps track of the xbpe bits of the current line
		int ni = 0x3; // keeps track of the ni bits of the current line
		String obj = null; // the object code for the current line
		int i = 0; // the index of the instruction
		int X = 0; // for index addressing
		OP temp = null;
		Integer tempi; // used to check if the operand is a symbol or a constant
		int code = 0; // the op code of the current instruction
		int loc = 0; // holds the displacement or the address to be used in the object code

		if (Symbols.containsKey(k[0])) {
			i = 1; // check if the first word is a symbol
		}
		X = i;
		temp = OPtable.get(k[i]);
		if (temp == null) {
			temp = OPtable.get(k[i] + " " + k[i+1]); // LD instruction
			if (temp == null) { 
				if (k.length > 3+i) { // ST with index addressing
					temp = OPtable.get(k[i] + " " + k[i+3]);
				} else { // normal ST instruction
					temp = OPtable.get(k[i] + " " + k[i+2]);
					X = k.length; // ensure X + 2 > k.length to show not indexed addressing
				}
			} else {
				i++; // move i so that i+1 will be the operand for the instruction
				X++; // increment X to be consistent with i
			}
		}
		// can assume temp != null at this point because it would otherwise have caused an error in pass 1
		code = temp.Code;
		if (Line.contains("+")) { // extended addressing
			xbpe += 0x1; // set e
		}
		if (Line.contains("#")) { // immediate mode
			ni = 0x1;
			if (!Symbols.containsKey(k[i+1])) {
				loc = Integer.valueOf(k[i+1]); //#CONSTANT
			} else {
				loc = Integer.valueOf(Symbols.get(k[i+1])); //#SYMBOL
			}
		} else if (Line.contains("@")) { // indirect mode
			ni = 0x2;
		} 
		if ((X+2) < k.length) { // index addressing
			if (k[X+2].equals("XX")) { // in case of: ST BUFFER, XX, AX
				xbpe += 0x8; // index addressing on so X is set
			} else {
				throw new Exception("Invlalid line: " + Line); // invalid line
			}
		} 
		code += ni;
		if ((xbpe & 1) != 0) { // e is set
			if (ni != 1) {
				loc = Symbols.get(k[i+1]); // not immediate mode, so get the location of the operand
			}
			obj = String.format("%02X", code) + String.format("%X", xbpe) + String.format("%05X", loc);
		} else if (ni == 1 && !Symbols.containsKey(k[i+1])) { // immediate mode with a constant value
			if (loc >= 0 && loc <= 4095) {
				obj = String.format("%02X", code) + String.format("%X", xbpe) + String.format("%03X", loc);
			} else {
				throw new Exception("Constant value is too large: " + loc); // error
			}
		} else { 
			tempi = Symbols.get(k[i+1]); // check if operand is a symbol
			if (tempi == null) {
				tempi = Integer.valueOf(k[i+1]); //must be a constant so get its value
			} 
			loc = tempi;
			loc -= current; // PC relative
			if (loc <= 2047 && loc >= -2048) {
				xbpe += 0x2; // set p
				String t = String.format("%03X", loc);
				obj = String.format("%02X", code) + String.format("%X", xbpe) + t.substring(t.length()-3);
			} else if (base != -1) { // base relative is turned on
				base = Integer.valueOf(Symbols.get(k[i+1])) - base; // base relative
				if (base >= 0 && base <= 4095) { // check base is within the range
					xbpe += 0x4; // set b
					obj = String.format("%02X", code) + String.format("%X", xbpe) + String.format("%03X", base);//.substring(s.length-4,s.length-1);
				} else {
					throw new Exception("Location of symbol is out of range");
				}
			} else {
				throw new Exception("Location of symbol is out of range");
			}
		}

		return obj;
	}

	//prints the first object line
	static void PrintFirstObj() {
		Owriter.printf("H%-6.6s%.6s%.6s\n", FileName, String.format("%06X", Locations.get(0)), String.format("%06X", ProgramLength));
	}
	//prints each ordinary line of the object file
	static void PrintObj(String objLine, int length, int firstLoc) throws Exception {
		Owriter.printf("T%.6s%.2s%s\n", String.format("%06X", firstLoc), String.format("%02X", length), objLine);
	}
	//prints the line to the listing file 
	static void PrintListing(int lineNumber, int loc, String Line, String Obj) throws Exception {
		Lwriter.println(String.format("%-8d", lineNumber) + String.format("%-10.5s", String.format("%05X", loc)) + String.format("%-26s%s", Line.trim(), Obj));
		//Lwriter.printf("%-6.6s%-6.5s%-25.25s%s\n", Integer.toString(lineNumber), String.format("%05X", loc), Line.trim(), Obj);
	}
	// prints comment lines and special cases to the listing file
	static void PrintSpecialListing(int lineNumber, String Line) {
		Lwriter.println(String.format("%-18d", lineNumber) + Line);
	}
	
}
// used in the op table to incorparate code and length of instruction
class OP {
	int Length;
	int Code;
	
	public OP() {}
	
	public OP (int Code, int Length) {
		this.Code = Code;
		this.Length = Length;
	}
}