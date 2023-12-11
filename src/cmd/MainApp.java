package cmd;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

public class MainApp 
{
	public static boolean checkGSC(RandomAccessFile gsc) throws IOException
	{
		boolean gscError = false;
		int gscf = gsc.readInt(); //GSCF (Game Scenario Contents of File)
		if (gscf != 0x47534346) gscError = true; 
		
		gsc.seek(8);
		short gscSize = getLittleEndianShort(gsc.readShort());
		if (gscSize+32 != gsc.length()) gscError = true;
		
		gsc.seek(16); 
		int gshd = gsc.readInt(); //GSHD (GSC version indicator)
		if (gshd != 0x47534844) gscError = true; 
		gsc.seek(32);
		
		/* gscVer values:
		 * v3.1 (0x0300000001000000) -> Budokai Tenkaichi 2
		 * v3.2 (0x0300000002000000) -> Budokai Tenkaichi 3
		 * v3.3 (0x0300000002000000) -> Raging Blast 1 */ 
		long gscVer = gsc.readLong();
		if (gscVer != 0x0300000002000000L) gscError = true; 
		
		gsc.seek(64);
		int gscd = gsc.readInt(); //GSCD (contains total file size of all scenes)
		if (gscd != 0x47534344) gscError = true;

		gsc.seek(72);
		short gsacTotalSize = getLittleEndianShort(gsc.readShort());
		gsc.seek(gsacTotalSize+96); //112 is the actual size of the header, but I put 96 bc of the next check
		
		if (gsc.readInt() != 0x47534454) gscError = true; //0x47534454 = GSDT (Game Scenario DaTa)
		return gscError;
	}
	public static float getFloatFromOffset(RandomAccessFile gsc, short offset, int gsdtStart) throws IOException
	{
		gsc.seek((offset*4)+gsdtStart);
		return getLittleEndianFloat(gsc.readFloat());
	}
	public static float getLittleEndianFloat(float data)
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.asFloatBuffer().put(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getFloat();
	}
	public static int getIntFromOffset(RandomAccessFile gsc, short offset, int gsdtStart) throws IOException
	{
		gsc.seek((offset*4)+gsdtStart);
		return getLittleEndianInt(gsc.readInt());
	}
	public static int getLittleEndianInt(int data)
	{
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.asIntBuffer().put(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getInt();
	}
	public static short getLittleEndianShort(short data)
	{
		ByteBuffer bb = ByteBuffer.allocate(2);
		bb.asShortBuffer().put(data);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		return bb.getShort();
	}
	public static short getStartOfGSDT(RandomAccessFile gsc) throws IOException
	{
		int curr=0, pos=0;
		while (curr != 0x47534454)
		{
			curr = gsc.readInt();
			pos+=4; gsc.seek(pos);
		}
		
		gsc.seek(pos+12);
		return (short) gsc.getFilePointer();
	}
	public static String getStringFromAnyID(RandomAccessFile txt, int ID) throws IOException
	{		
		int num=0; String currLine, name=null; Scanner sc = null;
		txt.seek(0);
		while (txt.getFilePointer() != txt.length())
		{
			currLine = txt.readLine();
			sc = new Scanner(currLine);
			sc.useDelimiter(";");
			
			while (sc.hasNext())
			{
				num = sc.nextInt();
				name = sc.nextLine();
				name = name.replace(";", "");
			}
			
			if (num == ID)
			{
				sc.close();
				return name;
			}
		}
		return null;
	}
	public static String getStringFromCondOrEventID(RandomAccessFile txt, int ID) throws IOException
	{
		byte[] bytes = ByteBuffer.allocate(4).putInt(ID).array();
		
		if (bytes[2] == -128)
		{
			bytes[2]=0;
			ID = ByteBuffer.wrap(bytes).getInt();
			return getStringFromAnyID(txt, ID) + " (Opponent)";
		}
		return getStringFromAnyID(txt, ID);
	} 
	public static String getCharInfo(int[][] teams, int charIndex, RandomAccessFile charTxt, RandomAccessFile itemsTxt) throws IOException
	{
		String output; boolean isDmg;
		if (teams[charIndex][2] == 0) isDmg = false; 
		else isDmg = true;
		output = "Character: " + getStringFromAnyID(charTxt, teams[charIndex][0]) + "\n";
		output = output + "Costume: " + (teams[charIndex][1]+1) + "\n";
		output = output + "Damaged: " + isDmg + "\n";
		output = output + "COM Difficulty Level: " + teams[charIndex][3] + "\n";
		output = output + "Strategy Z-Item: " + getStringFromAnyID(itemsTxt, teams[charIndex][4]) + "\n";
		output = output + "Initial Health: " + teams[charIndex][5] + "%\n";
		
		for (int i=6; i<=12; i++)
			output = output + "Z-Item #" + (i-5) + ": " + getStringFromAnyID(itemsTxt, teams[charIndex][i]) + "\n";
		return output;
	}
	public static void main(String[] args) throws IOException 
	{
		RandomAccessFile gsc = new RandomAccessFile("GSC", "r");
		RandomAccessFile charTxt = new RandomAccessFile("txt/characters.txt","r");
		RandomAccessFile condTxt = new RandomAccessFile("txt/conditions.txt","r");
		RandomAccessFile eventTxt = new RandomAccessFile("txt/events.txt","r");
		RandomAccessFile itemsTxt = new RandomAccessFile("txt/items.txt","r");

		if (checkGSC(gsc) == true) System.exit(1);
		short gsdtStart = getStartOfGSDT(gsc);
		
		int curr=0, currID=10000, pos=0, gsacType=0, param;
		short offset;
		RandomAccessFile bgmTxt = new RandomAccessFile("txt/bgm.txt","r");
		RandomAccessFile mapTxt = new RandomAccessFile("txt/maps.txt","r");
		int mapID, bgmID, charIndex=0, teamIndex=1;
		int[] teamCnt = new int[2];
		int[][] teams = new int[10][13];
		/* byte offsetType; unused for those who want the program to check for floats as well */
				
		while (curr!=0x01000300) //traverse until start of scene 0 is reached
		{
			curr = gsc.readInt();
			if (curr == 0x01050A00)
			{
				System.out.println("[Battle Settings]");
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				mapID = getIntFromOffset(gsc, offset, gsdtStart);
				pos+=4; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				bgmID = getIntFromOffset(gsc, offset, gsdtStart);
				System.out.println("Map: " + getStringFromAnyID(mapTxt,mapID));
				System.out.println("BGM: " + getStringFromAnyID(bgmTxt,bgmID));
			}
			if (curr == 0x01020B00)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc, offset, gsdtStart);
				pos+=4; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				teamCnt[teamIndex-1] = getIntFromOffset(gsc, offset, gsdtStart);
				System.out.println("Player " + teamIndex + " Teammates: " + teamCnt[teamIndex-1]);
				if (teamIndex==2) 
				{
					System.out.println(); teamIndex = 0;
				}
				teamIndex++;
			}
			if (curr == 0x010E0C00)
			{
				if (charIndex%teamCnt[teamIndex%2]==0) 
				{
					System.out.println("[Player " + teamIndex + "'s Team]");
					teamIndex++;
				}
				pos+=9; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				teams[charIndex][0] = getIntFromOffset(gsc, offset, gsdtStart);
				for (int i=1; i<13; i++)
				{
					pos+=4; gsc.seek(pos);
					offset = getLittleEndianShort(gsc.readShort());
					teams[charIndex][i] = getIntFromOffset(gsc, offset, gsdtStart);
				}
				System.out.println("> Teammate " + ((charIndex%teamCnt[teamIndex%2])+1));
				System.out.println(getCharInfo(teams, charIndex, charTxt, itemsTxt));
				charIndex++;
			}
			pos++; gsc.seek(pos);
		}
		
