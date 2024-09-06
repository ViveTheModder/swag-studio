package cmd;
//Swag Studio Message Box class by ViveTheModder
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class MsgBox 
{
	static JProgressBar bar;
	static JFrame frame;
	static JLabel label;
	static int currProg=0; //current progress units (unit = no. of params read from functions & properties)
	static int maxProgTotal=0; //maximum progress units for several gsc files
	static final int maxProgAdv = 50102; //maximum progress units (advanced) for Galaxy Battle
	static final int maxProgSim = 56150; //maximum progress units (simple) for Galaxy Battle
	static String finalMessage = "Time required for each GSC:\n";
	static final String HTML_TEXT = "<html><div style='text-align: center;'>";
	static final String MSG_INIT = "Would you like to include cinematic info for each log?";
	static final String WINDOW_TITLE = "Swag Studio";
	static final String[] LABEL_TEXT = 
	{"<br>Reading GSC file...","<br>Skipping faulty GSC file...","<br>Writing Common Info...","<br>Writing Scene Info..."};

	public static void updateProgress(int amount)
	{
		if (maxProgTotal==0) return; //this only happens when CLI is used
		int gscProg=0; //progress bar percentage
		currProg+=amount;
		gscProg = 100*currProg/maxProgTotal;
		maxProgTotal-=amount;
		bar.setValue(gscProg);
	}
	public static void setMsgBox()
	{
		if (MainApp.option==0) maxProgTotal=maxProgAdv*MainApp.gscCnt;
		else maxProgTotal=maxProgSim*MainApp.gscCnt;
		
		ImageIcon icon = new ImageIcon("icon.png"); 
		frame = new JFrame(WINDOW_TITLE);
		JPanel panel = new JPanel();
		label = new JLabel();
		
		bar = new JProgressBar();
		bar.setValue(0);
		bar.setStringPainted(true); //display percentage on progress bar
		
		panel.add(bar);
		panel.add(label);
		frame.add(panel);
		
		frame.setIconImage(icon.getImage());
		frame.setSize(256, 128);
		frame.setLocationRelativeTo(null); //set location to center of the screen
		frame.setResizable(false); //disable ability to resize window
		frame.setDefaultCloseOperation(0); //disable close button functionality
		frame.setVisible(true);
	}
	//LabelInstrID = Label Instruction ID (0 -> nada, 1 -> read, 2 -> skip, 3 & 4 -> write)
	public static void setTextFromLabelInstrID(int labelInstrID, String fileName) 
	{
		if (label==null) return;
		String text = HTML_TEXT + fileName + LABEL_TEXT[labelInstrID-1];
		label.setText(text);
	}
}
