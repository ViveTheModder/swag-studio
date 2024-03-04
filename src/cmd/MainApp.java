package cmd;
//Swag Studio v1.4 by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class MainApp 
{
	private static int sharedPos=0;
	private static int[][] teams = new int[10][13];
	static final int INFO = JOptionPane.INFORMATION_MESSAGE;
	static final String CSV_PATH = "./csv/";
	static final String GSC_PATH = "./gsc/";
	static final String OUT_PATH = "./out/";
	static final String WINDOW_TITLE = "Swag Studio";
	
	public static boolean isFaultyGSC(RandomAccessFile gsc) throws IOException
	{
		boolean gscError = false;
		gsc.seek(0); //added this just to make sure it always starts from 0
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
	public static float getFloatFromOffset(RandomAccessFile gsc, short offset, short gsdtStart) throws IOException
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
	public static int getIntFromOffset(RandomAccessFile gsc, short offset, short gsdtStart) throws IOException
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
	public static String getSceneType(RandomAccessFile gsc, int gsacID, short gsdtStart, int initGSACpos) throws IOException
	{
		String output = "";
		int pos=gsdtStart, currID=0;
		short offset=0, currOffset=0;
		gsc.seek(pos);
		
		if (gsacID==10049)
			output+=" (Placeholder Defeat)";
		else if (gsacID>=10040 && gsacID<=10048)
			output+=" (Defeat)";
		else if (gsacID==10039)
			output+=" (Placeholder Victory)";
		else if (gsacID>=10030)
			output+=" (Victory)";
		
		while (pos!=gsc.length())
		{
			currID = getLittleEndianInt(gsc.readInt());
			if (currID == gsacID)
			{
				offset = (short) ((pos-gsdtStart)/4);
				break;
			}
			pos+=4; gsc.seek(pos);
		}
		
		pos=initGSACpos; gsc.seek(pos);
		while (currOffset!=21319) //21319 = GS (1st half of GSAC)
		{
			currOffset = getLittleEndianShort(gsc.readShort());
			if (currOffset == offset)
			{
				output+=" - UNUSED"; break;
			}
			pos+=2; gsc.seek(pos);
		}
		
		return output;
	}
	public static String getStringFromAnyID(RandomAccessFile csv, int ID) throws IOException
	{
		int num=0; String currLine, name=null; Scanner sc = null;
		if (ID == 65535) return "Immediate"; //65535 is only used for condition IDs, whereas 0xFFFFFFFF is used for null Z-Items
		if (ID == -1) return name;
		
		csv.seek(0);
		while (csv.getFilePointer() != csv.length())
		{
			currLine = csv.readLine();
			sc = new Scanner(currLine);
			sc.useDelimiter(",");
			
			while (sc.hasNext())
			{
				num = sc.nextInt();
				name = sc.nextLine();
				name = name.replace(",", "");
			}
			
			if (num == ID)
			{
				sc.close(); return name;
			}
		}
		return "Unknown (ID: " + ID + ")";
	}
	public static String getStringFromCondOrEventID(RandomAccessFile csv, int ID, boolean isCond) throws IOException
	{
		byte[] bytes = ByteBuffer.allocate(4).putInt(ID).array();
		String output = "";
		
		if (bytes[2] == -128) //check if ID belongs to opponent condition/event
		{
			bytes[2]=0; //treat it like a normal condition/event
			ID = ByteBuffer.wrap(bytes).getInt();
			output += " (Opponent)";
			
			if (isCond == false) //check for event ID
				if (ID>=16 && ID<30)  output += " [AUTO]";
			if (isCond == true)
				if (ID>=22 && ID<29) output += " [AUTO]";
		}
		output = getStringFromAnyID(csv, ID) + output; //properly initialize output
		
		if (isCond == true) //check for condition ID
			if (ID == 34) output += " [AUTO]";

		return output;
	} 
	public static String getCharInfo(int[][] teams, int charIndex, RandomAccessFile charCsv, RandomAccessFile itemsCsv) throws IOException
	{
		String output; boolean isDmg;
		if (teams[charIndex][2] == 0) isDmg = false;
		else isDmg = true;
		
		output = "Character: " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
		output += "Costume: " + (teams[charIndex][1]+1) + "\n";
		output += "Damaged: " + isDmg + "\n";
		output += "COM Difficulty Level: " + teams[charIndex][3] + "\n";
		output += "Strategy Z-Item: " + getStringFromAnyID(itemsCsv, teams[charIndex][4]) + "\n";
		output += "Initial Health: " + teams[charIndex][5] + "%\n";
		
		for (int i=6; i<=12; i++)
			output += "Z-Item #" + (i-5) + ": " + getStringFromAnyID(itemsCsv, teams[charIndex][i]) + "\n";
		return output;
	}
	public static String showCommonInfo(RandomAccessFile gsc, RandomAccessFile bgmCsv, RandomAccessFile charCsv, RandomAccessFile itemsCsv, short gsdtStart) throws IOException
	{
		RandomAccessFile mapCsv = new RandomAccessFile(CSV_PATH+"maps.csv","r");
		RandomAccessFile namesCsv = new RandomAccessFile(CSV_PATH+"names.csv","r");
		RandomAccessFile sagasCsv = new RandomAccessFile(CSV_PATH+"sagas.csv","r");
		RandomAccessFile rewardCsv; //just a reference to the previous CSVs

		int bgmID, charIndex=0, curr=0, teamIndex=0, mapID;
		short offset;
		String output="", saga="";
		String[] rewardNames = {"Z-Points (from Easy to Hard)","Z-Items","Maps","Characters","Scenarios"};
		int[] rewards = new int[15];
		int[] teamCnt = new int[2];
		
		sharedPos=0;
		while (curr!=0x01000300) //traverse until start of scene 0 is reached
		{
			curr = gsc.readInt();
			if (curr == 0x01010200)
			{
				output += "[General Information]\n";
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int scenarioID = getIntFromOffset(gsc, offset, gsdtStart);
				if (scenarioID<48)
				{
					output += "Name: " + getStringFromAnyID(namesCsv,scenarioID) + "\n";
					if (scenarioID>=44) saga = getStringFromAnyID(sagasCsv,7);
					else if (scenarioID>=40) saga = getStringFromAnyID(sagasCsv,6);
					else if (scenarioID>=35) saga = getStringFromAnyID(sagasCsv,5);
					else if (scenarioID>=19) saga = getStringFromAnyID(sagasCsv,4);
					else if (scenarioID>=14) saga = getStringFromAnyID(sagasCsv,3);
					else if (scenarioID>=7) saga = getStringFromAnyID(sagasCsv,2);
					else if (scenarioID>=3) saga = getStringFromAnyID(sagasCsv,1);
					else saga = getStringFromAnyID(sagasCsv,0);
					output += "Saga: " + saga + "\n\n";
				}
			}
			if (curr == 0x010F1000)
			{
				output += "[Rewards]\n";
				sharedPos+=5; gsc.seek(sharedPos);
				for (int i=0; i<15; i++)
				{
					if (i>=12) rewardCsv = namesCsv;
					else if (i>=9) rewardCsv = charCsv;
					else if (i>=6) rewardCsv = mapCsv;
					else rewardCsv = itemsCsv;
					
					if (i%3==0) output += "> Acquired " + rewardNames[(int)i/3] + "\n";
					offset = getLittleEndianShort(gsc.readShort());
					rewards[i] = getIntFromOffset(gsc, offset, gsdtStart);
					sharedPos+=4; gsc.seek(sharedPos);
					if (i>=3) output += getStringFromAnyID(rewardCsv,rewards[i]) + "\n";
					else output += rewards[i] + "\n";
				}
			}
			if (curr == 0x01050A00)
			{
				output += "\n[Battle Settings]\n";
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				mapID = getIntFromOffset(gsc, offset, gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				bgmID = getIntFromOffset(gsc, offset, gsdtStart);
				output += "Map: " + getStringFromAnyID(mapCsv,mapID) + "\n";
				output += "BGM: " + getStringFromAnyID(bgmCsv,bgmID) + "\n\n";
			}
			if (curr == 0x010E0C00)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				charIndex = getIntFromOffset(gsc, offset, gsdtStart);
				
				if (charIndex >= 32768)
				{
					charIndex%=32768; charIndex+=5; teamIndex=1;
					if (charIndex==5) output += "[Opponent Team]\n";
				}
				if (charIndex==0) output += "[Player 1 Team]\n";
				output += "> Teammate " + ((charIndex%5)+1) + "\n";
				
				teamCnt[teamIndex]++;
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				teams[charIndex][0] = getIntFromOffset(gsc, offset, gsdtStart);
				for (int i=1; i<13; i++)
				{
					sharedPos+=4; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					teams[charIndex][i] = getIntFromOffset(gsc, offset, gsdtStart);
				}
				output += getCharInfo(teams, charIndex, charCsv, itemsCsv) + "\n";
			}
			sharedPos++; gsc.seek(sharedPos);
		}
		output += "> Results\nTeammate Count (Player 1): " + teamCnt[0] + "\n";
		output += "Teammate Count (Opponent): " + teamCnt[1] + "\n\n";
		return output;
	}
	public static String showSceneInfo(RandomAccessFile gsc, RandomAccessFile bgmCsv, RandomAccessFile charCsv, RandomAccessFile itemsCsv, short gsdtStart, int option) throws IOException
	{
		RandomAccessFile condCsv = new RandomAccessFile(CSV_PATH+"conditions.csv","r");
		RandomAccessFile eventCsv = new RandomAccessFile(CSV_PATH+"events.csv","r");
		int bgmID=0, charIndex, curr=0, currID=10000, gsacType=0, inputInt, initGSACpos=sharedPos-1;
		short offset; float inputFloat; String output="";
		
		gsc.seek(initGSACpos);
		output += "[Scene " + (currID-10000) + "]\n";
		while (curr!=0x47534454) //traverse until GSDT is reached
		{
			curr = gsc.readInt();
			currID = getLittleEndianInt(curr);
							
			if (currID>10000 && currID<10050)
				output += "\n[Scene " + (currID-10000) + getSceneType(gsc, currID, gsdtStart, initGSACpos) + "]\n";
			/* start of GSC functions 08-14 */
			if (curr == 0x01020800 && gsacType == -1)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventID = getIntFromOffset(gsc,offset,gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventCharID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "Event:     " + getStringFromCondOrEventID(eventCsv, eventID, false) + "\n";
				if (eventCharID != -1) //only print out char ID if the event actually uses it
					output += "Character: " + getStringFromAnyID(charCsv, eventCharID) + "\n";
			}
			if (curr == 0x01000700)
			{
				gsc.seek(sharedPos+4);
				if (gsc.readInt() != 0x01000D00) output += "(Battle Info)\n";
				else output += "(No Battle Info)\n";
			}
			if (curr == 0x01010900)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				gsacType = getIntFromOffset(gsc,offset,gsdtStart);
			}
			if (curr == 0x01000D00)
			{
				gsc.seek(sharedPos+4);
				if (gsc.readInt() != 0x01000E00) output += "[Changes to Player 1 detected.]\n";
			}
			if (curr == 0x01000E00)
			{
				gsc.seek(sharedPos+4);
				if (gsc.readInt() != 0x01020800) output += "[Changes to Opponent detected.]\n";
			}
			/* start of BGM functions */
			if (curr == 0x0101DD05)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				bgmID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "BG Music:  " + getStringFromAnyID(bgmCsv,bgmID) + "\n";
			}
			if (curr == 0x0100DE05)
			{
				gsc.seek(sharedPos+4);
				output += "Lower Current BG Music\n";
			}
			/* start of GSC properties from `0` to `a` type */
			if (curr >= 0x08300100 && curr <= 0x08370100)
			{
				sharedPos++; gsc.seek(sharedPos);
				int itemIndex = gsc.readByte()%48;
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int itemID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Z-Item #" + (itemIndex+1) + ": " + getStringFromAnyID(itemsCsv,itemID) + "\n";
			}
			if (curr == 0x08430100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Apply changes to Teammate " + (inputInt+1) + "\n";
			}
			if (curr == 0x08460100 || curr == 0x08420100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Add " + inputInt + "% more Ki\n";
			}
			if (curr == 0x08480100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Add " + inputInt + "% more Health\n";
			}
			if (curr == 0x08490100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Set current Ki Amount to " + inputInt + "%\n";
			}
			if (curr == 0x08620100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				if (inputInt == 1) output += "> Set current Blast Stock amount to " + inputInt + "\n";
			}
			if (curr == 0x08680100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Set current Health to " + inputInt + "%\n";
			}
			if (curr == 0x086C0100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				inputInt = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Set COM Difficulty Level to " + inputInt + "\n";
			}		
			if (curr == 0x08610200)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int condID = getIntFromOffset(gsc,offset,gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int gsacID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "Condition: " + getStringFromCondOrEventID(condCsv, condID, true) + "\n> GSAC ID: " + (gsacID-10000) + "\n";
			}
			/* cinematic option - I really wanted to split this method in 2, but I kept making things worse */
			if (option==0)
			{
				//start of GSC functions 01-06
				if (curr == 0x01010100)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "Wait " + inputFloat + " seconds\n";
				}
				if (curr == 0x01000300) output += "(Cinematic Info)\n";
				if (curr == 0x01000600) output += "Disable ALL VFX\n";
				
				//start of GSC functions 801-810
				if (curr == 0x01072103)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Position/Rotation for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
					for (int i=1; i<7; i++)
					{
						if (i==1) output+="> Position (XYZ): ";
						if (i==4) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; gsc.seek(sharedPos);
						offset = getLittleEndianShort(gsc.readShort());
						inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01042203)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Position for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
					for (int i=1; i<4; i++)
					{
						output+="> Position (XYZ): ";
						sharedPos+=4; gsc.seek(sharedPos);
						offset = getLittleEndianShort(gsc.readShort());
						inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01042303)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Rotation for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
					for (int i=1; i<4; i++)
					{
						output+="\n> Rotation (XYZ): ";
						sharedPos+=4; gsc.seek(sharedPos);
						offset = getLittleEndianShort(gsc.readShort());
						inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01012403)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Make " + getStringFromAnyID(charCsv, teams[charIndex][0]) + " Visible\n";
				}
				if (curr == 0x01012503)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Make " + getStringFromAnyID(charCsv, teams[charIndex][0]) + " Invisible\n";
				}
				if (curr == 0x01012603)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable Aura Charge for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012703)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Disable Aura Charge for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012803)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable Ki Charge for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012903)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Disable Ki Charge for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012A03)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable MPM Explosion for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				//start of GSC function 901
				if (curr == 0x01028503)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					sharedPos+=4; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputInt = getIntFromOffset(gsc,offset,gsdtStart);
					
					output += "Play Animation " + inputInt + " for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
				}
				
				//start of GSC functions 1001-1003
				if (curr == 0x0101E903)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "Add " + inputFloat + " s fade-out\n";
				}
				if (curr == 0x0101EA03)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "Add " + inputFloat + " s fade-in\n";
				}	
				if (curr == 0x0101EB03) output += "Disable Current Fade\n";
				
				//start of GSC functions 1201-1205
				if (curr == 0x0106B104)
				{
					output += "[Initial Camera Point]\n";
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "> Position (XYZ): " + inputFloat + " ";
					
					for (int i=1; i<6; i++)
					{
						if (i==3) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; gsc.seek(sharedPos);
						offset = getLittleEndianShort(gsc.readShort());
						inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x0100B204)
				{
					output += "[Additional Camera Point]\n";
					sharedPos+=9; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "> Transition Time: " + inputFloat + " ";
					
					for (int i=1; i<7; i++)
					{
						if (i==1) output+="\n> Position (XYZ): ";
						if (i==4) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; gsc.seek(sharedPos);
						offset = getLittleEndianShort(gsc.readShort());
						inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x0101B404)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "Shake Camera for " + inputFloat + " seconds\n";
				}
				if (curr == 0x0101B504) output += "Disable Current Camera Shake";
				
				//start of GSC functions 1603
				if (curr == 0x01024306)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					charIndex = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					sharedPos+=4; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputInt = getIntFromOffset(gsc,offset,gsdtStart);
					
					if (charIndex!=32767)
						output += "Play Voice Line " + inputInt + " for " + getStringFromAnyID(charCsv, teams[charIndex][0]) + "\n";
					else output += "Play Voice Line " + inputInt + " for Background Character\n";
				}
				/* common properties */
				if (curr == 0x08570000) output += "> Change fade-out color to white\n";
				if (curr == 0x086C0000) output += "> Loop property detected\n";
				if (curr == 0x08770000) output += "> Wait property detected\n";
				if (curr == 0x087401000)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputFloat = getFloatFromOffset(gsc,offset,gsdtStart);
					output += "Speed Coefficient: " + inputFloat + "\n";
				}
				//background voice lines
				if (curr == 0x08760200)
				{
					sharedPos+=5; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					int condID = getIntFromOffset(gsc,offset,gsdtStart);
					sharedPos+=4; gsc.seek(sharedPos);
					offset = getLittleEndianShort(gsc.readShort());
					inputInt = getIntFromOffset(gsc,offset,gsdtStart);
					output += "Condition: " + getStringFromCondOrEventID(condCsv, condID, true) + "\n> Voice Line ID: " + inputInt + "\n";
				}
			}
			sharedPos++; gsc.seek(sharedPos);
		}
		return output;
	}
	
	public static void main(String[] args) throws IOException 
	{
		File folder = new File(GSC_PATH);
		//filter out validated gsc file paths from everything else in the current working directory (I'd use RAF[] if it extended InputStream & OutputStream)
		File[] gscPaths = folder.listFiles((dir, name) -> 
		(
			name.startsWith("GSC-B-") && (name.toLowerCase().endsWith(".gsc") || name.toLowerCase().endsWith(".unk"))
		)); //that's right, the tool detects UNK files too lmao
		
		int gscIndex=0, msgType=INFO, option=1; short gsdtStart;
		String output1, output2, timeString, msg="Would you like to include cinematic info for each log?";
		double start, finish, time1=0, time2=0, interval, total=0;
		
		RandomAccessFile[] gscFiles = new RandomAccessFile[gscPaths.length];
		RandomAccessFile bgmCsv = new RandomAccessFile(CSV_PATH+"bgm.csv","r");
		RandomAccessFile charCsv = new RandomAccessFile(CSV_PATH+"characters.csv","r");
		RandomAccessFile itemsCsv = new RandomAccessFile(CSV_PATH+"items.csv","r");

		if (gscPaths.length != 0)
		{
			for (File file: gscPaths) //initialize the RAF array with the gsc file paths
			{
				gscFiles[gscIndex] = new RandomAccessFile(file.getAbsolutePath(), "r");
				gscIndex++;
			}
			//check for command line arguments
			if (args.length == 0) JOptionPane.showConfirmDialog(null, msg, WINDOW_TITLE, 0);
			else
			{
				if (args[0]==("-c")) option=0; //skip confirm dialog if argument is given
				else JOptionPane.showConfirmDialog(null, msg, WINDOW_TITLE, 0);
			}
				
			msg = "Time required for each GSC:\n";
			
			for (int i=0; i<gscFiles.length; i++)
			{
				RandomAccessFile gsc = gscFiles[i];
				String fileName = gscPaths[i].getName();
				gsdtStart = getStartOfGSDT(gsc);
				
				//get file extension
				String fileExt = "";
				int dotIndex = fileName.lastIndexOf('.');
				if (dotIndex>=0) fileExt = fileName.substring(dotIndex);
				
				System.out.println("> Reading " + fileName + "...");
				if (isFaultyGSC(gsc))
				{
					System.out.println("> Skipping " + fileName + " (faulty GSC)...");
					continue;
				}
				start = System.currentTimeMillis();
				output1 = showCommonInfo(gsc,bgmCsv,charCsv,itemsCsv,gsdtStart);
				finish = System.currentTimeMillis();
				time1 = (finish-start)/1000; total += time1;
				
				start = System.currentTimeMillis();
				output2 = showSceneInfo(gsc,bgmCsv,charCsv,itemsCsv,gsdtStart,option);
				finish = System.currentTimeMillis();
				time2 = (finish-start)/1000; total += time1;

				System.out.println("Time required for Battle Settings:   " + time1 + " seconds."
									+ "\nTime required for Scene Information: "+ time2 + " seconds.");
				gscIndex++;
				
				File outputLog = new File(OUT_PATH+fileName.replace(fileExt, ".log"));
				FileWriter outputWriter = new FileWriter(outputLog);
				System.out.println("> Writing " + outputLog.getName() + "...");
				
				interval = (time1+time2);
				timeString = String.format("%.3f", interval);
				msg += fileName + " - " + timeString + " seconds\n";
				outputWriter.write(output1+output2);
				outputWriter.close();
			}
			System.out.printf("Total time elapsed: %.0f minute(s) & %.3f seconds.", total/60, total%60);

			timeString = String.format("%.0f", total/60);
			msg += "\nTotal time elapsed:\n" + timeString + " minutes & ";
			timeString = String.format("%.3f", total%60);
			msg += timeString + " seconds";
		}
		else 
		{ 
			msg="No GSC files found!"; msgType = JOptionPane.ERROR_MESSAGE;
		}
		JOptionPane.showMessageDialog(null, msg, WINDOW_TITLE, msgType);
	}
}