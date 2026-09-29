import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class InsertCharacterAction extends RendererAction
{
    private char displayChar;
    
    public InsertCharacterAction(AnsiRenderer r, char c)
    {
        super(r);
        displayChar = c;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        renderer.renderText(""+displayChar);
        renderer.requestFocus();
    }
}