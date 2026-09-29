import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Color;

public class ChooseColorAction extends RendererAction
{
    private boolean isForeground, isBright;
    private int colorNumber;
    
    public ChooseColorAction(AnsiRenderer r, int n, boolean bright, boolean fg)
    {
        super(r);
        colorNumber = n;
        isForeground = fg;
        isBright = bright;
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(isForeground)
            renderer.setForeground(colorNumber, isBright);
        else
            renderer.setBackground(colorNumber);
        
        renderer.requestFocus();
    }
}