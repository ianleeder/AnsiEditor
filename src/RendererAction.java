import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public abstract class RendererAction implements ActionListener
{
    protected AnsiRenderer renderer;
    
    public RendererAction(AnsiRenderer r)
    {
        renderer = r;
    }
}