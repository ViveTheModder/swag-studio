package cmd;
//Swag Studio by ViveTheModder
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Scanner;

public class MainApp 
{
	private static int sharedPos=0;
	static final String GSC_PATH = "./gsc/";
	static final String OUT_PATH = "./out/";
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
	public static String getStringFromCondOrEventID(RandomAccessFile txt, int ID, boolean isCond) throws IOException
	{
		byte[] bytes = ByteBuffer.allocate(4).putInt(ID).array();
		String output = "";
		
		if (bytes[2] == -128) //check if ID belongs to opponent condition/event
		{
			bytes[2]=0; //treat it like a normal condition/event
			ID = ByteBuffer.wrap(bytes).getInt();
			output = " (Opponent)";
			
			if (isCond == false) //check for event ID
				if (ID>=16 && ID<30)  output += " [AUTO]";
		}
		
		output = getStringFromAnyID(txt, ID) + output; //properly initialize output
		
		if (isCond == true) //check for condition ID
			if (ID == 34) output += " [AUTO]";

		return output;
	} 
	public static String getCharInfo(int[][] teams, int charIndex, RandomAccessFile charTxt, RandomAccessFile itemsTxt) throws IOException
	{
		String output; boolean isDmg;
		if (teams[charIndex][2] == 0) isDmg = false; 
		else isDmg = true;
		output = "Character: " + getStringFromAnyID(charTxt, teams[charIndex][0]) + "\n";
		output += "Costume: " + (teams[charIndex][1]+1) + "\n";
		output += "Damaged: " + isDmg + "\n";
		output += "COM Difficulty Level: " + teams[charIndex][3] + "\n";
		output += "Strategy Z-Item: " + getStringFromAnyID(itemsTxt, teams[charIndex][4]) + "\n";
		output += "Initial Health: " + teams[charIndex][5] + "%\n";
		
		for (int i=6; i<=12; i++)
			output += "Z-Item #" + (i-5) + ": " + getStringFromAnyID(itemsTxt, teams[charIndex][i]) + "\n";
		return output;
	}
	public static String showBattleSettings(RandomAccessFile gsc, RandomAccessFile charTxt, RandomAccessFile itemsTxt, short gsdtStart) throws IOException
	{
		RandomAccessFile bgmTxt = new RandomAccessFile("txt/bgm.txt","r");
		RandomAccessFile mapTxt = new RandomAccessFile("txt/maps.txt","r");
		int bgmID, charIndex=0, curr=0, teamIndex=0, mapID;
		short offset;
		String output="";
		int[] teamCnt = new int[2];
		int[][] teams = new int[10][13];
		
		sharedPos=0;
		while (curr!=0x01000300) //traverse until start of scene 0 is reached
		{
			curr = gsc.readInt();
			if (curr == 0x01050A00)
			{
				output += "[Battle Settings]\n";
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				mapID = getIntFromOffset(gsc, offset, gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				bgmID = getIntFromOffset(gsc, offset, gsdtStart);
				output += "Map: " + getStringFromAnyID(mapTxt,mapID) + "\n";
				output += "BGM: " + getStringFromAnyID(bgmTxt,bgmID) + "\n\n";
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
				output += getCharInfo(teams, charIndex, charTxt, itemsTxt) + "\n";
			}
			sharedPos++; gsc.seek(sharedPos);
		}
		output += "> Results\nTeammate Count (Player 1): " + teamCnt[0] + "\n";
		output += "Teammate Count (Opponent): " + teamCnt[1] + "\n\n";
		return output;
	}
	public static String showSceneInfo(RandomAccessFile gsc, RandomAccessFile charTxt, RandomAccessFile itemsTxt, short gsdtStart) throws IOException
	{
		RandomAccessFile condTxt = new RandomAccessFile("txt/conditions.txt","r");
		RandomAccessFile eventTxt = new RandomAccessFile("txt/events.txt","r");
		int curr=0, currID=10000, gsacType=0, param;
		short offset; String output="";
		
		gsc.seek(sharedPos-1); output += "[Scene " + (currID-10000) + "]\n";
		while (curr!=0x47534454) //traverse until GSDT is reached
		{
			curr = gsc.readInt();
			currID = getLittleEndianInt(curr);
			if (currID>=10000 && currID<10030) //display scene ID as long as it's not a victory/loss sequence
				output += "\n[Scene " + (currID-10000) + "]\n";
			if (curr == 0x01010900)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				gsacType = getIntFromOffset(gsc,offset,gsdtStart);
			}
			if (curr == 0x08610200)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int condID = getIntFromOffset(gsc,offset,gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int gsacID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "Condition: " + getStringFromCondOrEventID(condTxt, condID, true) + "\n> GSAC ID: " + (gsacID-10000) + "\n";
			}
			if (curr == 0x01000D00)
			{
				gsc.seek(sharedPos+4);
				if (gsc.readInt() != 0x01000E00)
					output += "[Changes to Player 1 detected.]\n";
			}
			if (curr == 0x01000E00)
			{
				gsc.seek(sharedPos+4);
				if (gsc.readInt() != 0x01020800)
					output += "[Changes to Opponent detected.]\n";
			}
			if (curr == 0x08420100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart); //this param is likely a boolean
				if (param == 1) output += "> Gain full Ki Bars\n";
			}
			if (curr == 0x08430100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart); //this param is likely a multiple of 5000 or less
				output += "> Gain " + (param+1)*5000 + " HP (?)\n";
			}
			if (curr == 0x08460100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Set current Ki Amount to " + param + "%\n";
			}
			//0x08480100 and 0x08680100 work the same, although: the latter accepts 0 as a value while the former doesn't
			if (curr == 0x08480100 || curr == 0x08680100)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				param = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Set current Health to " + param + "%\n";
			}
			if (curr >= 0x08300100 && curr <= 0x08370100)
			{
				sharedPos++; gsc.seek(sharedPos);
				int itemIndex = gsc.readByte()%48;
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int itemID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "> Z-Item #" + (itemIndex+1) + ": " + getStringFromAnyID(itemsTxt,itemID) + "\n";
			}
			if (curr == 0x01020800 && gsacType == -1)
			{
				sharedPos+=5; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventID = getIntFromOffset(gsc,offset,gsdtStart);
				sharedPos+=4; gsc.seek(sharedPos);
				offset = getLittleEndianShort(gsc.readShort());
				int eventCharID = getIntFromOffset(gsc,offset,gsdtStart);
				output += "Event:     " + getStringFromCondOrEventID(eventTxt, eventID, false) + "\n";
				if (eventCharID != -1) //only print out char ID if the event actually uses it
					output += "Character: " + getStringFromAnyID(charTxt, eventCharID) + "\n";
			}
			sharedPos++; gsc.seek(sharedPos);
		}
		return output;
	}
	public static void main(String[] args) throws IOException 
	{
		File folder = new File(GSC_PATH);
		//filter out validated gsc file paths from everything else in the current working directory (I'd use RAF[] if it extended InputStream & OutputStream)
		File[] gscPaths = folder.listFiles((dir, name) -> name.startsWith("GSC-B-") && name.toLowerCase().endsWith(".gsc"));
		
		int gscIndex=0; short gsdtStart;
		String output1, output2;
		double start, finish, interval, total=0;
		
		RandomAccessFile[] gscFiles = new RandomAccessFile[gscPaths.length];
		RandomAccessFile charTxt = new RandomAccessFile("txt/characters.txt","r");
		RandomAccessFile itemsTxt = new RandomAccessFile("txt/items.txt","r");

		for (File file: gscPaths) //initialize the RAF array with the gsc file paths
		{
			gscFiles[gscIndex] = new RandomAccessFile(file.getAbsolutePath(), "r");
			gscIndex++;
		}
		gscIndex=0;
		for (RandomAccessFile gsc: gscFiles)
		{
			gsdtStart = getStartOfGSDT(gsc);
			String currName = gscPaths[gscIndex].getName();
			System.out.println("> Reading " + currName + "...");
			
			start = System.currentTimeMillis();
			output1 = showBattleSettings(gsc,charTxt,itemsTxt,gsdtStart);
			finish = System.currentTimeMillis();
			interval = finish-start; total += interval;
			
			start = System.currentTimeMillis();
			output2 = showSceneInfo(gsc,charTxt,itemsTxt,gsdtStart);
			finish = System.currentTimeMillis();
			total += finish-start;
			System.out.println("Time required for Battle Settings:   " + interval/1000 + " seconds."
								+ "\nTime required for Scene Information: "+(finish-start)/1000 + " seconds.");
			gscIndex++;
			
			File outputTxt = new File(OUT_PATH + currName.replace(".gsc", ".txt"));
			if (outputTxt.exists()) continue; //skip already-made text files
			FileWriter outputWriter = new FileWriter(outputTxt);
			System.out.println("> Writing " + outputTxt.getName() + "...");
			outputWriter.write(output1+output2);
			outputWriter.close();

			/* my attempt at solving the problem in parallel, which had the gsdtStart be calculated inside the method
			new Thread(() -> {try {System.out.println(showBattleSettings(gsc, charTxt, itemsTxt));} catch (IOException e) {e.printStackTrace();}}); */
		}
		System.out.printf("Total time elapsed: %.0f minutes & %.3f seconds.", (total/1000)/60, (total/1000)%60);
	}
}