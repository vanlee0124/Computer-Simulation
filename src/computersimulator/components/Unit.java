package computersimulator.components;

/**
 * A Unit is the primary component for data inside our Computer Simulator. It
 * allows our computer to maintain N-bit data types. It also allows quick debugging
 * and access to both base-10 and base-2 values.
 */
public class Unit {
    
    private String data;   
    private final int size;


    private final int MIN_SIGNED_VALUE;
    private final int MIN_UNSIGNED_VALUE;
    
    
    private final int MAX_SIGNED_VALUE;
    private final int MAX_UNSIGNED_VALUE;
    
    
    public Unit(int Size) {
        this(Size,0);
    }

    
    public Unit(int Size, int Value){
        if(Size>32 || Size<1){
            throw new java.lang.ArithmeticException("Unit size valid range 1-32 ("+Size+")");
           
        }
        this.size = Size;
        
        // MAX SIGNED VALUE = ((2xy(n-1))-1)      
        this.MAX_SIGNED_VALUE = (int)(Math.pow(2, (this.size-1))-1);        
        // MIN SIGNED VALUE = -((2xy(n-1))-1)
        this.MIN_SIGNED_VALUE = -(this.MAX_SIGNED_VALUE);
        
        // MAX UNSIGNED VALUE (2xyn)-1
        this.MAX_UNSIGNED_VALUE = (int)(Math.pow(2, this.size))-1;
        this.MIN_UNSIGNED_VALUE = 0;
        
        this.setValue(Value);
    }
    
    
    /**
     * Copy constructor
     * @param c
     */
    public Unit(Unit c){
        this.data = c.data;
        this.size = c.size;
        
        this.MAX_SIGNED_VALUE = c.MAX_SIGNED_VALUE;
        this.MAX_UNSIGNED_VALUE = c.MAX_UNSIGNED_VALUE;
        
        this.MIN_SIGNED_VALUE = c.MIN_SIGNED_VALUE;
        this.MIN_UNSIGNED_VALUE = c.MIN_UNSIGNED_VALUE;        
    }    

    /** 
     * Creates a Unit from a Binary String. This method allows for spacing which is trimmed for readability.
     * @param binaryReadable Binary String
     * @return Unit 
     */
    public static Unit UnitFromBinaryString(String binaryReadable){              
        String binary = binaryReadable.replace(" ", "");
        
        // original size
        int size = binary.length();
               
        Unit ret = new Unit(size);
        ret.setValueBinary(binary);
        
        return ret;
    }
    
    /**
     *
     * @return Number of Bits of Unit 
    */
    public int getSize() {
        return size;
    }

    /**
     *
     * @param value Integer
     * @throws ArithmeticException
     */
    public final void setValue(int value) throws java.lang.ArithmeticException {        
        if(value <= this.MAX_UNSIGNED_VALUE && value >= this.MIN_SIGNED_VALUE){           
            String raw = Integer.toBinaryString(value);            
            
            char signExtend = (value < 0) ? raw.charAt(0) : '0';
            
            while(raw.length() < this.size){
                raw = signExtend + raw;
            }
            
            this.data=raw;
            
        } else {
            throw new java.lang.ArithmeticException("{"+value+"} Out Of Range: ["+this.MIN_UNSIGNED_VALUE+" through "+this.MAX_SIGNED_VALUE+"]"); 
            //@TODO: this is a great location to throw a special overflow exception which can be caught later
        }
    }        
    
    
    /**
     *
     * @return Value as Signed Integer
     */
    public Integer getSignedValue(){        
        return this.getLongValue().intValue();
    }
    
    /**
     * @return Value as Unsigned Integer
     */
    public Integer getUnsignedValue(){
        return Integer.parseInt(this.data, 2);
    }
    
    
    /**
     * @return Value as Unsigned Long
     */
    public Long getLongValue(){
        String res = this.data;
        while(res.length() < 32){ // sign extend
            res = res.charAt(0) + res;
        }
        return Long.parseLong(res, 2);
    }
    
    /**
     * Decomposes a larger Unit and returns a smaller Unit by offset
     * @param start Start Index
     * @param stop  Stop Index
     * @return Unit(Start to Stop)
     */
    public Unit decomposeByOffset(int start, int stop){
        Integer[] digits = this.getBinaryArray();
        StringBuilder tempBinaryString = new StringBuilder();
        
        for(int i=start; i<=stop;i++){
            tempBinaryString.append(digits[i]);            
        }
        
        String binary = tempBinaryString.toString();
        
        int intValue = Integer.parseInt(binary, 2);
        
        return new Unit(binary.length(), intValue);        
    }

    /**
     * Extract out a single bit from a Unit and return a smaller Unit
     * @param index
     * @return Unit(index)
     */
    public Unit decomposeByIndex(int index){
        Integer[] digits = this.getBinaryArray();
        
        int intValue = Integer.parseInt(String.valueOf(digits[index]), 2);
        
        return new Unit(1, intValue);        
    }

