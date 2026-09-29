import javax.swing.JTextPane;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.io.*;

public class AnsiRenderer extends JTextPane
{
    private static final int DEFAULT_ROWS = 25;
    private static final int DEFAULT_COLUMNS = 80;
    
    public static final String NEWLINE = System.getProperty("line.separator");
    
    private static final String ASCII_ENCODING_SCHEME = "cp437";
    private static final String ESCAPE_CHAR = getStringFromAscii(""+(char)0x1B); // I know the ascii esc char, not sure about unicode char
    
    private static final Font SYSTEM_FONT = new Font("Courier New", Font.PLAIN, 13);
    
    public static final Color[][] COLORS ={{new Color(0,0,0), new Color(128,128,128)},         // black, light black (gray)
                                           {new Color(128,0,0), new Color(255,0,0)},           // dark red, red
                                           {new Color(0,128,0), new Color(0,255,0)},           // dark green, green
                                           {new Color(128,128,0), new Color(255,255,0)},       // dark yellow, yellow
                                           {new Color(0,0,128), new Color(0,0,255)},           // dark blue, blue
                                           {new Color(128,0,128), new Color(255,0,255)},       // dark magenta, magenta
                                           {new Color(0,128,128), new Color(0,255,255)},       // dark cyan, cyan
                                           {new Color(128,128,128), new Color(255,255,255)}};  // dark white (gray), white
    
    private AbstractDocument doc;
    private SimpleAttributeSet attributes;
    
    private int fgColor, bgColor;
    private boolean isBright;
    
    public AnsiRenderer()
    {
        setPreferredSize(new Dimension(650,400));
        setFont(SYSTEM_FONT);
        setBackground(Color.BLACK);
        setEditable(true);
        
        StyledDocument styledDoc = getStyledDocument();
        if (styledDoc instanceof AbstractDocument)
        {
            doc = (AbstractDocument)styledDoc;
        }
        else
        {
            System.err.println("Text pane's document isn't an AbstractDocument!");
            System.exit(-1);
        }
        
        attributes = new SimpleAttributeSet();
        resetColor();
        setCaretPosition(0);
        setCaretColor(Color.WHITE);
    }
    
    public void setForeground(int n, boolean bright)
    {
        if(n<0 || n>=COLORS.length)
            throw new IllegalArgumentException("Invalid color index: " + n);
        
        StyleConstants.setForeground(attributes, COLORS[n][bright?1:0]);
        setCharacterAttributes(attributes, false);
    }
    
    public void setBackground(int n)
    {
        if(n<0 || n>=COLORS.length)
            throw new IllegalArgumentException("Invalid color index: " + n);
        
        StyleConstants.setBackground(attributes, COLORS[n][0]);
        setCharacterAttributes(attributes, false);
    }
    
    public void clearText()
    {
        try
        {
            doc.remove(0, doc.getLength());
        }
        catch (BadLocationException ble)
        {
            System.err.println("Couldn't clear the text.");
        }
    }
    
    public void renderText(String s)
    {
        if(s == null)
            return;
        
        String[] tokens = s.split("\\e");
    
        for(int i=0;i<tokens.length;i++)
        {
            if(tokens[i].length()==0)
                continue;
            
            int index = tokens[i].indexOf('m');
            
            if(index != -1)
                parseColorTag(tokens[i].substring(0, index));
            
            String toInsert = tokens[i].substring(index+1);
            int caretPos = getCaretPosition();
            insertString(getStringFromAscii(toInsert));
            
            // bit of a dodgy fix
            // it seems to not update caret position correctly after inserting whitespace
            // so inserting whitespace, then chars, then more whitespace is displayed incorrectly
            int pos = java.lang.Math.min(caretPos+toInsert.length(), doc.getLength());
            setCaretPosition(pos);
        }
    }
    
    private void insertString(String s)
    {
        insertString(getCaretPosition(), s);
    }
    
    private void insertString(int index, String s)
    {
        try
        {
            doc.insertString(index, s, attributes);
        }
        catch (BadLocationException ble)
        {
            System.err.println("Couldn't insert initial text.");
        }
    }
    
