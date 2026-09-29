import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class AnsiEditor extends JFrame implements ActionListener
{
    private static final String VERSION = "v1.01";
    private static final String ABOUT_STRING = "ANSI ASCII Editor " + VERSION + " by Ian Leeder" + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "It's exceedingly difficult to get this text component (JTextPane) to show line/column numbers, " +
                                               "so you'll have to play it by ear.  It's a feature I want too.  Maybe in the future." + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "This page taught me all I needed to know about ANSI (however I only bothered implemented color control)" + AnsiRenderer.NEWLINE +
                                               "http://www.termsys.demon.co.uk/vtansi.htm" + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "Any bugs/complaints/compliments can be mailed to me at" + AnsiRenderer.NEWLINE +
                                               "i_leeder@hotmail.com" + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "Version history:" + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "Version 1.01" + AnsiRenderer.NEWLINE +
                                               "- Changed button fonts to match rendered fonts" + AnsiRenderer.NEWLINE +
                                               "- Added capability to capture keyboard entry and use the chosen colors" + AnsiRenderer.NEWLINE +
                                               "- Changed buttons to radio buttons, and added a sample text to see current colours" + AnsiRenderer.NEWLINE +
                                               "- Improved algorithm for insertion of ansi codes" + AnsiRenderer.NEWLINE +
                                               "- Fixed a problem with newline characters" + AnsiRenderer.NEWLINE +
                                               "- Improved \"About\" window :)" + AnsiRenderer.NEWLINE +
                                               "- Added \"reset cursor\" ansi code to the start of each file produced... safety measure";
    
    private static final Dimension GRID_ELEMENT_DIMENSION = new Dimension(10,10);
    private static final Insets NO_INSET = new Insets(0,0,0,0);
    private static final Insets FIVE_INSET = new Insets(5,5,5,5);
    private AnsiRenderer renderer;
    
    private JMenuItem aboutMenuItem, clearMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem;
    private File saveFile = null;
    private boolean isSaved = true;
    private JFileChooser chooser;
    private JScrollPane aboutScrollPane;
    
    public AnsiEditor()
    {
        setTitle("ANSI ASCII Editor " + VERSION);
        
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    exit();
                }
            });
        
        JTextArea jta = new JTextArea(ABOUT_STRING);
        jta.setEditable(false);
        jta.setLineWrap(true);
        jta.setWrapStyleWord(true);
        jta.setMargin(FIVE_INSET);
        
        aboutScrollPane = new JScrollPane(jta);
        aboutScrollPane.setPreferredSize(new Dimension(400,300));
        
        chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setControlButtonsAreShown(true);
        
        renderer = new AnsiRenderer();
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout(5,5));

        createMenuBar();
        
        pane.add(new JLabel("Standard screen seems to be 25x80"), "North");
        pane.add(new JScrollPane(renderer), "Center");
        pane.add(createButtonPanel(), "East");
        pane.add(createColorPanel(), "South");
                
        pack();
        
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth())/2, (screen.height - getHeight())/2);
    }
    
    private void createMenuBar()
    {
        // File menu
        openMenuItem = new JMenuItem("Open", 'O');
        openMenuItem.addActionListener(this);
        saveMenuItem = new JMenuItem("Save", 'S');
        saveMenuItem.addActionListener(this);
        saveAsMenuItem = new JMenuItem("Save As...", 'A');
        saveAsMenuItem.addActionListener(this);
        clearMenuItem = new JMenuItem("Clear document", 'C');
        clearMenuItem.addActionListener(this);
        aboutMenuItem = new JMenuItem("About", 'B');
        aboutMenuItem.addActionListener(this);
        exitMenuItem = new JMenuItem("Exit", 'x');
        exitMenuItem.addActionListener(this);
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(clearMenuItem);
        fileMenu.add(aboutMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        
        setJMenuBar(menuBar);
    }
    
    private JPanel createColorPanel()
    {
        String[] headings = {"Background", "Foreground", "FG bright"};
        JPanel colorPanel = new JPanel(new GridLayout(3,AnsiRenderer.COLORS.length,2,2));
        
        ButtonGroup bg = new ButtonGroup();
        
        for(int j=0;j<3;j++)
        {
            if(j==1)
                bg = new ButtonGroup();
            
            for(int i=0;i<AnsiRenderer.COLORS.length;i++)
            {
                if(i==0)
                    colorPanel.add(new JLabel(headings[j]));
                
                JRadioButton rb = new JRadioButton("", ((j==0&&i==0) || (j!=0&&i==7)));
                rb.addActionListener(new ChooseColorAction(renderer, i, (j==2), (j!=0)));
                rb.setBackground(AnsiRenderer.COLORS[i][(j==2?1:0)]);
                rb.setMargin(NO_INSET);
                
                bg.add(rb);
                
                colorPanel.add(rb);
            }
        }
        
        return colorPanel;
    }
    
    private JPanel createButtonPanel()
    {
        JPanel characterPanel = new JPanel(new GridLayout(17,17));
        for(int i=0;i<17;i++)
            characterPanel.add(new JLabel(i==0?"":Integer.toString(i-1,16).toUpperCase(), SwingConstants.CENTER));
        
        // converting the whole long string at once is quicker than doing the conversion 256 times
        String ascii = AnsiRenderer.getCompleteAsciiString();
        
        for(int i=0;i<256;i++)
        {
            if(i%16==0)
                characterPanel.add(new JLabel(Integer.toString(i,16).toUpperCase()));
            
            JButton b = new JButton(""+ascii.charAt(i));
            b.addActionListener(new InsertCharacterAction(renderer, (char)i));
            b.setMargin(NO_INSET);
            b.setFont(AnsiRenderer.SYSTEM_FONT);
            characterPanel.add(b);
        }
        
        return characterPanel;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        
        if(src == exitMenuItem)
            exit();
        
        if(src == aboutMenuItem)
            showAbout();
        
        if(src == saveMenuItem)
        {
            if(saveFile==null)
                saveAs();
            else
                save();
        }
        
        if(src == saveAsMenuItem)
            saveAs();
        
        if(src == openMenuItem)
            open();
            
        if(src == clearMenuItem)
        {
            saveFile = null;
            renderer.clearText();
        }
    }
    
    private void showAbout()
    {
        JOptionPane.showMessageDialog(this, aboutScrollPane, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void save()
    {
        // saveAs would have already been called if this was unintentional
        // therefore, if savefile is null, saveAs was cancelled
        if(saveFile == null)
            return;
        
        try
        {
            String toWrite = renderer.getText();
            
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(saveFile)));
            out.println(toWrite);
            out.close();
        }
        catch(IOException e)
        {
            showErrorMessage(this, "Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveAs()
    {
        int returnVal = chooser.showSaveDialog(this);
        
        saveFile = chooser.getSelectedFile();
        
        if(returnVal != JFileChooser.APPROVE_OPTION || saveFile == null)
        {
            saveFile = null;
            return;
        }
        
        save();
    }
    
    private void open()
    {
        int returnVal = chooser.showOpenDialog(this);
        saveFile = chooser.getSelectedFile();
        
        if(returnVal != JFileChooser.APPROVE_OPTION || saveFile == null)
        {
            saveFile = null;
            return;
        }
        
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(saveFile)));
            StringBuffer sb = new StringBuffer();
            String read = in.readLine();
            while(read!=null)
            {
                sb.append(read);
                sb.append(AnsiRenderer.NEWLINE);
                read = in.readLine();
            }
            in.close();
            
            renderer.clearText();
            renderer.renderText(sb.toString());
        }
        catch(Exception e)
        {
            showErrorMessage(this, "Error opening file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exit()
    {
        if(confirm(this, "Are you sure you want to exit?\nData may not be saved!"))
            System.exit(0);
    }
    
    public static void showErrorMessage(Component parent, String message)
    {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static boolean confirm(Component c, String question)
    {
        int choice = JOptionPane.showConfirmDialog(c, question, "Confirm", JOptionPane.YES_NO_OPTION);
        return ( choice == JOptionPane.YES_OPTION );
    }
    
    public static void main(String[] args)
    {
        new AnsiEditor().setVisible(true);
    }
}