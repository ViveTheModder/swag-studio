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

	public static void main(String[] args) throws IOException 
	{
		RandomAccessFile gsc = new RandomAccessFile("GSC", "r");
		RandomAccessFile charTxt = new RandomAccessFile("characters.txt","r");
		RandomAccessFile condTxt = new RandomAccessFile("conditions.txt","r");
		RandomAccessFile eventTxt = new RandomAccessFile("events.txt","r");

		if (checkGSC(gsc) == true) System.exit(1);
		short gsdtStart = getStartOfGSDT(gsc);
		
		int curr=0, currID=10000, pos=0, gsacType=0;
		short condOffset, gsacOffset, gsacTypeOffset, eventOffset, eventCharOffset; 
		byte offsetType; //currently unused (tool still has float support for those who wanna fork it)
		
		while (curr!=0x01000300) //traverse until start of scene 0 is reached
		{
			curr = gsc.readInt();
			pos+=4; gsc.seek(pos);
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
				gsacTypeOffset = getLittleEndianShort(gsc.readShort());
				gsacType = getIntFromOffset(gsc,gsacTypeOffset,gsdtStart);
			}
			if (gsacType!=-1) break; //-1 means the scene in question doesn't determine win or loss
			if (curr == 0x08610200)
			{
				pos+=5; gsc.seek(pos);
				condOffset = getLittleEndianShort(gsc.readShort());
				int condID = getIntFromOffset(gsc,condOffset,gsdtStart);
				pos+=4; gsc.seek(pos);
				gsacOffset = getLittleEndianShort(gsc.readShort());
				int gsacID = getIntFromOffset(gsc,gsacOffset,gsdtStart);
				System.out.println("Condition: " + getStringFromCondOrEventID(condTxt, condID) + "\n> GSAC ID: " + (gsacID-10000));
			}
			if (curr == 0x01020800)
			{
				pos+=5; gsc.seek(pos);
				eventOffset = getLittleEndianShort(gsc.readShort());
				int eventID = getIntFromOffset(gsc,eventOffset,gsdtStart);
				pos+=4; gsc.seek(pos);
				eventCharOffset = getLittleEndianShort(gsc.readShort());
				int eventCharID = getIntFromOffset(gsc,eventCharOffset,gsdtStart);
				System.out.println("Event:     " + getStringFromCondOrEventID(eventTxt, eventID));
				if (eventCharID != -1) //only print out char ID if the event actually uses it
					System.out.println("Character: " + getStringFromAnyID(charTxt, eventCharID));
			}
			pos++; gsc.seek(pos);
		}
	}
}