    public String getText()
    {
        StringBuffer toReturn = new StringBuffer();
        SimpleAttributeSet curAttribs;
        
        try
        {
            for(int i=0;i<doc.getLength();)
            {
                Element e = doc.getParagraphElement(i);
                
                i = e.getEndOffset()+1;
                for(int j=0;j<e.getElementCount();j++)
                {
                    Element subElement = e.getElement(j);
                    AttributeSet as = subElement.getAttributes();
                    Color fg = StyleConstants.getForeground(as);
                    Color bg = StyleConstants.getBackground(as);
                    int start = subElement.getStartOffset();
                    int length = subElement.getEndOffset() - subElement.getStartOffset();
                    
                    String ansiCode = getAnsiCode(fg, bg);
                    toReturn.append(ansiCode);
                    toReturn.append(doc.getText(start, length));
                }
            }
        }
        catch(BadLocationException e)
        {
            System.err.println(e.getMessage());
            return null;
        }
        
        return convertStringToAscii(toReturn.toString());
    }
    
    private String getAnsiCode(Color fg, Color bg)
    {
        int nfg = -1;
        int nbg = -1;
        boolean fgBright = false;
        
        for(int i=0;i<COLORS[0].length;i++)
        {
            for(int j=0;j<COLORS.length;j++)
            {
                if(nfg==-1 && fg.equals(COLORS[j][i]))
                {
                    nfg = 30+j;
                    fgBright = (i==1);
                }
                
                if(nbg==-1 && bg.equals(COLORS[j][i]))
                    nbg = 40+j;
            }
        }
        
        if(nfg==-1)
            nfg=37;
        
        if(nbg==-1)
            nbg=40;
        
        // if fg and bg colors are default, just send the reset command
        if(nfg == 37 && nbg == 40)
            return ESCAPE_CHAR + "[0m";
        
        return ESCAPE_CHAR + "[" + (fgBright?1:0) + ";" + nfg + ";" + nbg + "m";
    }
    
    private void resetColor()
    {
        fgColor = 7;
        bgColor = 0;
        isBright = false;
        
        applyColor();
    }
    
    private void applyColor()
    {
        StyleConstants.setBackground(attributes, COLORS[bgColor][0]);
        StyleConstants.setForeground(attributes, COLORS[fgColor][isBright?1:0]);
    }
    
    // stripped of <esc> and m
    private void parseColorTag(String tag)
    {
        tag = tag.substring(1); // chop off the [
        
        String[] flags = tag.split(";");
        
        for(int j=0;j<flags.length;j++)
        {
            int i = Integer.parseInt(flags[j]);
            
            if(i>=0 && i<=8)
            {
                switch(i)
                {
                    case 0: // Reset all attributes
                        resetColor();
                        break;
                    case 1: // Bright
                        isBright = true;
                        break;
                    case 2: // Dim
                        isBright = false;
                        break;
                    case 4: // Underscore
                    case 5: // Blink
                    case 7: // Reverse
                    case 8: // Hidden
                        break;
                    
                    default:
                        throw new IllegalArgumentException("Invalid color tag attribute: " + flags[j]);
                }
            }
            else if(i>=30 && i<=37)
                fgColor = i-30;
            else if(i>=40 && i<=47)
                bgColor = i-40;
            else
                throw new IllegalArgumentException("Invalid color tag attribute: " + flags[j]);
        }
        
        applyColor();
    }
    
    public static String getCompleteAsciiString()
    {
        byte[] bytes = new byte[256];
        
        for(int i=0;i<256;i++)
            bytes[i]=(byte)i;
        
        return getStringFromAscii(new String(bytes));
    }
    
    public static String getStringFromAscii(String s)
    {
        String toReturn = null;
        try
        {
            toReturn = new String(s.getBytes(), ASCII_ENCODING_SCHEME);
        }
        catch(UnsupportedEncodingException e)
        {
            System.err.println("Encoding scheme " + ASCII_ENCODING_SCHEME + " not supported");
            e.printStackTrace();
            System.exit(-1);
        }
        return toReturn;
    }
    
    public static String convertStringToAscii(String s)
    {
        String toReturn = null;
        
        try
        {
            toReturn = new String(s.getBytes(ASCII_ENCODING_SCHEME));
        }
        catch(UnsupportedEncodingException e)
        {
            System.err.println("Encoding scheme "+ASCII_ENCODING_SCHEME+" not supported");
            e.printStackTrace();
            System.exit(-1);
        }
        
        return toReturn;
    }
    
    public static void main(String[] args) throws Exception
    {
        AnsiRenderer ansi = new AnsiRenderer();
        
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(new javax.swing.JScrollPane(ansi));
        frame.pack();
        frame.setTitle("Ian's super-sexy ANSI viewer");
        
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("map.ANS")));
        
        String read = in.readLine();
        while(read!=null)
        {
            ansi.renderText(read+AnsiRenderer.NEWLINE);
            read = in.readLine();
        }
        in.close();
        
        String text = ansi.getText();
        ansi.renderText(text);
        
        frame.setVisible(true);
    }
}