    /** 
     * Alias for decompose by index for cleaner code.
     * @param index
     * @return Unit(index)
     */
    public Unit decomposeByOffset(int index){       
        return this.decomposeByIndex(index);       
    }
    
    
    /**
     *
     * @return Array of Bits (Only possible values are 1/0 despite integer storage)
     */
    public Integer[] getBinaryArray(){
        char[] temp=this.data.toCharArray();
        
        Integer[] digits = new Integer[temp.length];
        for (int i = 0; i < temp.length; ++i) {
            digits[i]= Character.digit(temp[i],10);
        }

        return digits;  
    }
    
    /**
     * Sets value using an array of integers
     * @param raw Integer Binary Array
     */
    public void setValueBinaryArray(Integer[] raw){
        this.setValueBinary(Unit.IntArrayToBinaryString(raw));
    }
    
    /**
     * Perform Shift Operation 
     * @param leftRight shifted left (L/R =1) or right (L/R = 0)
     * @param count number of times to shift
     * @param algorithmicLogical logically (A/L = 1) or arithmetically (A/L = 0)
     */
    public void shiftByCount(int leftRight, int count, int algorithmicLogical){
        if(leftRight==1){ // left
            // logical / algorithmic are identical
            while(count>0){
                Integer[] shiftTemp= this.getBinaryArray();
                for(int i=0; i<this.size-1; i++){
                    shiftTemp[i] = shiftTemp[i+1];
                }
                shiftTemp[this.size-1] = 0; // shiftOff in Rotate
                this.setValueBinaryArray(shiftTemp);
                count--;
            }
        } else { // right            
            while(count>0){
                Integer[] shiftTemp= this.getBinaryArray();
                int shiftOn=(algorithmicLogical==0) ? shiftTemp[0] : 0; 

                for(int i=this.size-1; i>0; i--){
                    shiftTemp[i] = shiftTemp[i-1];
                }
                shiftTemp[0] = shiftOn;
                this.setValueBinaryArray(shiftTemp);
                count--;
            }
            
        }
    }
    
    /**
     * Perform Rotate Operation 
     * @param leftRight rotated left (L/R =1) or right (L/R = 0)
     * @param count number of times to rotate
     */
    public void rotateByCount(int leftRight, int count){
        if(leftRight==1){ // left
            // logical / algorithmic are identical
            while(count>0){
                Integer[] shiftTemp= this.getBinaryArray();
                int shiftOff = shiftTemp[0]; // shifted on to the right side below
                for(int i=0; i<this.size-1; i++){
                    shiftTemp[i] = shiftTemp[i+1];
                }
                shiftTemp[this.size-1] = shiftOff; 
                this.setValueBinaryArray(shiftTemp);
                count--;
            }
        } else { // right            
            while(count>0){
                Integer[] shiftTemp= this.getBinaryArray();
                int shiftOff = shiftTemp[this.size-1]; // used in rotate
                for(int i=this.size-1; i>0; i--){
                    shiftTemp[i] = shiftTemp[i-1];
                }
                shiftTemp[0] = shiftOff;
                this.setValueBinaryArray(shiftTemp);
                count--;
            }            
        }
    }    
    
    /**
     *
     * @return Binary representation as a String
     */
    public String getBinaryString(){        
        Integer[] arr = getBinaryArray();
        return Unit.IntArrayToBinaryString(arr);                
    }
    
    public static String IntArrayToBinaryString(Integer [] arr){
        StringBuilder result = new StringBuilder() ;
        for (Integer el : arr) {
            result.append(el);
        }
        return result.toString();
    }

    /**
     * Accepts a binary string and loads it into the Unit
     * @param binary
     */
    public void setValueBinary(String binary){
        if(binary.length()==this.size){
            this.data = binary;
        } else if(binary.length() < this.size){
            do { // extend sign until bitsize matches
                binary = binary.substring(0,1) + binary;
            } while(binary.length() < this.size);
        } else {
            this.data = binary.substring(binary.length()-this.size); // cut from end (overflow left)
            System.out.println("!!!!!overflow in set value -- this should never happen!!!!!");
        }
            
    }
    /**
     *  Accepts an array and performs logical NOT on the bits
     */
    public Integer[] logicalNOT(Integer[] array)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = 1 - array[i];
        }
        return array;
    }
    
    /**
     *  Accepts two arrays and performs logical OR on the bits
     */
    public Integer[] logicalOR(Integer[] array1, Integer[] array2)
    {
        Integer[] ORarray = null;
        for (int i = 0; i < array1.length; i++)
        {
            if (array1[i] == array2[i])
            {
                ORarray[i] = 0;
            }
            else
            {
                ORarray[i] = 1;
            }
        }
        return ORarray;
    }
    
    @Override
    public String toString() {
        return "Unit("+this.size+"){" + "b10S=" + this.getSignedValue() +",b10U="+this.getUnsignedValue()+" v:["+this.MIN_UNSIGNED_VALUE+"to"+this.MAX_SIGNED_VALUE+"]), b2=" + this.getBinaryString() + '}';
    }
    
}