		System.out.println("[Scene " + (currID-10000) + "]");
		while (curr!=0x47534454) //traverse until GSDT is reached
		{
			curr = gsc.readInt();
			currID = getLittleEndianInt(curr);
			if (currID>=10000 && currID<10030) //display scene ID as long as it's not a victory/loss sequence
				System.out.println("\n[Scene " + (currID-10000) + "]");
			if (curr == 0x01010900)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				gsacType = getIntFromOffset(gsc,offset,gsdtStart);
			}
			if (curr == 0x08610200)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				int condID = getIntFromOffset(gsc,offset,gsdtStart);
				pos+=4; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				int gsacID = getIntFromOffset(gsc,offset,gsdtStart);
				System.out.println("Condition: " + getStringFromCondOrEventID(condTxt, condID) + "\n> GSAC ID: " + (gsacID-10000));
			}
			if (curr == 0x01000D00)
			{
				gsc.seek(pos+4);
				if (gsc.readInt() != 0x01000E00)
					System.out.println("[Changes to Player 1 detected.]");
			}
			if (curr == 0x01000E00)
			{
				gsc.seek(pos+4);
				if (gsc.readInt() != 0x01020800)
					System.out.println("[Changes to Opponent detected.]");
			}
			if (curr == 0x08420100)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart); //this param is likely a boolean
				if (param == 1) System.out.println("> Gain full Ki Bars");
			}
			if (curr == 0x08430100)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart); //this param is likely a multiple of 5000 or less
				System.out.println("> Gain " + (param+1)*5000 + " HP (?)");
			}
			if (curr == 0x08460100)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart);
				System.out.println("> Set current Ki Amount to " + param + "%");
			}
			//0x08480100 and 0x08680100 work the same, although: the latter accepts 0 as a value while the former doesn't
			if (curr == 0x08480100 || curr == 0x08680100)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart);
				System.out.println("> Set current Health to " + param + "%");
			}
			if (curr >= 0x08300100 && curr <= 0x08370100)
			{
				pos++; gsc.seek(pos);
				int itemIndex = gsc.readByte()%48;
				pos+=4; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				int itemID = getIntFromOffset(gsc,offset,gsdtStart);
				System.out.println("> Z-Item #" + (itemIndex+1) + ": " + getStringFromAnyID(itemsTxt,itemID));
			}
			if (curr == 0x01020800 && gsacType == -1)
			{
				pos+=5; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventID = getIntFromOffset(gsc,offset,gsdtStart);
				pos+=4; gsc.seek(pos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventCharID = getIntFromOffset(gsc,offset,gsdtStart);
				System.out.println("Event:     " + getStringFromCondOrEventID(eventTxt, eventID));
				if (eventCharID != -1) //only print out char ID if the event actually uses it
					System.out.println("Character: " + getStringFromAnyID(charTxt, eventCharID));
			}
			pos++; gsc.seek(pos);
		}
	}
}