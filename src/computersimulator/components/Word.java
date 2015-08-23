
package computersimulator.components;

/**
 * This is a simple extension of Unit. A Word in our simulator is just a fixed
 * 20-bit Unit. This allows for basic type checking while programming.
 */
public class Word extends Unit {
    
    private final static int WORD_SIZE=20;

    public Word() {
        super(WORD_SIZE);
    }

    public Word(int value) {
        super(WORD_SIZE, value);
    }
    
    /**
     * Copy Constructor
     * @param c
     */
    public Word(Word c){
        super((Unit)c);
    }
    
    /**
     * Conversion Constructor
     * @param c
     */
    public Word(Unit c){
        super(WORD_SIZE);
        this.setValueBinary(c.getBinaryString());
    }
    
    
    /** 
     * Creates a Word from a Binary String. This method allows for spacing which is trimmed for readability.
     * @param binaryReadable Binary String
     * @return Word 
     */
    public static Word WordFromBinaryString(String binaryReadable){              
        String binary = binaryReadable.replace(" ", "");
        
        if(binary.length()!=WORD_SIZE){           
            return null;            
        }
        
        Long longValue = Long.parseLong(binary,2);
        int intValue = longValue.intValue();
        return new Word(intValue);
        
    }    
    
}
