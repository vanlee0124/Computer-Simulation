package computersimulator.gui;

import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Numeric pad composite 
 * @author perzanp
 */
public class PadComposite {
    private JPanel p;
    
    public PadComposite() {
    }    
    
    /**
     * 
     */
    public void createComposite() {
        JPanel buttonPanel = new JPanel();
        JButton[] buttons = new JButton[10];    // create 10 buttons from 0 to 10
        p = new JPanel();
         
        buttonPanel.setLayout(new GridLayout(4,3));
        
        // add 10 buttons
        for (int i = 0; i < buttons.length; i++) {
             buttons[i] = new JButton(""+i+"");
             if(i>0)buttonPanel.add(buttons[i]);
        }
        
        buttonPanel.add(buttons[0]); // add 0 on the bottom row
        
        // add Enter/Clear buttons
        buttonPanel.add(new JButton("Enter"));
        buttonPanel.add(new JButton("Clear"));
        buttonPanel.setPreferredSize(new Dimension(200, 200));
        p.add(buttonPanel);
    }
    
    /**
     * @return panel instance
     */ 
    public JComponent getGUI() {
        return p;
    }
}
