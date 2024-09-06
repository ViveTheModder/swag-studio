package cmd;
//Swag Studio v1.8 by ViveTheModder
import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class MainApp 
{
	private static final double VERSION = 1.8;
	private static int sharedPos=0; //position in GSC file shared amongst several methods
	private static int[][] teams = new int[10][13];
	static int gscCnt=0;
	static int option=1; //default option (simple, no cinematic info)
	static RandomAccessFile currGSC; //currently loaded GSC file
	static final int MAX_COLS = 440;
	static final int CSV_COUNT = 9;
	static final String CSV_PATH = "./csv/";
	static final String GSC_PATH = "./gsc/";
	static final String OUT_PATH = "./out/";
	static final String[] CSV_NAMES = 
	{"anm.csv","bgm.csv","characters.csv","conditions.csv","events.csv","items.csv","maps.csv","names.csv","sagas.csv"}; 
	static String[][] csvContents = new String[CSV_COUNT][MAX_COLS];
	
	public static boolean isFaultyGSC() throws IOException
	{
		boolean gscError = false;
		//24576 is the reserved space of Unexpected Help - anything bigger won't even work in-game so why use the tool lol
		if (currGSC.length()>24576) return true;
		currGSC.seek(0); //added this just to make sure it always starts from 0
		int gscf = currGSC.readInt(); //GSCF (Game Scenario Contents of File)
		if (gscf != 0x47534346) gscError = true; 
		
		currGSC.seek(8);
		short gscSize = LittleEndian.getShort(currGSC.readShort());
		if (gscSize+32 != currGSC.length()) gscError = true;
		
		currGSC.seek(16); 
		int gshd = currGSC.readInt(); //GSHD (GSC version indicator)
		if (gshd != 0x47534844) gscError = true; 
		currGSC.seek(32);
		
		/* gscVer values:
		 * v3.1 (0x0300000001000000) -> Budokai Tenkaichi 2
		 * v3.2 (0x0300000002000000) -> Budokai Tenkaichi 3
		 * v3.3 (0x0300000002000000) -> Raging Blast 1 */ 
		long gscVer = currGSC.readLong();
		if (gscVer != 0x0300000002000000L) gscError = true; 
		
		currGSC.seek(64);
		int gscd = currGSC.readInt(); //GSCD (contains total file size of all scenes)
		if (gscd != 0x47534344) gscError = true;

		currGSC.seek(72);
		short gsacTotalSize = LittleEndian.getShort(currGSC.readShort());
		currGSC.seek(gsacTotalSize+96); //112 is the actual size of the header, but I put 96 bc of the next check
		
		if (currGSC.readInt() != 0x47534454) gscError = true; //0x47534454 = GSDT (Game Scenario DaTa)
		return gscError;
	}
	public static float getFloatFromOffset(short offset, short gsdtStart) throws IOException
	{
		currGSC.seek((offset*4)+gsdtStart);
		return LittleEndian.getFloat(currGSC.readFloat());
	}
	public static int getIntFromOffset(short offset, short gsdtStart) throws IOException
	{
		currGSC.seek((offset*4)+gsdtStart);
		return LittleEndian.getInt(currGSC.readInt());
	}
	public static short getStartOfGSDT() throws IOException
	{
		int curr=0, pos=0;
		while (curr != 0x47534454)
		{
			curr = currGSC.readInt();
			pos+=4; currGSC.seek(pos);
		}
		
		currGSC.seek(pos+12);
		return (short) currGSC.getFilePointer();
	}
	public static String getSceneType(int gsacID, short gsdtStart, int initGSACpos) throws IOException
	{
		String output = "";
		int pos=gsdtStart, currID=0;
		short offset=0, currOffset=0;
		currGSC.seek(pos);
		
		if (gsacID==10049)
			output+=" (Placeholder Defeat)";
		else if (gsacID>=10040 && gsacID<=10048)
			output+=" (Defeat)";
		else if (gsacID==10039)
			output+=" (Placeholder Victory)";
		else if (gsacID>=10030)
			output+=" (Victory)";
		
		while (pos!=currGSC.length())
		{
			currID = LittleEndian.getInt(currGSC.readInt());
			if (currID == gsacID)
			{
				offset = (short) ((pos-gsdtStart)/4);
				break;
			}
			pos+=4; currGSC.seek(pos);
		}
		
		pos=initGSACpos; currGSC.seek(pos);
		while (currOffset!=21319) //21319 = GS (1st half of GSAC)
		{
			currOffset = LittleEndian.getShort(currGSC.readShort());
			if (currOffset == offset)
			{
				output+=" - UNUSED"; break;
			}
			pos+=2; currGSC.seek(pos);
		}
		
		return output;
	}
	public static String getStringFromAnyID(int csvIndex, int ID) throws IOException
	{
		setCsvContents(csvIndex);
		if (ID == 65535) return "Immediate"; //65535 (0xFFFF) is for condition IDs, 0xFFFFFFFF is for null Z-Items
		if (ID == -1) return null;
		String text = csvContents[csvIndex][ID];
		if (text==null) return "UNKNOWN (ID: " + ID + ")";
		return csvContents[csvIndex][ID];
	}
	public static String getStringFromCondOrEventID(int csvIndex, int ID, boolean isCond) throws IOException
	{
		byte[] bytes = ByteBuffer.allocate(4).putInt(ID).array();
		String output="", temp="";
		
		if (bytes[2] == -128) //check if ID belongs to opponent condition/event
		{
			bytes[2]=0; //treat it like a normal condition/event
			ID = ByteBuffer.wrap(bytes).getInt();
			output += " (Opponent)";
			
			if (isCond == false) //check for event ID
				if (ID>=16 && ID<30) output += " [AUTO]";
			if (isCond == true)
				if (ID>=22 && ID<29) output += " [AUTO]";
		}
		
		temp = getStringFromAnyID(csvIndex, ID);
		if (temp.equals("UNKNOWN")) temp += "(ID: " + ID + ")";
		output = temp + output; //properly initialize output
		
		if (isCond == true) //check for condition ID
			if (ID == 34) output += " [AUTO]";

		return output;
	} 
	public static String getCharInfo(int[][] teams, int charIndex) throws IOException
	{
		String output; boolean isDmg;
		if (teams[charIndex][2] == 0) isDmg = false;
		else isDmg = true;
		
		output = "Character: " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
		output += "Costume: " + (teams[charIndex][1]+1) + "\n";
		output += "Damaged: " + isDmg + "\n";
		output += "COM Difficulty Level: " + teams[charIndex][3] + "\n";
		output += "Strategy Z-Item: " + getStringFromAnyID(5, teams[charIndex][4]) + "\n";
		output += "Initial Health: " + teams[charIndex][5] + "%\n";
		
		for (int i=6; i<=12; i++)
			output += "Z-Item #" + (i-5) + ": " + getStringFromAnyID(5, teams[charIndex][i]) + "\n";
		return output;
	}
	public static String getCommonInfo(short gsdtStart) throws IOException
	{
		int rewardCsvIndex; //just a reference to the previous CSVs
		int bgmID, charIndex=0, curr=0, teamIndex=0, mapID;
		short offset;
		String output="", saga="";
		String[] rewardNames = {"Z-Points (from Easy to Hard)","Z-Items","Maps","Characters","Scenarios"};
		int[] rewards = new int[15];
		int[] teamCnt = new int[2];
		
		sharedPos=0;
		while (curr!=0x01000300) //traverse until start of scene 0 is reached
		{
			curr = currGSC.readInt();
			if (curr == 0x01010200)
			{
				output += "[General Information]\n";
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int scenarioID = getIntFromOffset(offset, gsdtStart);
				if (scenarioID<48)
				{
					output += "Name: " + getStringFromAnyID(7,scenarioID) + "\n";
					if (scenarioID>=44) saga = getStringFromAnyID(8,7);
					else if (scenarioID>=40) saga = getStringFromAnyID(8,6);
					else if (scenarioID>=35) saga = getStringFromAnyID(8,5);
					else if (scenarioID>=19) saga = getStringFromAnyID(8,4);
					else if (scenarioID>=14) saga = getStringFromAnyID(8,3);
					else if (scenarioID>=7) saga = getStringFromAnyID(8,2);
					else if (scenarioID>=3) saga = getStringFromAnyID(8,1);
					else saga = getStringFromAnyID(8,0);
					output += "Saga: " + saga + "\n\n";
				}
				MsgBox.updateProgress(2);
			}
			if (curr == 0x010F1000)
			{
				output += "[Rewards]\n";
				sharedPos+=5; currGSC.seek(sharedPos);
				for (int i=0; i<15; i++)
				{
					if (i>=12) rewardCsvIndex = 7;
					else if (i>=9) rewardCsvIndex = 2;
					else if (i>=6) rewardCsvIndex = 6;
					else rewardCsvIndex = 5;
					
					if (i%3==0) output += "> Acquired " + rewardNames[(int)i/3] + "\n";
					offset = LittleEndian.getShort(currGSC.readShort());
					rewards[i] = getIntFromOffset(offset, gsdtStart);
					sharedPos+=4; currGSC.seek(sharedPos);
					if (i>=3) output += getStringFromAnyID(rewardCsvIndex,rewards[i]) + "\n";
					else output += rewards[i] + "\n";
				}
				MsgBox.updateProgress(15);
			}
			if (curr == 0x01050A00)
			{
				output += "\n[Battle Settings]\n";
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				mapID = getIntFromOffset(offset, gsdtStart);
				sharedPos+=4; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				bgmID = getIntFromOffset(offset, gsdtStart);
				output += "Map: " + getStringFromAnyID(6,mapID) + "\n";
				output += "BGM: " + getStringFromAnyID(1,bgmID) + "\n\n";
				MsgBox.updateProgress(2);
			}
			if (curr == 0x010E0C00)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				charIndex = getIntFromOffset(offset, gsdtStart);
				
				if (charIndex >= 32768)
				{
					charIndex%=32768; charIndex+=5; teamIndex=1;
					if (charIndex==5) output += "[Opponent Team]\n";
				}
				if (charIndex==0) output += "[Player 1 Team]\n";
				output += "> Teammate " + ((charIndex%5)+1) + "\n";
				
				teamCnt[teamIndex]++;
				sharedPos+=4; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				teams[charIndex][0] = getIntFromOffset( offset, gsdtStart);
				for (int i=1; i<13; i++)
				{
					sharedPos+=4; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					teams[charIndex][i] = getIntFromOffset( offset, gsdtStart);
				}
				output += getCharInfo(teams, charIndex) + "\n";
			}
			sharedPos++; currGSC.seek(sharedPos);
		}
		output += "> Results\nTeammate Count (Player 1): " + teamCnt[0] + "\n";
		output += "Teammate Count (Opponent): " + teamCnt[1] + "\n\n";
		MsgBox.updateProgress(13*(teamCnt[0]+teamCnt[1]));
		return output;
	}
	public static String getSceneInfo(short gsdtStart, int option) throws IOException
	{
		int bgmID=0, charIndex, curr=0, currID=10000, gsacType=0, inputInt, initGSACpos=sharedPos-1;
		short offset; float inputFloat; String output="";
		
		currGSC.seek(initGSACpos);
		output += "[Scene " + (currID-10000) + "]\n";
		while (curr!=0x47534454) //traverse until GSDT is reached
		{
			curr = currGSC.readInt();
			currID = LittleEndian.getInt(curr);
							
			if (currID>10000 && currID<10050)
				output += "\n[Scene " + (currID-10000) + getSceneType( currID, gsdtStart, initGSACpos) + "]\n";
			/* start of GSC functions 08-14 */
			if (curr == 0x01020800 && gsacType == -1)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int eventID = getIntFromOffset(offset,gsdtStart);
				sharedPos+=4; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int eventCharID = getIntFromOffset(offset,gsdtStart);
				output += "Event:     " + getStringFromCondOrEventID(4, eventID, false) + "\n";
				if (eventCharID != -1) //only print out char ID if the event actually uses it
					output += "Character: " + getStringFromAnyID(2, eventCharID) + "\n";
			}
			if (curr == 0x01000700)
			{
				currGSC.seek(sharedPos+4);
				if (currGSC.readInt() != 0x01000D00) output += "\n===Battle Info===\n";
				else output += "\n===No Battle Info===\n";
			}
			if (curr == 0x01010900)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				gsacType = getIntFromOffset(offset,gsdtStart);
			}
			if (curr == 0x01000D00)
			{
				currGSC.seek(sharedPos+4);
				if (currGSC.readInt() != 0x01000E00) output += "[Changes to Player 1 detected.]\n";
			}
			if (curr == 0x01000E00)
			{
				currGSC.seek(sharedPos+4);
				if (currGSC.readInt() != 0x01020800) output += "[Changes to Opponent detected.]\n";
			}
			/* start of BGM functions */
			if (curr == 0x0101DD05)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				bgmID = getIntFromOffset(offset,gsdtStart);
				output += "BG Music:  " + getStringFromAnyID(1,bgmID) + "\n";
			}
			if (curr == 0x0100DE05)
			{
				currGSC.seek(sharedPos+4);
				output += "Lower Current BG Music\n";
			}
			
			//increment progress by function's number of parameters for functions from 1 to 1701
			if (curr >= 0x01010100 && curr <= 0x0101A506)
			{
				byte[] bytes = ByteBuffer.allocate(4).putInt(curr).array();
				MsgBox.updateProgress(bytes[2]);
			}
			//increment progress by property's number of paramters for properties from `0` to `z`
			if (curr >= 0x08300000 && curr <= 0x087A0000) 
			{
				byte[] bytes = ByteBuffer.allocate(4).putInt(curr).array();
				MsgBox.updateProgress(bytes[1]);
			}
			
			/* start of GSC properties from `0` to `a` type */
			if (curr >= 0x08300100 && curr <= 0x08370100)
			{
				sharedPos++; currGSC.seek(sharedPos);
				int itemIndex = currGSC.readByte()%48; //48=0x30, look at the 2nd byte of the property
				sharedPos+=4; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int itemID = getIntFromOffset(offset,gsdtStart);
				output += "> Z-Item #" + (itemIndex+1) + ": " + getStringFromAnyID(5,itemID) + "\n";
			}
			if (curr == 0x08430100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Apply changes to Teammate " + (inputInt+1) + "\n";
			}
			if (curr == 0x08460100 || curr == 0x08420100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Add " + inputInt + "% more Ki\n";
			}
			if (curr == 0x08480100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Add " + inputInt + "% more Health\n";
			}
			if (curr == 0x08490100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Set current Ki Amount to " + inputInt + "%\n";
			}
			if (curr == 0x08620100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				if (inputInt == 1) output += "> Set current Blast Stock amount to " + inputInt + "\n";
			}
			if (curr == 0x08680100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Set current Health to " + inputInt + "%\n";
			}
			if (curr == 0x086C0100)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				inputInt = getIntFromOffset(offset,gsdtStart);
				output += "> Set COM Difficulty Level to " + inputInt + "\n";
			}		
			if (curr == 0x08610200)
			{
				sharedPos+=5; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int condID = getIntFromOffset(offset,gsdtStart);
				sharedPos+=4; currGSC.seek(sharedPos);
				offset = LittleEndian.getShort(currGSC.readShort());
				int gsacID = getIntFromOffset(offset,gsdtStart);
				output += "Condition: " + getStringFromCondOrEventID(3, condID, true) + "\n> Scene ID: " + (gsacID-10000) + "\n";
			}
			/* cinematic option - I really wanted to split this method in 2, but I kept making things worse */
			if (option==0)
			{
				//start of GSC functions 01-06
				if (curr == 0x01010100)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "Wait " + inputFloat + " seconds\n";
				}
				if (curr == 0x01000300) output += "\n===Cinematic Info===\n";
				if (curr == 0x01000600) output += "Disable ALL VFX\n";
				
				//start of GSC functions 801-810
				if (curr == 0x01072103)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Position/Rotation for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
					for (int i=1; i<7; i++)
					{
						if (i==1) output+="> Position (XYZ): ";
						if (i==4) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; currGSC.seek(sharedPos);
						offset = LittleEndian.getShort(currGSC.readShort());
						inputFloat = getFloatFromOffset(offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01042203)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Position for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
					for (int i=1; i<4; i++)
					{
						output+="> Position (XYZ): ";
						sharedPos+=4; currGSC.seek(sharedPos);
						offset = LittleEndian.getShort(currGSC.readShort());
						inputFloat = getFloatFromOffset(offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01042303)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					
					output += "Change Rotation for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
					for (int i=1; i<4; i++)
					{
						output+="\n> Rotation (XYZ): ";
						sharedPos+=4; currGSC.seek(sharedPos);
						offset = LittleEndian.getShort(currGSC.readShort());
						inputFloat = getFloatFromOffset(offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x01012403)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Make " + getStringFromAnyID(2, teams[charIndex][0]) + " Visible\n";
				}
				if (curr == 0x01012503)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Make " + getStringFromAnyID(2, teams[charIndex][0]) + " Invisible\n";
				}
				if (curr == 0x01012603)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable Aura Charge for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012703)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Disable Aura Charge for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012803)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable Ki Charge for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012903)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Disable Ki Charge for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				if (curr == 0x01012A03)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					output += "Enable MPM Explosion for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				//start of GSC function 901
				if (curr == 0x01028503)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					sharedPos+=4; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputInt = getIntFromOffset(offset,gsdtStart);
					
					output += "Play Animation " + inputInt + "_" + getStringFromAnyID(0, inputInt) + ".canm for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
				}
				
				//start of GSC functions 1001-1003
				if (curr == 0x0101E903)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "Add " + inputFloat + " s fade-out\n";
				}
				if (curr == 0x0101EA03)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "Add " + inputFloat + " s fade-in\n";
				}	
				if (curr == 0x0101EB03) output += "Disable Current Fade\n";
				
				//start of GSC functions 1201-1205
				if (curr == 0x0106B104)
				{
					output += "[Initial Camera Point]\n";
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "> Position (XYZ): " + inputFloat + " ";
					
					for (int i=1; i<6; i++)
					{
						if (i==3) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; currGSC.seek(sharedPos);
						offset = LittleEndian.getShort(currGSC.readShort());
						inputFloat = getFloatFromOffset(offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x0100B204)
				{
					output += "[Additional Camera Point]\n";
					sharedPos+=9; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "> Transition Time: " + inputFloat + " ";
					
					for (int i=1; i<7; i++)
					{
						if (i==1) output+="\n> Position (XYZ): ";
						if (i==4) output+="\n> Rotation (XYZ): ";
						sharedPos+=4; currGSC.seek(sharedPos);
						offset = LittleEndian.getShort(currGSC.readShort());
						inputFloat = getFloatFromOffset(offset,gsdtStart);
						output += inputFloat + " ";
					}
					output += "\n";
				}
				if (curr == 0x0101B404)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "Shake Camera for " + inputFloat + " seconds\n";
				}
				if (curr == 0x0101B504) output += "Disable Current Camera Shake";
				
				//start of GSC functions 1603
				if (curr == 0x01024306)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					charIndex = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex >= 32768)
					{
						charIndex%=32768; charIndex+=5;
					}
					sharedPos+=4; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputInt = getIntFromOffset(offset,gsdtStart);
					
					if (charIndex!=32767)
						output += "Play Voice Line " + inputInt + " for " + getStringFromAnyID(2, teams[charIndex][0]) + "\n";
					else output += "Play Voice Line " + inputInt + " for Background Character\n";
				}
				/* common properties */
				if (curr == 0x08570000) output += "> Change fade-out color to white\n";
				if (curr == 0x086C0000) output += "> Loop property detected\n";
				if (curr == 0x08770000) output += "> Wait property detected\n";
				if (curr == 0x08740100)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputFloat = getFloatFromOffset(offset,gsdtStart);
					output += "Speed Coefficient: " + inputFloat + "\n";
				}
				//background voice lines
				if (curr == 0x08760200)
				{
					sharedPos+=5; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					int condID = getIntFromOffset(offset,gsdtStart);
					sharedPos+=4; currGSC.seek(sharedPos);
					offset = LittleEndian.getShort(currGSC.readShort());
					inputInt = getIntFromOffset(offset,gsdtStart);
					output += "Condition: " + getStringFromCondOrEventID(3, condID, true) + "\n> Voice Line ID: " + inputInt + "\n";
				}
			}
			sharedPos++; currGSC.seek(sharedPos);
		}
		return output;
	}
	public static void setCsvContents(int csvIndex) throws IOException
	{
		if (csvContents[csvIndex][0]!=null) return; //skip already-initialized CSV contents
		File csv = new File(CSV_PATH+CSV_NAMES[csvIndex]);
		
		Scanner sc = new Scanner(csv);
		while (sc.hasNextLine())
		{
			String input = sc.nextLine();
			String[] inputArray = input.split(",");
			int nameIndex = Integer.parseInt(inputArray[0]);
			csvContents[csvIndex][nameIndex] = inputArray[1];
		}
		sc.close();
	}
	
	public static void main(String[] args) throws IOException 
	{
		File perfTxt = new File(OUT_PATH+"performance.txt"), folder = new File(GSC_PATH);
		//filter out validated gsc paths from the rest in current working directory (I'd use RAF[] if it extended InputStream & OutputStream)
		File[] gscPaths = folder.listFiles((dir, name) -> 
		(
			name.startsWith("GSC-B-") && (name.toLowerCase().endsWith(".gsc") || name.toLowerCase().endsWith(".unk"))
		)); //that's right, the tool detects UNK files too lmao
		FileWriter outputWriter;
		
		double start, finish, interval=0, time1=0, time2=0, total=0;
		int gscIndex=0, msgType=JOptionPane.INFORMATION_MESSAGE;
		short gsdtStart;
		String output1, output2, performance="Date & Time of Execution: ", timeString;
		boolean isCmdUsed=false;
		performance += new Date().toString() + "\nSwag Studio Version: " + VERSION + "\n\n";
		
		gscCnt=gscPaths.length;
		RandomAccessFile[] gscFiles = new RandomAccessFile[gscCnt];

		if (gscCnt!=0)
		{
			for (File file: gscPaths) //initialize the RAF array with the file paths
			{
				gscFiles[gscIndex] = new RandomAccessFile(file.getAbsolutePath(), "r");
				gscIndex++;
			}
			//check for command line arguments
			if (args.length == 0) option=JOptionPane.showConfirmDialog(null, MsgBox.MSG_INIT, MsgBox.WINDOW_TITLE, 0);
			else
			{	
				isCmdUsed=true;
				if (args[0].equals("-c")) option=0; //skip confirm dialog if argument is given
				else option=JOptionPane.showConfirmDialog(null, MsgBox.MSG_INIT, MsgBox.WINDOW_TITLE, 0);
			}
			
			if (option==-1) System.exit(2); //user closes program
			if (!isCmdUsed) MsgBox.setMsgBox(); //apply GUI components if no CLI is used
			
			for (int i=0; i<gscFiles.length; i++)
			{
				currGSC = gscFiles[i];
				String fileName = gscPaths[i].getName();
				gsdtStart = getStartOfGSDT();
				
				//get file extension
				String fileExt = "";
				int dotIndex = fileName.lastIndexOf('.');
				if (dotIndex>=0) fileExt = fileName.substring(dotIndex);
				
				System.out.println("> Reading " + fileName + "...");
				MsgBox.setTextFromLabelInstrID(1, fileName);
				if (isFaultyGSC())
				{
					System.out.println("> Skipping " + fileName + " (faulty GSC)...");
					MsgBox.setTextFromLabelInstrID(2, fileName);
					continue;
				}
				
				total=0; //reset for each gsc
				start = System.currentTimeMillis();
				output1 = getCommonInfo(gsdtStart);
				MsgBox.setTextFromLabelInstrID(3, fileName);
				finish = System.currentTimeMillis();
				time1 = (finish-start)/1000; total += time1;
								
				start = System.currentTimeMillis();
				output2 = getSceneInfo(gsdtStart,option);
				MsgBox.setTextFromLabelInstrID(4, fileName);
				finish = System.currentTimeMillis();
				time2 = (finish-start)/1000; total += time2;
				interval += total;
				System.out.println("Time required for Battle Settings:   " + time1 + " seconds."
									+ "\nTime required for Scene Information: "+ time2 + " seconds.");
				gscIndex++;
				
				File outputLog = new File(OUT_PATH+fileName.replace(fileExt, ".log"));
				outputWriter = new FileWriter(outputLog);
				System.out.println("> Writing " + outputLog.getName() + "...");
				
				timeString = String.format("%.3f", total);
				performance += fileName + " - " + timeString + " seconds\n";
				outputWriter.write(output1+output2);
				outputWriter.close();
			}
			System.out.printf("Total time elapsed: %.0f minute(s) & %.3f seconds.", interval/60, interval%60);
						
			timeString = String.format("%.0f", interval/60);
			MsgBox.finalMessage = "Total time elapsed: " + timeString + " minute(s) & ";
			timeString = String.format("%.3f", interval%60);
			MsgBox.finalMessage += timeString + " seconds.\nDo you want to check the performance results for each file?";
		}
		else 
		{ 
			MsgBox.finalMessage="No GSC files found!"; msgType = JOptionPane.ERROR_MESSAGE;
			JOptionPane.showMessageDialog(null, MsgBox.finalMessage, MsgBox.WINDOW_TITLE, msgType);
			System.exit(1); //unsuccessful termination
		}
		if (!isCmdUsed)
		{
			MsgBox.frame.setVisible(false);
			option = JOptionPane.showConfirmDialog(null, MsgBox.finalMessage, MsgBox.WINDOW_TITLE, 0, msgType);
			if (option==0) //create and open performance.txt file if user clicks on 'Yes'
			{
				outputWriter = new FileWriter(perfTxt);
				outputWriter.write(performance);
				outputWriter.close();
				Desktop.getDesktop().open(perfTxt);
			}
		}
		System.exit(0); //successful termination
	}
}