public class Test {
	public static void main(String[] args) throws Exception{
		// int code = 0x14;
		// int xbpe = 0x2;
		// int loc;
		// int current = 0x00;
		// int base = 0x800;
		// int SymLoc = 0x802;
		// String obj = null;
		// code += 3;
		// loc = SymLoc - current;
		// if (loc <= 2047 && loc >= -2048) {
		// 	String temp = String.format("%03X", loc);
		// 	obj = String.format("%02X", code) + Integer.toHexString(xbpe) + temp.substring(temp.length()-4);
		// } else if (base != -1) {
		// 	base = SymLoc - base;
		// 	if (base >= 0 && base <= 4095) {
		// 		xbpe = 0x4;
		// 		obj = String.format("%02X", code) + Integer.toHexString(xbpe) + String.format("%05X", base);;
		// 	}
		// } else {
		// 	System.out.print("TOO BIG ");
		// }
		// System.out.println(obj);

		// String s = new String("+LD\tBX,\t#STUFF");
		// String[] k = s.trim().replace("\t", " ").replace("+", "").replace("@","").replace("#","").replace(",", " ").split("[ ]+");
		// for (int i = 0; i < k.length; i++) {
		// 	System.out.println(k[i]);
		// }

		// String temp = new String("STUFF BYTE C'AB'");
		// String obj = new String();
		// String[] k = temp.trim().replace("+", "").replace("#", "").replace("@", "").replace(",", " ").split("[ ]+");
		// obj += String.format("%02X", Integer.valueOf(k[2].charAt(2)));
		// obj += String.format("%02X", Integer.valueOf(k[2].charAt(3)));
		// System.out.println(obj);
		//Owriter.println("H" + String.format("%-6s", FileName) + String.format("%-6s", Integer.toHexString(Locations.get(0))) + String.format("%-6s", Integer.toHexString(ProgramLength)));
		//Owriter.printf("H%-6s%-6s%-6s\n", FileName, Integer.toHexString(Locations.get(0)), Integer.toHexString(ProgramLength));
		String FileName = "input1.txt";
		System.out.printf("%-7.6s hello\n", FileName);

	}
}

COPY START 0
FIRST ST RETADR, LX
	LD BX, #LENGTH
	BASE LENGTH
CLOOP +JSUB RDREC
	LD AX, LENGTH
	COMP 54
	JEQ ENDFIL
	+JSUB WRREC
	J CLOOP
ENDFIL LD AX, EOF
	ST BUFFER, AX
	LD AX, #3
	ST LENGTH, AX
	+JSUB WRREC
	J @RETADR
EOF BYTE C'EOF'
RETADR RESW 1
LENGTH RESW 1
BUFFER RESB 4096
.
. SUBROUTINE TO READ RECORD INTO BUFFER
.
RDREC CLEAR XX
	CLEAR AX
	CLEAR SX
	+LD TX, #4096
RLOOP TD INPUT
	JEQ RLOOP
	RD INPUT
	COMPR AX, SX
	JEQ EXIT
	STCH BUFFER, XX
	TIXR TX
	JLT RLOOP
EXIT ST LENGTH, XX
	RSUB
INPUT BYTE X'F1'
.
.
.
WRREC CLEAR XX
	LD TX, LENGTH
WLOOP TD OUTPUT
	JEQ WLOOP
	LDCH BUFFER, XX
	WD OUTPUT
	TIXR TX
	JLT WLOOP
	RSUB
OUTPUT BYTE X'05'
	END FIRST

COPY START 0
FIRST LD AX, #1
	ST ARRAY, XX
	+LD TX, #4096
	LD XX, #1
LOOP LD TX, #3
	MULR XX, TX
	ADDR TX, SX
	LD AX, #3
	SUBR AX, SX
	ADDR XX, AX
	ST XBKP, XX
CHR ADDR SX, XX
	ST #ARRAY, XX, BX
	MUL ARRAY, XX
	ADDR XX, TX
	LD BX, #XBKP
	ST ARRAY, XX, AX
	CLEAR TX
	+LD BX, #ARRAY, XX
	LD XX, XBKP
	.
	.
	.
	STCH ARRAY, XX
	LD BX, ARRAY, XX
	ST ARRAY, XX
	ST ARRAY, XX, AX
	COMP ARRAY, XX
	TIX #11
	JLT LOOP
XBKP RESW 1
ARRAY RESW 2048
BTH ADD #6
	+LD BX, #CHR
	BASE CHR
	ADD XBKP
	LD XX, XBKP
	ST XBKP, AX
END FIRST