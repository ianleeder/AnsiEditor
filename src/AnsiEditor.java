import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class AnsiEditor extends JFrame implements ActionListener
{
    private static final String ABOUT_STRING = "ANSI ASCII editor by Ian Leeder" + AnsiRenderer.NEWLINE +
                                               "It's exceedingly difficult to get this text component (JTextPane) to show line/column numbers, " + AnsiRenderer.NEWLINE +
                                               "so you'll have to play it by ear.  It's a feature I wanted too." + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "This page taught me all I needed to know about ANSI (however I only bothered implemented color control)" + AnsiRenderer.NEWLINE +
                                               "http://www.termsys.demon.co.uk/vtansi.htm" + AnsiRenderer.NEWLINE +
                                               AnsiRenderer.NEWLINE +
                                               "Any bugs/complaints/compliments can be mailed to me at" + AnsiRenderer.NEWLINE +
                                               "i_leeder@hotmail.com";
    
    private static final Dimension GRID_ELEMENT_DIMENSION = new Dimension(10,10);
    private static final Insets NO_INSET = new Insets(0,0,0,0);
    private AnsiRenderer renderer;
    
    private JMenuItem aboutMenuItem, clearMenuItem, openMenuItem, saveMenuItem, saveAsMenuItem, exitMenuItem;
    private File saveFile = null;
    private boolean isSaved = true;
    private JFileChooser chooser;
    
    public AnsiEditor()
    {
        setTitle("Ian's super-sexy ASCII ANSI EDITOR");
        
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    exit();
                }
            });
        
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
        
        for(int j=0;j<3;j++)
        {
            for(int i=0;i<AnsiRenderer.COLORS.length;i++)
            {
                if(i==0)
                    colorPanel.add(new JLabel(headings[j]));
                
                JButton b = new JButton();
                b.addActionListener(new ChooseColorAction(renderer, i, (j==2), (j!=0)));
                b.setBackground(AnsiRenderer.COLORS[i][(j==2?1:0)]);
                b.setMargin(NO_INSET);
                colorPanel.add(b);
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
            JOptionPane.showMessageDialog(this, ABOUT_STRING, "About", JOptionPane.INFORMATION_MESSAGE);
        
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
            renderer.clearText();
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