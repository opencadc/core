package ca.nrc.cadc.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ca.nrc.cadc.util.AbstractConfiguration;

/*
 * Created on Jun 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

/**
 * @author goliaths
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AllTypesConfig extends AbstractConfiguration
{

    AllTypesConfig()
    {
        config_ = new Config();
    }

    public String configLabel;
    public String stringType_;
    public boolean booleanType_;
    public int intType_;
    public double doubleType_;

    public String[] stringArrayType_;
    public boolean[] booleanArrayType_;
    public int[] intArrayType_;
    public double[] doubleArrayType_;

    public Config config_;

    public List structInList_;

    public Map structInMap_;
    
    public static final String StringType_CONFIG_LOOKUP_VALUE = "STRINGTYPE";
    public static final String BooleanType_CONFIG_LOOKUP_VALUE = "BOOLEANTYPE";
    public static final String IntType_CONFIG_LOOKUP_VALUE = "INTTYPE";
    public static final String DoubleType_CONFIG_LOOKUP_VALUE = "DOUBLETYPE";

    public static final String StringArrayType_CONFIG_LOOKUP_VALUE =
        "STRING ARRAY TYPE";
    public static final String BooleanArrayType_CONFIG_LOOKUP_VALUE =
        "BOOLEAN ARRAY TYPE";
    public static final String IntArrayType_CONFIG_LOOKUP_VALUE =
        "INT ARRAY TYPE";
    public static final String DoubleArrayType_CONFIG_LOOKUP_VALUE =
        "DOUBLE ARRAY TYPE";

    public static final String Config_CONFIG_LOOKUP_VALUE = "CONFIG";

    public static final String Tables_CONFIG_LOOKUP_VALUE = "TABLES";
    public static final String StructInList_CONFIG_LOOKUP_VALUE = "TABLE";

    public static final String Patterns_CONFIG_LOOKUP_VALUE = "PATTERNS";
    public static final String StructInMap_CONFIG_LOOKUP_VALUE = "STRUCT_IN_MAP";

    /* (non-Javadoc)
     * @see ca.nrc.cadc.util.AbstractConfiguration#getConfigLabel()
     */
    public String getConfigLabel()
    {
        return configLabel;
    }

    /**
     * Set the config label
     * @param configLabel new config label
     */
    public void setConfigLabel(String configLabel)
    {
        this.configLabel = configLabel;
    }

    /**
     * @param b
     */
    public void setBooleanType(Boolean b)
    {
        booleanType_ = b.booleanValue();
    }

    /**
     * @param d
     */
    public void setDoubleType(Double d)
    {
        doubleType_ = d.doubleValue();
    }

    /**
     * @param i
     */
    public void setIntType(Integer i)
    {
        intType_ = i.intValue();
    }

    /**
     * @param string
     */
    public void setStringType(String string)
    {
        stringType_ = string;
    }

    /**
     * @param bs
     */
    public void setBooleanArrayType(boolean[] bs)
    {
        booleanArrayType_ = bs;
    }

    /**
     * @param ds
     */
    public void setDoubleArrayType(double[] ds)
    {
        doubleArrayType_ = ds;
    }

    /**
     * @param is
     */
    public void setIntArrayType(int[] is)
    {
        intArrayType_ = is;
    }

    /**
     * @param strings
     */
    public void setStringArrayType(String[] strings)
    {
        stringArrayType_ = strings;
    }

    /**
     * @param c
     */
    public void setConfig(Config c)
    {
        //config_ = c;
        //		config_.dd_ = c.dd_;
        //		config_.joinKey_ = c.joinKey_;

        this.config_ = c;
    }

    public void setTables(StructInList[] o)
    {
        this.structInList_ = Arrays.asList(o);
    }

    public boolean isValid()
    {
        return true;
    }

    public StructInList getStructInList(String forId)
    {
        for (int ii = 0; ii < this.structInList_.size(); ii++)
        {
            StructInList sil = (StructInList)structInList_.get(ii);
            String id = sil.entry1;
            if( id.equals( forId ))
            {
                return sil;
            }
        }
        return null;
    }

    public Map getStructInMap()
    {
        return structInMap_;
    }
    
} // end AllTypesConfig
