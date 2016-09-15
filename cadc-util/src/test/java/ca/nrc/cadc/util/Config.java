package ca.nrc.cadc.util;

import java.util.Map;

import ca.nrc.cadc.util.AbstractConfiguration;
/*
 * Created on Jun 18, 2003
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
public class Config extends AbstractConfiguration
{

	//	Config( String archive,
	//				 String database,
	//				 String ifmKeyword,
	//				 String dd,
	//				 String ddTable,
	//				 String joinKey,
	//				 String format )
	//	{
	//		archive_ = archive;
	//		database_ = database;
	//		ifmKeyword_ = ifmKeyword;
	//		dd_ = dd;
	//		ddTable_ = ddTable;
	//		joinKey_ = joinKey;
	//		format_ = format;
	//	}

	public Config()
	{
	}

	public String configLabel;
	public String database_;
	public String ifmKeyword_;
	public String dd_;
	public String ddTable_;
	public String joinKey_;
	public String format_;
	public String[] embeddedList;
	public String xtra = null;
	// parameter that comes from a second config file

	public static String staticKeyword_;
	public static String staticKeyword2_;

	public Map columns;

	public static final String Database_CONFIG_LOOKUP_VALUE = "DATABASE";
	public static final String IfmKeyword_CONFIG_LOOKUP_VALUE = "IFMKEYWORD";
	public static final String Dd_CONFIG_LOOKUP_VALUE = "DD";
	public static final String DdTable_CONFIG_LOOKUP_VALUE = "DDTABLE";
	public static final String JoinKey_CONFIG_LOOKUP_VALUE = "JOINKEY";
	public static final String Format_CONFIG_LOOKUP_VALUE = "FORMAT";
	public static final String StaticKeyword_CONFIG_LOOKUP_VALUE = "STATIC";
	public static final String StaticKeyword2_CONFIG_LOOKUP_VALUE = "STATIC2";
	public static final String EmbeddedList_CONFIG_LOOKUP_VALUE =
		"EMBEDDED LIST";
	public static final String Columns_CONFIG_LOOKUP_VALUE = "COLUMNS";
	public static final String Xtra_CONFIG_LOOKUP_VALUE = "XTRA";

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
	 * @param string
	 */
	public void setDatabase(String string)
	{
		database_ = string;
	}

	/**
	 * @param string
	 */
	public void setDd(String string)
	{
		dd_ = string;
	}

	/**
	 * @param string
	 */
	public void setDdTable(String string)
	{
		ddTable_ = string;
	}

	/**
	 * @param string
	 */
	public void setFormat(String string)
	{
		format_ = string;
	}

	/**
	 * @param string
	 */
	public void setIfmKeyword(String string)
	{
		ifmKeyword_ = string;
	}

	/**
	 * @param string
	 */
	public void setJoinKey(String string)
	{
		joinKey_ = string;
	}

	public void setStaticKeyword(String string)
	{
		staticKeyword_ = string;
	}

	public static void setStaticKeyword2(String string)
	{
		staticKeyword2_ = string;
	}

	public void setEmbeddedList(String[] strings)
	{
		embeddedList = strings;
	}

	public void setColumns(Map columns)
	{
		this.columns = columns;
	}

	public void setXtra(String string)
	{
		this.xtra = string;
	}

	public boolean isValid()
	{
		return true;
	}
} // end Config
