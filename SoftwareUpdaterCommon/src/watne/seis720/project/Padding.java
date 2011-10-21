/**
 * The Software and related technology may not be downloaded or otherwise 
 * exported or reexported (i) into (or to a national or resident of) Cuba, Iraq, Libya, 
 * North Korea, Iran, or any other country to which the U.S. has embargoed goods; 
 * or (ii) to anyone on the U.S. Treasury Department's list of Specially Designated 
 * Nationals or the U.S. Commerce Department's Table of Denial Orders. By installing 
 * or using the Software, Licensee is agreeing to the foregoing and representing 
 * and warranting that they are not located in, under the control of, or a national or 
 * resident of any such country or on any such list. In addition, The Licensee agrees 
 * to comply with any other applicable U.S. export control laws and any local laws
 * in their jurisdiction that may impact the right to import, export, or use the Software. 
 * By installing or using the Software, Licensee is also representing and warranting
 * that they will not use, or permit or authorize others to use, the Software in connection 
 * with the design, development, production, stockpiling or use of any chemical or 
 * biological weapons.
 */
package watne.seis720.project;

/**
 * Constants for padding types available to AES_Implementable objects.
 * @author John Watne
 * 
 */
public enum Padding
{
    PKCS5PADDING("PKCS5Padding"), NOPADDING("NoPadding");

    private String description;

    /**
     * Initialize the Padding enum with the description used for Sun's
     * libraries.
     * @param description the description used for Sun's libraries.
     */
    Padding(String description)
    {
        this.description = description;
    }

    public String toString()
    {
        return this.description;
    }
} // public enum Padding
